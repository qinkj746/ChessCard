package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.CardDto;
import com.chesscard.shengji.api.dto.DeclareRequest;
import com.chesscard.shengji.api.dto.ErrorResponse;
import com.chesscard.shengji.api.dto.KittyRequest;
import com.chesscard.shengji.api.dto.PlayRequest;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameControllerErrorTest {
    @Test
    void mapsGameNotFoundTo404() {
        GameController controller = new GameController(null);

        var response = controller.notFound(new GameNotFoundException("牌局不存在"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("GAME_NOT_FOUND");
        assertThat(body.message()).isEqualTo("牌局不存在");
        assertThat(body.requestId()).isNotBlank();
    }

    @Test
    void mapsIllegalOperationTo400() {
        GameController controller = new GameController(null);

        var response = controller.badRequest(new IllegalArgumentException("当前阶段不能出牌"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INVALID_OPERATION");
        assertThat(body.message()).isEqualTo("当前阶段不能出牌");
        assertThat(body.requestId()).isNotBlank();
    }

    @Test
    void mapsPermissionDeniedTo403() {
        GameController controller = new GameController(null);

        var response = controller.forbidden(new PermissionDeniedException("不能操作其他玩家的座位"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("PERMISSION_DENIED");
        assertThat(body.message()).isEqualTo("不能操作其他玩家的座位");
        assertThat(body.requestId()).isNotBlank();
    }

    @Test
    void errorResponseGeneratesUniqueRequestId() {
        GameController controller = new GameController(null);

        var r1 = controller.badRequest(new IllegalArgumentException("err"));
        var r2 = controller.badRequest(new IllegalArgumentException("err"));

        assertThat(r1.getBody()).isNotNull();
        assertThat(r2.getBody()).isNotNull();
        assertThat(r1.getBody().requestId()).isNotEqualTo(r2.getBody().requestId());
    }

    @Test
    void rejectsMissingCardListBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyCardListBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardItemBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(java.util.Collections.singletonList(null), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, java.util.Collections.singletonList(null), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCardRankBeforeCallingService() {
        GameController controller = new GameController(null);
        var cardWithoutRank = new CardDto(Suit.HEART, null, 0);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(List.of(cardWithoutRank), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, List.of(cardWithoutRank), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingNonJokerCardSuitBeforeCallingService() {
        GameController controller = new GameController(null);
        var cardWithoutSuit = new CardDto(null, Rank.ACE, 0);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(List.of(cardWithoutSuit), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, List.of(cardWithoutSuit), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsJokerCardSuitBeforeCallingService() {
        GameController controller = new GameController(null);
        var jokerWithSuit = new CardDto(Suit.HEART, Rank.BIG_JOKER, 0);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(List.of(jokerWithSuit), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, List.of(jokerWithSuit), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidCardDeckIndexBeforeCallingService() {
        GameController controller = new GameController(null);
        var cardWithInvalidDeckIndex = new CardDto(Suit.HEART, Rank.ACE, 2);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(List.of(cardWithInvalidDeckIndex), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, List.of(cardWithInvalidDeckIndex), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateCardBeforeCallingService() {
        GameController controller = new GameController(null);
        var card = new CardDto(Suit.HEART, Rank.ACE, 0);
        var duplicatedCards = List.of(card, card);

        assertThatThrownBy(() -> controller.kitty("game-1", new KittyRequest(duplicatedCards, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(PlayerSeat.SOUTH, duplicatedCards, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingRequestBodyBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.declare("game-1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.kitty("game-1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play("game-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankGameIdBeforeCallingService() {
        GameController controller = new GameController(null);
        var card = new CardDto(Suit.HEART, Rank.ACE, 0);

        assertThatThrownBy(() -> controller.get(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.declare(" ", new DeclareRequest(Suit.HEART, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.kitty(" ", new KittyRequest(List.of(card), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.play(" ", new PlayRequest(PlayerSeat.SOUTH, List.of(card), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.aiStep(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.next(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingDeclareSuitBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.declare("game-1", new DeclareRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingPlaySeatBeforeCallingService() {
        GameController controller = new GameController(null);

        assertThatThrownBy(() -> controller.play("game-1", new PlayRequest(null, List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
