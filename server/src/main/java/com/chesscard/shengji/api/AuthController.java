package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.AuthSessionDto;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.LoginRequest;
import com.chesscard.shengji.api.dto.LogoutRequest;
import com.chesscard.shengji.api.dto.RegisterRequest;
import com.chesscard.shengji.service.AuthService;
import com.chesscard.shengji.service.AuthenticationFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthSessionDto register(@RequestBody RegisterRequest request) {
        return AuthSessionDto.from(service.register(request.username(), request.password(), request.playerId()));
    }

    @PostMapping("/login")
    public AuthSessionDto login(@RequestBody LoginRequest request) {
        return AuthSessionDto.from(service.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    public void logout(@RequestBody LogoutRequest request) {
        service.logout(request.sessionToken());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> unauthorized(AuthenticationFailedException ex) {
        return ResponseEntity.status(401).body(ErrorResponse.of("AUTH_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_AUTH_REQUEST", ex.getMessage()));
    }
}