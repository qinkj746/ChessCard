package com.chesscard.shengji.api.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String database,
        String redis,
        String version,
        Instant time
) {
}