package org.mouni.ds.sqlrunner.repo;


import org.mouni.ds.sqlrunner.model.ScriptRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ScriptHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ScriptHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ScriptRecord> ROW_MAPPER = new RowMapper<>() {
        @Override
        public ScriptRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ScriptRecord(
                    rs.getLong("id"),
                    rs.getString("script_name"),
                    rs.getString("script_type"),
                    rs.getInt("major_version"),
                    rs.getInt("minor_version"),
                    rs.getString("checksum"),
                    rs.getString("status"),
                    rs.getTimestamp("executed_at") != null
                            ? rs.getTimestamp("executed_at").toInstant()
                            : null,
                    rs.getString("error_message")
            );
        }
    };

    public Optional<ScriptRecord> findByScriptName(String scriptName) {
        List<ScriptRecord> list = jdbcTemplate.query(
                "SELECT * FROM script_history WHERE script_name = ?",
                ROW_MAPPER,
                scriptName
        );
        return list.stream().findFirst();
    }

    public List<ScriptRecord> findAll() {
        return jdbcTemplate.query("SELECT * FROM script_history ORDER BY script_name", ROW_MAPPER);
    }

    public void insert(String scriptName,
                       String scriptType,
                       int major,
                       int minor,
                       String checksum,
                       String status) {
        jdbcTemplate.update(
                "INSERT INTO script_history (script_name, script_type, major_version, minor_version, checksum, status) VALUES (?, ?, ?, ?, ?, ?)",
                scriptName, scriptType, major, minor, checksum, status
        );
    }

    public void updateStatus(String scriptName,
                             String checksum,
                             String status,
                             java.time.Instant executedAt,
                             String errorMessage) {
        jdbcTemplate.update(
                "UPDATE script_history SET checksum = ?, status = ?, executed_at = ?, error_message = ? WHERE script_name = ?",
                checksum,
                status,
                executedAt != null ? java.sql.Timestamp.from(executedAt) : null,
                errorMessage,
                scriptName
        );
    }
}
