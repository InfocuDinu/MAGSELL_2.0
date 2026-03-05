package com.bakerymanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupRecoveryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateValidBackupFile() throws Exception {
        Path db = tempDir.resolve("bakery.db");
        createSampleDb(db, "initial");

        Path settings = tempDir.resolve("config.properties");
        writeSettings(settings, tempDir.resolve("backups"), true, "Zilnic");

        BackupRecoveryService service = new BackupRecoveryService(
            "jdbc:sqlite:" + db,
            settings.toString(),
            true
        );

        Path backup = service.createBackupNow();
        assertTrue(Files.exists(backup));
        assertTrue(Files.size(backup) > 0);
        assertTrue(service.validateBackup(backup));
    }

    @Test
    void shouldRestoreDatabaseFromBackup() throws Exception {
        Path db = tempDir.resolve("bakery.db");
        createSampleDb(db, "versionA");

        Path settings = tempDir.resolve("config.properties");
        Path backupDir = tempDir.resolve("backups");
        writeSettings(settings, backupDir, true, "Zilnic");

        BackupRecoveryService service = new BackupRecoveryService(
            "jdbc:sqlite:" + db,
            settings.toString(),
            true
        );

        Path backup = service.createBackupNow();

        overwriteSampleDb(db, "versionB");
        assertEquals("versionB", readValue(db));

        Path preRestore = service.restoreFromBackup(backup);

        assertTrue(Files.exists(preRestore));
        assertEquals("versionA", readValue(db));
    }

    private void writeSettings(Path settingsPath, Path backupDir, boolean enabled, String frequency) throws IOException {
        Files.createDirectories(backupDir);
        Properties props = new Properties();
        props.setProperty("auto.backup", String.valueOf(enabled));
        props.setProperty("backup.frequency", frequency);
        props.setProperty("backup.location", backupDir.toString());
        try (var out = Files.newOutputStream(settingsPath)) {
            props.store(out, "test");
        }
    }

    private void createSampleDb(Path dbPath, String value) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS t (id INTEGER PRIMARY KEY, v TEXT)");
            st.execute("DELETE FROM t");
            st.execute("INSERT INTO t(id, v) VALUES (1, '" + value + "')");
        }
    }

    private void overwriteSampleDb(Path dbPath, String value) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement()) {
            st.execute("UPDATE t SET v='" + value + "' WHERE id=1");
        }
    }

    private String readValue(Path dbPath) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT v FROM t WHERE id=1")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }
}
