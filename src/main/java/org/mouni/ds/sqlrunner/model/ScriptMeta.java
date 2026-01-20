package org.mouni.ds.sqlrunner.model;

public record ScriptMeta(
        String type,          // "DDL" or "DML"
        int majorVersion,
        int minorVersion,
        String logicalName,
        String fileName
) {}
