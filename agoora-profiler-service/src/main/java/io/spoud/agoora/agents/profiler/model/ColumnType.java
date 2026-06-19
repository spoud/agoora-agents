package io.spoud.agoora.agents.profiler.model;

public enum ColumnType {
    STRING,
    TEXT,       // high-cardinality free-form strings
    CATEGORICAL, // low-cardinality strings
    NUMBER,
    INTEGER,
    BOOLEAN,
    DATE,
    TIMESTAMP,
    URL,
    EMAIL,
    NULL,
    MIXED
}
