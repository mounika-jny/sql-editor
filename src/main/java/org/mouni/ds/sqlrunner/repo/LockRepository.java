package org.mouni.ds.sqlrunner.repo;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class LockRepository {

    private final JdbcTemplate jdbcTemplate;

    public LockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquireLock(String lockName, String lockedBy) {
        try {
            int updated = jdbcTemplate.update(
                    "INSERT INTO script_lock (lock_name, locked_by, locked_at) VALUES (?, ?, ?)",
                    lockName,
                    lockedBy,
                    java.sql.Timestamp.from(Instant.now())
            );
            return updated == 1;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void releaseLock(String lockName, String lockedBy) {
        jdbcTemplate.update(
                "DELETE FROM script_lock WHERE lock_name = ? AND locked_by = ?",
                lockName,
                lockedBy
        );
    }
}
