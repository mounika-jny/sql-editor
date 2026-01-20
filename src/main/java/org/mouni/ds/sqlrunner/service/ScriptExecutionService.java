package org.mouni.ds.sqlrunner.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mouni.ds.sqlrunner.config.RunnerProperties;
import org.mouni.ds.sqlrunner.config.ScriptProperties;
import org.mouni.ds.sqlrunner.model.ScriptMeta;
import org.mouni.ds.sqlrunner.model.ScriptRecord;
import org.mouni.ds.sqlrunner.repo.LockRepository;
import org.mouni.ds.sqlrunner.repo.ScriptHistoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ConditionalOnProperty(prefix = "scripts", name = "enabled", havingValue = "true", matchIfMissing = true)
@Service
public class ScriptExecutionService {

    private static final Logger log = LogManager.getLogger(ScriptExecutionService.class);

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^(ddl|dml)_V(\\d+)\\.(\\d+)_([A-Za-z0-9_]+)\\.sql$");

    private static final String GLOBAL_LOCK_NAME = "GLOBAL_SCRIPT_RUN";

    private final ScriptProperties scriptProperties;
    private final ScriptHistoryRepository historyRepository;
    private final LockRepository lockRepository;
    private final RunnerProperties runnerProperties;
    private final DataSource dataSource;

    public ScriptExecutionService(ScriptProperties scriptProperties,
                                  ScriptHistoryRepository historyRepository,
                                  LockRepository lockRepository,
                                  RunnerProperties runnerProperties,
                                  DataSource dataSource) {
        this.scriptProperties = scriptProperties;
        this.historyRepository = historyRepository;
        this.lockRepository = lockRepository;
        this.runnerProperties = runnerProperties;
        this.dataSource = dataSource;
    }

    public void runAllScripts() throws Exception {
        String nodeId = runnerProperties.getNodeId();

        boolean acquired = lockRepository.tryAcquireLock(GLOBAL_LOCK_NAME, nodeId);
        if (!acquired) {
            log.info("[LOCK] Another node holds the script lock. nodeId={} skipping run.", nodeId);
            return;
        }

        log.info("[LOCK] Lock acquired by nodeId={} for running all scripts.", nodeId);

        try {
            List<Path> scripts = listSqlFiles(scriptProperties.getDir());

            for (Path scriptPath : scripts) {
                String fileName = scriptPath.getFileName().toString();
                ScriptMeta meta = parseScriptName(fileName);
                if (meta == null) {
                    log.info("[SKIP] Name does not match pattern: {}", fileName);
                    continue;
                }

                String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
                String checksum = computeChecksum(content);

                Optional<ScriptRecord> maybeRec = historyRepository.findByScriptName(fileName);

                if (maybeRec.isEmpty()) {
                    log.info("[NEW] type={} script={}", meta.type(), fileName);
                    historyRepository.insert(fileName, meta.type(), meta.majorVersion(),
                            meta.minorVersion(), checksum, "PENDING");
                    executeAndUpdate(scriptPath, fileName, meta.type(), checksum);
                } else {
                    handleExistingScript(scriptPath, checksum, meta, maybeRec.get());
                }
            }
        } finally {
            lockRepository.releaseLock(GLOBAL_LOCK_NAME, nodeId);
            log.info("[LOCK] Lock released by nodeId={}", nodeId);
        }
    }

    public void rerunScript(String scriptName) throws Exception {
        String nodeId = runnerProperties.getNodeId();

        boolean acquired = lockRepository.tryAcquireLock(GLOBAL_LOCK_NAME, nodeId);
        if (!acquired) {
            log.info("[LOCK] Another node holds the script lock. nodeId={} cannot rerun {}", nodeId, scriptName);
            throw new IllegalStateException("Another node is running scripts. Try again later.");
        }

        log.info("[LOCK] Lock acquired by nodeId={} for rerun of {}.", nodeId, scriptName);

        try {
            Path path = Paths.get(scriptProperties.getDir()).resolve(scriptName);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Script not found: " + path.toAbsolutePath());
            }

            ScriptMeta meta = parseScriptName(scriptName);
            if (meta == null) {
                throw new IllegalArgumentException("Invalid script name pattern: " + scriptName);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            String checksum = computeChecksum(content);

            Optional<ScriptRecord> maybeRec = historyRepository.findByScriptName(scriptName);
            if (maybeRec.isEmpty()) {
                log.info("[RERUN] No record found, inserting as new: {}", scriptName);
                historyRepository.insert(scriptName, meta.type(), meta.majorVersion(),
                        meta.minorVersion(), checksum, "PENDING");
            } else {
                log.info("[RERUN] Existing status={} for {}", maybeRec.get().status(), scriptName);
            }

            executeAndUpdate(path, scriptName, meta.type(), checksum);
        } finally {
            lockRepository.releaseLock(GLOBAL_LOCK_NAME, nodeId);
            log.info("[LOCK] Lock released by nodeId={} after rerun of {}", nodeId, scriptName);
        }
    }

