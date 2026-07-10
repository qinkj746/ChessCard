package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.AuthSessionDto;
import com.chesscard.shengji.api.dto.LoginRequest;
import com.chesscard.shengji.api.dto.LogoutRequest;
import com.chesscard.shengji.api.dto.RegisterRequest;
import com.chesscard.shengji.domain.AuthSession;
import com.chesscard.shengji.service.AuthService;
import com.chesscard.shengji.service.AuthenticationFailedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {
    @Test
    void registerReturnsSessionDto() {
        AuthController controller = new AuthController(new StubAuthService());

        AuthSessionDto dto = controller.register(new RegisterRequest("alice", "secret123", "guest-1"));

        assertThat(dto.playerId()).isEqualTo("player-1");
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.sessionToken()).isEqualTo("token-1");
    }

    @Test
    void loginReturnsSessionDto() {
        AuthController controller = new AuthController(new StubAuthService());

        AuthSessionDto dto = controller.login(new LoginRequest("alice", "secret123"));

        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.sessionToken()).isEqualTo("token-1");
    }

    @Test
    void logoutDelegatesToService() {
        StubAuthService service = new StubAuthService();
        AuthController controller = new AuthController(service);

        controller.logout(new LogoutRequest("token-1"));

        assertThat(service.loggedOutToken).isEqualTo("token-1");
    }

    @Test
    void authenticationFailureMapsToUnauthorized() {
        AuthController controller = new AuthController(new StubAuthService());

        var response = controller.unauthorized(new AuthenticationFailedException("bad credentials"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().code()).isEqualTo("AUTH_FAILED");
    }

    private static class StubAuthService extends AuthService {
        String loggedOutToken;

        StubAuthService() {
            super(null, null);
        }

        @Override
        public AuthSession register(String username, String password, String playerId) {
            return new AuthSession("player-1", username, username, "token-1");
        }

        @Override
        public AuthSession login(String username, String password) {
            return new AuthSession("player-1", username, username, "token-1");
        }

        @Override
        public void logout(String sessionToken) {
            loggedOutToken = sessionToken;
        }
    }
}