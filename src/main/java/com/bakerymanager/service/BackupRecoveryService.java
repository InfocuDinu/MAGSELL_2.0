package com.bakerymanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

@Service
public class BackupRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(BackupRecoveryService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String SETTINGS_AUTO_BACKUP = "auto.backup";
    private static final String SETTINGS_BACKUP_FREQUENCY = "backup.frequency";
    private static final String SETTINGS_BACKUP_LOCATION = "backup.location";

    private final Path settingsFile;
    private final Path databaseFile;

    private final Object lock = new Object();

    public BackupRecoveryService(@Value("${spring.datasource.url}") String datasourceUrl,
                                 @Value("${app.settings.file:config.properties}") String settingsFilePath) {
        this.settingsFile = Paths.get(settingsFilePath).toAbsolutePath().normalize();
        this.databaseFile = resolveSqliteFile(datasourceUrl);
    }

    BackupRecoveryService(String datasourceUrl, String settingsFilePath, boolean testMode) {
        this.settingsFile = Paths.get(settingsFilePath).toAbsolutePath().normalize();
        this.databaseFile = resolveSqliteFile(datasourceUrl);
    }

    @Scheduled(fixedDelayString = "${backup.scheduler.fixed-delay-ms:60000}")
    public void scheduledBackupTick() {
        try {
            BackupSettings settings = readBackupSettings();
            if (!settings.enabled()) {
                return;
            }

            Path backupDir = ensureBackupDir(settings.location());
            if (!isBackupDue(backupDir, settings.frequency())) {
                return;
            }

            Path backupFile = createBackupInternal(backupDir, "scheduled");
            logger.info("Scheduled backup created: {}", backupFile);
        } catch (Exception ex) {
            logger.error("Scheduled backup failed", ex);
        }
    }

    public Path createBackupNow() throws IOException {
        BackupSettings settings = readBackupSettings();
        Path backupDir = ensureBackupDir(settings.location());
        return createBackupInternal(backupDir, "manual");
    }

    public Path restoreFromBackup(Path backupFile) throws IOException {
        Objects.requireNonNull(backupFile, "backupFile");
        Path normalizedBackup = backupFile.toAbsolutePath().normalize();

        if (!Files.exists(normalizedBackup) || !Files.isRegularFile(normalizedBackup)) {
            throw new IOException("Backup file not found: " + normalizedBackup);
        }

        if (!validateBackup(normalizedBackup)) {
            throw new IOException("Backup file failed integrity check: " + normalizedBackup);
        }

        synchronized (lock) {
            Path dbParent = databaseFile.getParent();
            if (dbParent != null) {
                Files.createDirectories(dbParent);
            }

            String preRestoreName = databaseFile.getFileName() + ".pre-restore-" + LocalDateTime.now().format(TS) + ".bak";
            Path preRestore = dbParent != null ? dbParent.resolve(preRestoreName) : Paths.get(preRestoreName).toAbsolutePath();

            if (Files.exists(databaseFile)) {
                Files.copy(databaseFile, preRestore, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.copy(normalizedBackup, databaseFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Database restored from backup {}. Pre-restore backup: {}", normalizedBackup, preRestore);
            return preRestore;
        }
    }

    public boolean validateBackup(Path backupFile) {
        try {
            if (backupFile == null) {
                return false;
            }

            Path path = backupFile.toAbsolutePath().normalize();
            if (!Files.exists(path) || !Files.isRegularFile(path) || Files.size(path) == 0L) {
                return false;
            }

            String jdbc = "jdbc:sqlite:" + path;
            try (Connection connection = DriverManager.getConnection(jdbc);
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA integrity_check")) {
                if (!rs.next()) {
                    return false;
                }
                String result = rs.getString(1);
                return "ok".equalsIgnoreCase(result);
            }
        } catch (Exception ex) {
            logger.warn("Backup validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    private Path createBackupInternal(Path backupDir, String trigger) throws IOException {
        synchronized (lock) {
            if (!Files.exists(databaseFile)) {
                throw new IOException("Database file not found: " + databaseFile);
            }

            String fileName = "bakery_backup_" + LocalDateTime.now().format(TS) + ".db";
            Path backupFile = backupDir.resolve(fileName);
            Files.copy(databaseFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

            if (!validateBackup(backupFile)) {
                Files.deleteIfExists(backupFile);
                throw new IOException("Backup file failed integrity check after creation");
            }

            writeLastBackupMetadata(backupDir, Instant.now().toEpochMilli(), trigger, backupFile.getFileName().toString());
            return backupFile;
        }
    }

    private BackupSettings readBackupSettings() {
        Properties props = new Properties();

        if (Files.exists(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                props.load(in);
            } catch (IOException ex) {
                logger.warn("Cannot read settings file {}, using defaults", settingsFile, ex);
            }
        }

        boolean enabled = Boolean.parseBoolean(props.getProperty(SETTINGS_AUTO_BACKUP, "true"));
        String frequencyRaw = props.getProperty(SETTINGS_BACKUP_FREQUENCY, "Zilnic");
        BackupFrequency frequency = BackupFrequency.fromLabel(frequencyRaw);
        String locationRaw = props.getProperty(
            SETTINGS_BACKUP_LOCATION,
            Paths.get(System.getProperty("user.home"), "bakery_backups").toString()
        );

        return new BackupSettings(enabled, frequency, locationRaw);
    }

    private Path ensureBackupDir(String location) throws IOException {
        Path dir = Paths.get(location).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private boolean isBackupDue(Path backupDir, BackupFrequency frequency) {
        Path metadata = backupDir.resolve("backup-meta.properties");
        if (!Files.exists(metadata)) {
            return true;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(metadata)) {
            props.load(in);
            long lastEpoch = Long.parseLong(props.getProperty("lastBackupEpochMs", "0"));
            if (lastEpoch <= 0) {
                return true;
            }

            Duration elapsed = Duration.between(Instant.ofEpochMilli(lastEpoch), Instant.now());
            return elapsed.compareTo(frequency.toDuration()) >= 0;
        } catch (Exception ex) {
            logger.warn("Could not parse backup metadata, forcing backup now", ex);
            return true;
        }
    }

    private void writeLastBackupMetadata(Path backupDir, long epochMs, String trigger, String fileName) {
        Path metadata = backupDir.resolve("backup-meta.properties");
        Properties props = new Properties();
        props.setProperty("lastBackupEpochMs", String.valueOf(epochMs));
        props.setProperty("lastBackupFile", fileName);
        props.setProperty("lastBackupTrigger", trigger);

        try (OutputStream out = Files.newOutputStream(metadata)) {
            props.store(out, "Backup metadata");
        } catch (IOException ex) {
            logger.warn("Could not write backup metadata", ex);
        }
    }

    private static Path resolveSqliteFile(String datasourceUrl) {
        if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("Unsupported datasource for backup/recovery: " + datasourceUrl);
        }

        String raw = datasourceUrl.substring("jdbc:sqlite:".length());
        int queryIndex = raw.indexOf('?');
        if (queryIndex >= 0) {
            raw = raw.substring(0, queryIndex);
        }

        if (raw.startsWith("file:")) {
            raw = raw.substring("file:".length());
        }

        return Paths.get(raw).toAbsolutePath().normalize();
    }

    private record BackupSettings(boolean enabled, BackupFrequency frequency, String location) {
    }

    private enum BackupFrequency {
        DAILY("zilnic", Duration.ofDays(1)),
        WEEKLY("săptămânal", Duration.ofDays(7)),
        MONTHLY("lunar", Duration.ofDays(30));

        private final String label;
        private final Duration duration;

        BackupFrequency(String label, Duration duration) {
            this.label = label;
            this.duration = duration;
        }

        public Duration toDuration() {
            return duration;
        }

        static BackupFrequency fromLabel(String label) {
            if (label == null) {
                return DAILY;
            }
            String normalized = label.trim().toLowerCase();
            return Stream.of(values())
                .filter(v -> v.label.equals(normalized))
                .findFirst()
                .orElse(DAILY);
        }
    }
}
