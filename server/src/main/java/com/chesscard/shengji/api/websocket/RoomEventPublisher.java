package com.chesscard.shengji.api.websocket;

public interface RoomEventPublisher {
    void publish(RoomEvent event);

    static RoomEventPublisher noop() {
        return event -> {
        };
    }
}