    public List<ScriptRecord> listHistory() {
        return historyRepository.findAll();
    }

    // internal helpers

    private void handleExistingScript(Path scriptPath,
                                      String checksum,
                                      ScriptMeta meta,
                                      ScriptRecord rec) throws IOException, SQLException {

        String name = rec.scriptName();
        String currentStatus = rec.status();

        if ("DDL".equals(meta.type())) {
            if ("SUCCESS".equals(currentStatus)) {
                if (checksum.equals(rec.checksum())) {
                    log.info("[SKIP] DDL already applied and unchanged: {}", name);
                } else {
                    log.info("[MARK] DDL changed, marking NEEDS_RERUN (manual only): {}", name);
                    historyRepository.updateStatus(name, checksum, "NEEDS_RERUN", null, null);
                }
            } else if ("FAILED".equals(currentStatus) || "NEEDS_RERUN".equals(currentStatus)) {
                log.info("[HOLD] DDL {} is {}, requires manual rerun", name, currentStatus);
            } else if ("PENDING".equals(currentStatus)) {
                log.info("[PENDING] Running DDL: {}", name);
                executeAndUpdate(scriptPath, name, meta.type(), checksum);
            }
        } else {
            if ("SUCCESS".equals(currentStatus)) {
                if (checksum.equals(rec.checksum())) {
                    log.info("[SKIP] DML already applied and unchanged: {}", name);
                } else {
                    log.info("[RUN] DML changed, re-running automatically: {}", name);
                    executeAndUpdate(scriptPath, name, meta.type(), checksum);
                }
            } else if ("FAILED".equals(currentStatus) || "NEEDS_RERUN".equals(currentStatus)) {
                if (!checksum.equals(rec.checksum())) {
                    log.info("[RETRY] DML changed after {}, re-running: {}", currentStatus, name);
                    executeAndUpdate(scriptPath, name, meta.type(), checksum);
                } else {
                    log.info("[HOLD] DML {} is {} with same checksum, requires manual rerun", name, currentStatus);
                }
            } else if ("PENDING".equals(currentStatus)) {
                log.info("[PENDING] Running DML: {}", name);
                executeAndUpdate(scriptPath, name, meta.type(), checksum);
            }
        }
    }

    private List<Path> listSqlFiles(String dir) throws IOException {
        Path folder = Paths.get(dir);
        if (!Files.exists(folder)) {
            throw new IllegalArgumentException("Scripts directory does not exist: " + folder.toAbsolutePath());
        }

        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    private ScriptMeta parseScriptName(String fileName) {
        Matcher m = NAME_PATTERN.matcher(fileName);
        if (!m.matches()) return null;

        String prefix = m.group(1);
        int major = Integer.parseInt(m.group(2));
        int minor = Integer.parseInt(m.group(3));
        String logicalName = m.group(4);

        String type = "ddl".equalsIgnoreCase(prefix) ? "DDL" : "DML";
        return new ScriptMeta(type, major, minor, logicalName, fileName);
    }

    private String computeChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not compute checksum", e);
        }
    }

    private void executeAndUpdate(Path scriptPath,
                                  String scriptName,
                                  String scriptType,
                                  String checksum) throws IOException, SQLException {

        String sqlContent = Files.readString(scriptPath, StandardCharsets.UTF_8);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeSqlScript(conn, sqlContent);
                conn.commit();
                historyRepository.updateStatus(scriptName, checksum, "SUCCESS", Instant.now(), null);
                log.info("[OK] {} ({})", scriptName, scriptType);
            } catch (SQLException e) {
                conn.rollback();
                historyRepository.updateStatus(scriptName, checksum, "FAILED", Instant.now(), e.getMessage());
                log.error("[FAIL] {} ({}): {}", scriptName, scriptType, e.getMessage());
                throw e;
            }
        }
    }

    private void executeSqlScript(Connection conn, String sqlContent) throws SQLException {
        String[] statements = sqlContent
                .replace("\r", "")
                .split(";");

        try (Statement st = conn.createStatement()) {
            for (String raw : statements) {
                String sql = raw.trim();
                if (sql.isEmpty()) continue;
                st.execute(sql);
            }
        }
    }
}
