package com.chesscard.shengji.api.dto;

import java.util.UUID;

public record ErrorResponse(String code, String message, String requestId) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, UUID.randomUUID().toString());
    }
}
