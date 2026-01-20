package org.mouni.ds.sqlrunner.model;

import java.time.Instant;

public record ScriptRecord(
        Long id,
        String scriptName,
        String scriptType,
        int majorVersion,
        int minorVersion,
        String checksum,
        String status,
        Instant executedAt,
        String errorMessage
) {}
