package com.chesscard.shengji.api.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketRoomEventPublisher implements RoomEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketRoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(RoomEvent event) {
        if (event == null || event.roomId() == null || event.roomId().isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + event.roomId(), event);
    }
}
