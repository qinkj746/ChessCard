package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.ChatMessageDto;
import com.chesscard.shengji.api.dto.SendChatMessageRequest;
import com.chesscard.shengji.domain.ChatMessage;
import com.chesscard.shengji.service.ChatService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerTest {
    @Test
    void sendMessageReturnsMessageDto() {
        ChatController controller = new ChatController(new StubChatService());

        ChatMessageDto dto = controller.sendMessage("room-1", new SendChatMessageRequest("player-1", "hello"));

        assertThat(dto.messageId()).isEqualTo("message-1");
        assertThat(dto.roomId()).isEqualTo("room-1");
        assertThat(dto.senderPlayerId()).isEqualTo("player-1");
        assertThat(dto.content()).isEqualTo("hello");
    }

    @Test
    void listMessagesReturnsMessageDtos() {
        ChatController controller = new ChatController(new StubChatService());

        List<ChatMessageDto> messages = controller.listMessages("room-1");

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).content()).isEqualTo("hello");
    }

    @Test
    void permissionFailureMapsToForbidden() {
        ChatController controller = new ChatController(new StubChatService());

        var response = controller.forbidden(new PermissionDeniedException("not a member"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().code()).isEqualTo("PERMISSION_DENIED");
    }

    private static class StubChatService extends ChatService {
        StubChatService() {
            super(null, null, null);
        }

        @Override
        public ChatMessage sendMessage(String roomId, String senderPlayerId, String content) {
            return message(roomId, senderPlayerId, content);
        }

        @Override
        public List<ChatMessage> listMessages(String roomId) {
            return List.of(message(roomId, "player-1", "hello"));
        }

        private ChatMessage message(String roomId, String senderPlayerId, String content) {
            return new ChatMessage(
                    "message-1",
                    roomId,
                    senderPlayerId,
                    content,
                    Instant.parse("2026-07-10T08:00:00Z")
            );
        }
    }
}