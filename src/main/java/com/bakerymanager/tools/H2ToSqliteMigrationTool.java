package com.bakerymanager.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Tool de migrare/validare pentru transfer date din H2 in SQLite.
 *
 * Exemple:
 *   java -cp target\\bakery-manager-pro-1.0.0-jar-with-dependencies.jar com.bakerymanager.tools.H2ToSqliteMigrationTool \
 *        --mode=migrate --h2Url=jdbc:h2:file:./data/bakery-h2 --h2User=sa --h2Password= --sqlitePath=./bakery.db --replace=true
 *
 *   java -cp target\\bakery-manager-pro-1.0.0-jar-with-dependencies.jar com.bakerymanager.tools.H2ToSqliteMigrationTool \
 *        --mode=validate --h2Url=jdbc:h2:file:./data/bakery-h2 --h2User=sa --h2Password= --sqlitePath=./bakery.db
 */
public class H2ToSqliteMigrationTool {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static void main(String[] args) {
        try {
            Map<String, String> params = parseArgs(args);
            String mode = params.getOrDefault("mode", "migrate").trim().toLowerCase(Locale.ROOT);

            String h2Url = resolveH2Url(params);
            String h2User = params.getOrDefault("h2User", "sa");
            String h2Password = params.getOrDefault("h2Password", "");
            String sqlitePathRaw = requireParam(params, "sqlitePath");
            Path sqlitePath = Paths.get(sqlitePathRaw).toAbsolutePath().normalize();

            boolean replace = parseBool(params.getOrDefault("replace", "true"));
            boolean backup = parseBool(params.getOrDefault("backup", "true"));
            boolean validateAfter = parseBool(params.getOrDefault("validateAfter", "true"));

            Path reportPath = resolveReportPath(params.get("reportPath"), mode);

            if ("migrate".equals(mode)) {
                MigrationSummary summary = migrate(h2Url, h2User, h2Password, sqlitePath, replace, backup);
                List<String> report = new ArrayList<>();
                report.add("=== H2 -> SQLite MIGRATION REPORT ===");
                report.add("status=migrated");
                report.add("h2Url=" + h2Url);
                report.add("sqlitePath=" + sqlitePath);
                report.add("replace=" + replace);
                report.add("backup=" + backup);
                report.add("tablesMigrated=" + summary.tableCount());
                report.add("rowsMigrated=" + summary.totalRows());

                if (validateAfter) {
                    ValidationSummary validation = validate(h2Url, h2User, h2Password, sqlitePath);
                    report.add("");
                    report.add("=== VALIDATION ===");
                    report.add("status=" + (validation.ok() ? "ok" : "failed"));
                    report.add("checkedTables=" + validation.checkedTables());
                    report.add("mismatches=" + validation.mismatches().size());
                    for (String mismatch : validation.mismatches()) {
                        report.add(" - " + mismatch);
                    }

                    writeReport(reportPath, report);
                    if (!validation.ok()) {
                        System.err.println("[ERROR] Validarea a gasit neconcordante. Vezi raport: " + reportPath);
                        System.exit(2);
                    }
                } else {
                    writeReport(reportPath, report);
                }

                System.out.println("[OK] Migrare finalizata. Rapor: " + reportPath);
                System.out.println("[OK] Tabele migrate: " + summary.tableCount() + ", randuri migrate: " + summary.totalRows());
                return;
            }

            if ("validate".equals(mode)) {
                ValidationSummary validation = validate(h2Url, h2User, h2Password, sqlitePath);

                List<String> report = new ArrayList<>();
                report.add("=== H2 -> SQLite VALIDATION REPORT ===");
                report.add("status=" + (validation.ok() ? "ok" : "failed"));
                report.add("h2Url=" + h2Url);
                report.add("sqlitePath=" + sqlitePath);
                report.add("checkedTables=" + validation.checkedTables());
                report.add("mismatches=" + validation.mismatches().size());
                for (String mismatch : validation.mismatches()) {
                    report.add(" - " + mismatch);
                }
                writeReport(reportPath, report);

                if (!validation.ok()) {
                    System.err.println("[ERROR] Validare esuata. Vezi raport: " + reportPath);
                    System.exit(2);
                }

                System.out.println("[OK] Validare finalizata fara neconcordante. Raport: " + reportPath);
                return;
            }

            throw new IllegalArgumentException("Mode necunoscut: " + mode + " (acceptat: migrate|validate)");
        } catch (Exception ex) {
            System.err.println("[ERROR] " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static MigrationSummary migrate(String h2Url,
                                            String h2User,
                                            String h2Password,
                                            Path sqlitePath,
                                            boolean replace,
                                            boolean backup) throws Exception {

        if (backup && Files.exists(sqlitePath)) {
            String backupFileName = sqlitePath.getFileName() + ".pre-migration-" + LocalDateTime.now().format(TS) + ".bak";
            Path backupPath = sqlitePath.getParent() != null
                ? sqlitePath.getParent().resolve(backupFileName)
                : Paths.get(backupFileName).toAbsolutePath();
            Files.copy(sqlitePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[INFO] Backup SQLite creat: " + backupPath);
        }

        try (Connection h2 = DriverManager.getConnection(h2Url, h2User, h2Password);
             Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath)) {

            sqlite.setAutoCommit(false);
            executeSqlitePragma(sqlite, "PRAGMA foreign_keys = OFF");

            List<String> tables = readSourceTables(h2);
            long rows = 0;

            for (String table : tables) {
                TableDefinition def = readTableDefinition(h2, table);
                createTargetTable(sqlite, def, replace);
                long copied = copyTableData(h2, sqlite, def);
                rows += copied;
                System.out.println("[INFO] Tabela migrata: " + table + " (rows=" + copied + ")");
            }

            sqlite.commit();
            executeSqlitePragma(sqlite, "PRAGMA foreign_keys = ON");
            return new MigrationSummary(tables.size(), rows);
        }
    }

    private static ValidationSummary validate(String h2Url,
                                              String h2User,
                                              String h2Password,
                                              Path sqlitePath) throws Exception {

        try (Connection h2 = DriverManager.getConnection(h2Url, h2User, h2Password);
             Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath)) {

            List<String> tables = readSourceTables(h2);
            List<String> mismatches = new ArrayList<>();

            for (String table : tables) {
                if (!existsSqliteTable(sqlite, table)) {
                    mismatches.add(table + " missing in SQLite");
                    continue;
                }

                long sourceCount = countRows(h2, table);
                long targetCount = countRows(sqlite, table);
                if (sourceCount != targetCount) {
                    mismatches.add(table + " rowCount mismatch (h2=" + sourceCount + ", sqlite=" + targetCount + ")");
                }
            }

            return new ValidationSummary(tables.size(), mismatches);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String normalized = arg.trim();
            if (!normalized.startsWith("--") || !normalized.contains("=")) {
                continue;
            }
            int idx = normalized.indexOf('=');
            String key = normalized.substring(2, idx).trim();
            String val = normalized.substring(idx + 1).trim();
            map.put(key, val);
        }
        return map;
    }

    private static String resolveH2Url(Map<String, String> params) {
        String direct = params.get("h2Url");
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }

        String h2File = params.get("h2File");
        if (h2File != null && !h2File.isBlank()) {
            Path base = Paths.get(h2File).toAbsolutePath().normalize();
            String basePath = base.toString().replace("\\", "/");
            return "jdbc:h2:file:" + basePath;
        }

        throw new IllegalArgumentException("Parametru lipsa: h2Url sau h2File");
    }

