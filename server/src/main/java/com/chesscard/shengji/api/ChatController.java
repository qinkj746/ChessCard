package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.ChatMessageDto;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.SendChatMessageRequest;
import com.chesscard.shengji.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@CrossOrigin(origins = "*")
public class ChatController {
    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public ChatMessageDto sendMessage(@PathVariable String roomId, @RequestBody SendChatMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return ChatMessageDto.from(service.sendMessage(roomId, request.playerId(), request.content()));
    }

    @GetMapping
    public List<ChatMessageDto> listMessages(@PathVariable String roomId) {
        return service.listMessages(roomId).stream().map(ChatMessageDto::from).toList();
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(PermissionDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of("PERMISSION_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(GameNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of("ROOM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_CHAT_MESSAGE", ex.getMessage()));
    }
}