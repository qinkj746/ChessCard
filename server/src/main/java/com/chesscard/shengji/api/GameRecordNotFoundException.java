package com.chesscard.shengji.api;

public class GameRecordNotFoundException extends RuntimeException {
    public GameRecordNotFoundException(String message) {
        super(message);
    }
}