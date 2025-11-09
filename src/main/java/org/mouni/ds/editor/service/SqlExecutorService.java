package org.mouni.ds.editor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.mouni.ds.editor.model.SqlRequestDto;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SqlExecutorService {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutorService.class);

    private final JdbcTemplate jdbcTemplate;

    public SqlExecutorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Object execute(SqlRequestDto request) {
        String sql = request.getQuery();
        if (!StringUtils.hasText(sql)) {
            log.warn("Rejected empty SQL query");
            throw new IllegalArgumentException("SQL query must not be empty");
        }

        String leading = leadingKeyword(sql);
        log.debug("Executing SQL statement with leading keyword: {}", leading);

        switch (leading) {
            case "SELECT":
                log.debug("Executing SELECT query");
                return executeSelect(sql, request);
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                log.debug("Executing {} statement", leading);
                return executeUpdateCount(sql);
            case "CREATE":
            case "ALTER":
            case "DROP":
            case "TRUNCATE":
            case "RENAME":
            case "GRANT":
            case "REVOKE":
                log.debug("Executing DDL statement: {}", leading);
                return executeDdl(sql);
            default:
                log.debug("Unknown keyword '{}', attempting as UPDATE first", leading);
                // Try update first, then DDL execute as fallback
                try {
                    return executeUpdateCount(sql);
                } catch (DataAccessException e) {
                    log.error("UPDATE failed, trying as DDL: {}", e.getMessage(), e);
                    return executeDdl(sql);
                }
        }
    }

    private String leadingKeyword(String sql) {
        String trimmed = sql.stripLeading();
        // take first token
        int spaceIdx = trimmed.indexOf(' ');
        String first = (spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx))
                .replaceAll("[^A-Za-z]", "");
        return first.toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> executeUpdateCount(String sql) {
        try {
            int updated = jdbcTemplate.update(sql);
            log.info("Update statement executed: {} row(s) affected", updated);
            Map<String, Object> result = new HashMap<>();
            result.put("updated", updated);
            return result;
        } catch (DataAccessException e) {
            log.error("Failed to execute update statement: {}", e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> executeDdl(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("DDL statement executed successfully");
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Executed");
            return result;
        } catch (DataAccessException e) {
            log.error("Failed to execute DDL statement: {}", e.getMessage());
            throw e;
        }
    }

    private List<Map<String, Object>> executeSelect(String sql, SqlRequestDto req) {
        int page = req.getPage() == null || req.getPage() < 0 ? 0 : req.getPage();
        int size = req.getSize() == null || req.getSize() <= 0 ? 20 : req.getSize();
        int offset = page * size;

        log.debug("SELECT query pagination: page={}, size={}, offset={}", page, size, offset);

        String lower = sql.toLowerCase(Locale.ROOT);
        boolean hasLimit = lower.contains(" limit ");
        boolean hasOffset = lower.contains(" offset ");

        String pagedSql;
        Object[] params;

        if (hasLimit || hasOffset) {
            log.debug("Query already contains LIMIT/OFFSET, using as-is");
            // Trust user-provided pagination; do not wrap
            pagedSql = sql;
            params = new Object[]{};
        } else {
            log.debug("Wrapping query with pagination");
            // Portable enough for PostgreSQL/H2/MySQL/MariaDB
            pagedSql = "SELECT * FROM ( " + sql + " ) AS sub LIMIT ? OFFSET ?";
            params = new Object[]{size, offset};
        }

        try {
            List<Map<String, Object>> results = params.length == 0
                    ? jdbcTemplate.queryForList(pagedSql)
                    : jdbcTemplate.queryForList(pagedSql, params);
            log.info("SELECT query executed successfully, returned {} row(s)", results.size());
            return results;
        } catch (DataAccessException e) {
            log.error("Failed to execute SELECT query: {}", e.getMessage());
            throw e;
        }
    }
}