    private static String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parametru lipsa: " + key);
        }
        return value;
    }

    private static boolean parseBool(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private static Path resolveReportPath(String reportPathArg, String mode) {
        if (reportPathArg != null && !reportPathArg.isBlank()) {
            return Paths.get(reportPathArg).toAbsolutePath().normalize();
        }
        String file = "h2-sqlite-" + mode + "-" + LocalDateTime.now().format(TS) + ".txt";
        return Paths.get("logs", "migration", file).toAbsolutePath().normalize();
    }

    private static void writeReport(Path reportPath, List<String> lines) throws IOException {
        Path parent = reportPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(reportPath, lines);
    }

    private static void executeSqlitePragma(Connection sqlite, String sql) throws SQLException {
        try (Statement st = sqlite.createStatement()) {
            st.execute(sql);
        }
    }

    private static List<String> readSourceTables(Connection h2) throws SQLException {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME";

        List<String> tables = new ArrayList<>();
        try (Statement st = h2.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String table = rs.getString(1);
                if (table != null && !table.isBlank()) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    private static TableDefinition readTableDefinition(Connection h2, String table) throws SQLException {
        DatabaseMetaData meta = h2.getMetaData();

        List<ColumnDefinition> columns = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(null, "PUBLIC", table, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME");
                int nullable = rs.getInt("NULLABLE");
                String isAuto = rs.getString("IS_AUTOINCREMENT");
                columns.add(new ColumnDefinition(
                    name,
                    type,
                    nullable != DatabaseMetaData.columnNoNulls,
                    "YES".equalsIgnoreCase(isAuto)
                ));
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalStateException("Nu s-au detectat coloane pentru tabela " + table);
        }

        List<PrimaryKeyColumn> pks = new ArrayList<>();
        try (ResultSet rs = meta.getPrimaryKeys(null, "PUBLIC", table)) {
            while (rs.next()) {
                pks.add(new PrimaryKeyColumn(rs.getString("COLUMN_NAME"), rs.getShort("KEY_SEQ")));
            }
        }

        pks.sort(Comparator.comparingInt(PrimaryKeyColumn::keySeq));
        List<String> pkColumns = pks.stream().map(PrimaryKeyColumn::columnName).filter(Objects::nonNull).toList();

        return new TableDefinition(table, columns, pkColumns);
    }

    private static void createTargetTable(Connection sqlite, TableDefinition table, boolean replace) throws SQLException {
        try (Statement st = sqlite.createStatement()) {
            if (replace) {
                st.execute("DROP TABLE IF EXISTS " + q(table.name()));
            }

            Set<String> pkSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            pkSet.addAll(table.primaryKeys());

            boolean singlePk = table.primaryKeys().size() == 1;
            String singlePkName = singlePk ? table.primaryKeys().get(0) : null;

            List<String> parts = new ArrayList<>();
            for (ColumnDefinition column : table.columns()) {
                String mapped = mapSqlTypeToSqlite(column.typeName());
                boolean inlineAutoPk = singlePk
                    && column.name().equalsIgnoreCase(singlePkName)
                    && column.autoIncrement()
                    && isIntegerLike(mapped);

                if (inlineAutoPk) {
                    parts.add(q(column.name()) + " INTEGER PRIMARY KEY AUTOINCREMENT");
                    continue;
                }

                String notNull = column.nullable() ? "" : " NOT NULL";
                parts.add(q(column.name()) + " " + mapped + notNull);
            }

            boolean pkAlreadyInline = parts.stream().anyMatch(p -> p.toUpperCase(Locale.ROOT).contains("PRIMARY KEY"));
            if (!pkSet.isEmpty() && !pkAlreadyInline) {
                String pkClause = pkSet.stream().map(H2ToSqliteMigrationTool::q).collect(Collectors.joining(", "));
                parts.add("PRIMARY KEY (" + pkClause + ")");
            }

            String ddl = "CREATE TABLE IF NOT EXISTS " + q(table.name()) + " (" + String.join(", ", parts) + ")";
            st.execute(ddl);
        }
    }

    private static long copyTableData(Connection h2, Connection sqlite, TableDefinition table) throws SQLException {
        String select = "SELECT * FROM " + q(table.name());

        try (Statement st = h2.createStatement(); ResultSet rs = st.executeQuery(select)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            List<String> names = new ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                names.add(md.getColumnName(i));
            }

            String columns = names.stream().map(H2ToSqliteMigrationTool::q).collect(Collectors.joining(", "));
            String placeholders = String.join(", ", java.util.Collections.nCopies(cols, "?"));
            String insert = "INSERT INTO " + q(table.name()) + " (" + columns + ") VALUES (" + placeholders + ")";

            long count = 0;
            try (PreparedStatement ps = sqlite.prepareStatement(insert)) {
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        Object value = rs.getObject(i);
                        bindSqliteValue(ps, i, value);
                    }
                    ps.addBatch();
                    count++;

                    if (count % 500 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }

            return count;
        }
    }

    private static void bindSqliteValue(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setObject(index, null);
            return;
        }

        if (value instanceof Boolean b) {
            ps.setInt(index, b ? 1 : 0);
            return;
        }

        if (value instanceof Timestamp ts) {
            ps.setString(index, ts.toLocalDateTime().toString());
            return;
        }

        if (value instanceof java.sql.Date dt) {
            ps.setString(index, dt.toLocalDate().toString());
            return;
        }

        if (value instanceof LocalDateTime ldt) {
            ps.setString(index, ldt.toString());
            return;
        }

        if (value instanceof LocalDate ld) {
            ps.setString(index, ld.toString());
            return;
        }

        if (value instanceof BigDecimal bd) {
            ps.setBigDecimal(index, bd);
            return;
        }

        ps.setObject(index, value);
    }

    private static boolean existsSqliteTable(Connection sqlite, String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name = ?";
        try (PreparedStatement ps = sqlite.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static long countRows(Connection conn, String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + q(table);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static String mapSqlTypeToSqlite(String h2Type) {
        if (h2Type == null) {
            return "TEXT";
        }
        String type = h2Type.toUpperCase(Locale.ROOT);

        if (containsAny(type, "INT", "SERIAL", "BIGSERIAL", "IDENTITY")) {
            return "INTEGER";
        }
        if (containsAny(type, "DECIMAL", "NUMERIC", "REAL", "DOUBLE", "FLOAT", "DECFLOAT")) {
            return "REAL";
        }
        if (containsAny(type, "BLOB", "BINARY", "VARBINARY", "BYTEA")) {
            return "BLOB";
        }
        if (containsAny(type, "BOOLEAN", "BIT")) {
            return "INTEGER";
        }
        if (containsAny(type, "DATE", "TIME", "TIMESTAMP")) {
            return "TEXT";
        }
        return "TEXT";
    }

    private static boolean isIntegerLike(String mappedType) {
        return "INTEGER".equalsIgnoreCase(mappedType);
    }

    private static boolean containsAny(String text, String... needles) {
        return Arrays.stream(needles).anyMatch(text::contains);
    }

    private static String q(String name) {
        String escaped = name.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private record PrimaryKeyColumn(String columnName, int keySeq) {
    }

    private record ColumnDefinition(String name, String typeName, boolean nullable, boolean autoIncrement) {
    }

    private record TableDefinition(String name, List<ColumnDefinition> columns, List<String> primaryKeys) {
    }

    private record MigrationSummary(int tableCount, long totalRows) {
    }

    private record ValidationSummary(int checkedTables, List<String> mismatches) {
        boolean ok() {
            return mismatches == null || mismatches.isEmpty();
        }
    }
}
