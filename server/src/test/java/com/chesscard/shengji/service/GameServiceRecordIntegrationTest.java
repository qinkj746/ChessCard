package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameRecord;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.domain.Team;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceRecordIntegrationTest {
    @Test
    void playRecordsGameWhenLastTrickFinishes() {
        FakeGameRepository repository = new FakeGameRepository();
        RecordingGameRecordService records = new RecordingGameRecordService();
        GameService service = new GameService(repository, new AiPlayer(), RoomEventPublisher.noop(), records);
        GameState game = playingGame();
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        Card spadeSix = card(Suit.SPADE, Rank.SIX, 0);
        Card spadeKing = card(Suit.SPADE, Rank.KING, 0);
        Card spadeTen = card(Suit.SPADE, Rank.TEN, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(spadeFive)));
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(spadeSix)));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(spadeKing)));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(spadeTen)));
        repository.save(game);

        service.play(game.getId(), PlayerSeat.SOUTH, List.of(spadeFive));
        service.play(game.getId(), PlayerSeat.WEST, List.of(spadeSix));
        service.play(game.getId(), PlayerSeat.NORTH, List.of(spadeKing));
        service.play(game.getId(), PlayerSeat.EAST, List.of(spadeTen));

        assertThat(records.recordedGameIds).containsExactly(game.getId());
    }

    @Test
    void createNextGameRecordsPreviousFinishedGame() {
        FakeGameRepository repository = new FakeGameRepository();
        RecordingGameRecordService records = new RecordingGameRecordService();
        GameService service = new GameService(repository, new AiPlayer(), RoomEventPublisher.noop(), records);
        GameState previous = finishedGame();
        repository.save(previous);

        service.createNextGame(previous.getId());

        assertThat(records.recordedGameIds).containsExactly(previous.getId());
    }

    private static GameState playingGame() {
        GameState game = new GameState();
        game.setPhase(GamePhase.PLAY);
        game.setTrumpSuit(Suit.HEART);
        game.setLevelRank(Rank.ACE);
        game.setBanker(PlayerSeat.SOUTH);
        game.setCurrentTurn(PlayerSeat.SOUTH);
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, new ArrayList<>());
        }
        return game;
    }

    private static GameState finishedGame() {
        GameState game = playingGame();
        game.setPhase(GamePhase.FINISHED);
        game.setWinningTeam(Team.SOUTH_NORTH);
        game.setAttackerScore(70);
        game.setLevelDelta(1);
        game.setNextLevelRank(Rank.THREE);
        game.getKitty().addAll(List.of(
                card(Suit.CLUB, Rank.THREE, 0),
                card(Suit.CLUB, Rank.FOUR, 0),
                card(Suit.CLUB, Rank.FIVE, 0),
                card(Suit.CLUB, Rank.SIX, 0),
                card(Suit.CLUB, Rank.SEVEN, 0),
                card(Suit.CLUB, Rank.EIGHT, 0),
                card(Suit.CLUB, Rank.NINE, 0),
                card(Suit.CLUB, Rank.TEN, 0)
        ));
        return game;
    }

    private static Card card(Suit suit, Rank rank, int deckIndex) {
        return new Card(suit, rank, deckIndex);
    }

    private static class RecordingGameRecordService extends GameRecordService {
        final List<String> recordedGameIds = new ArrayList<>();

        RecordingGameRecordService() {
            super(null);
        }

        @Override
        public GameRecord recordFinishedGame(GameState game) {
            recordedGameIds.add(game.getId());
            return null;
        }
    }

    private static class FakeGameRepository implements GameRepository {
        private final Map<String, GameState> games = new java.util.HashMap<>();

        @Override
        public GameState save(GameState game) {
            games.put(game.getId(), game);
            return game;
        }

        @Override
        public Optional<GameState> find(String id) {
            return Optional.ofNullable(games.get(id));
        }

        @Override
        public Optional<GameState> findByRoomId(String roomId) {
            return Optional.empty();
        }
    }
}
