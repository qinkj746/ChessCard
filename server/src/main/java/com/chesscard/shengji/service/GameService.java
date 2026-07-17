package com.chesscard.shengji.service;

import com.chesscard.shengji.api.GameNotFoundException;
import com.chesscard.shengji.api.websocket.RoomEvent;
import com.chesscard.shengji.api.websocket.RoomEventPublisher;
import com.chesscard.shengji.api.PermissionDeniedException;
import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.domain.Team;
import com.chesscard.shengji.rules.DeclarationRules;
import com.chesscard.shengji.rules.DeckFactory;
import com.chesscard.shengji.rules.ScoreRules;
import com.chesscard.shengji.rules.ShengjiSorter;
import com.chesscard.shengji.rules.TrickRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chesscard.shengji.domain.RoomSeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class GameService {
    private final GameRepository repository;
    private final AiPlayer aiPlayer;
    private final RoomEventPublisher eventPublisher;
    private final GameRecordService gameRecordService;

    public GameService(GameRepository repository, AiPlayer aiPlayer) {
        this(repository, aiPlayer, RoomEventPublisher.noop(), null);
    }

    public GameService(GameRepository repository, AiPlayer aiPlayer, RoomEventPublisher eventPublisher) {
        this(repository, aiPlayer, eventPublisher, null);
    }

    @Autowired
    public GameService(GameRepository repository, AiPlayer aiPlayer, RoomEventPublisher eventPublisher, GameRecordService gameRecordService) {
        this.repository = repository;
        this.aiPlayer = aiPlayer;
        this.eventPublisher = eventPublisher;
        this.gameRecordService = gameRecordService;
    }

    public GameState createGame() {
        GameState game = new GameState();
        deal(game);
        sortSouthHand(game);
        return saveAndPublish(game);
    }

    public GameState createGameForRoom(String roomId, Map<PlayerSeat, RoomSeat> roomSeats) {
        GameState game = new GameState();
        game.setRoomId(roomId);
        for (PlayerSeat seat : PlayerSeat.values()) {
            RoomSeat roomSeat = roomSeats.get(seat);
            game.getSeatOwners().put(seat, roomSeat != null && !roomSeat.isBot() ? roomSeat.getPlayerId() : null);
        }
        deal(game);
        sortSouthHand(game);
        return saveAndPublish(game);
    }

    public GameState clearRoomSeatOwner(String roomId, PlayerSeat seat, String playerId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (seat == null) {
            throw new IllegalArgumentException("seat \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        GameState game = repository.findByRoomId(roomId)
                .orElseThrow(() -> new GameNotFoundException("\u724c\u5c40\u4e0d\u5b58\u5728"));
        String owner = game.getSeatOwners().get(seat);
        if (!playerId.equals(owner)) {
            throw new PermissionDeniedException("\u8be5\u73a9\u5bb6\u672a\u5165\u5ea7");
        }
        game.getSeatOwners().put(seat, null);
        return saveAndPublish(game);
    }

    public GameState createNextGame(String previousId) {
        GameState previous = getGame(previousId);
        if (previous.getPhase() != GamePhase.FINISHED) {
            throw new IllegalArgumentException("\u724c\u5c40\u5c1a\u672a\u7ed3\u675f\uff0c\u4e0d\u80fd\u5f00\u59cb\u4e0b\u4e00\u5c40");
        }
        if (previous.isCompleted()) {
            throw new IllegalArgumentException("\u5df2\u7ecf\u901a\u5173\uff0c\u4e0d\u80fd\u5f00\u59cb\u4e0b\u4e00\u5c40");
        }
        if (previous.getNextLevelRank() == null) {
            throw new IllegalArgumentException("\u4e0b\u4e00\u5c40\u7ea7\u724c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getLevelRank() == null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u7ea7\u724c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getWinningTeam() == null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u80dc\u65b9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getBanker() == null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u5e84\u5bb6\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getTrumpSuit() == null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u4e3b\u82b1\u8272\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getCurrentTurn() == null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (previous.getLevelRank() == Rank.TWO || previous.getNextLevelRank() == Rank.TWO) {
            throw new IllegalArgumentException("2 \u662f\u6d3b\u4e3b\uff0c\u4e0d\u80fd\u4f5c\u4e3a\u7ea7\u724c\u521b\u5efa\u4e0b\u4e00\u5c40");
        }
        if (previous.getAttackerScore() < 0 || previous.getAttackerScore() > 200) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u95f2\u5bb6\u5206\u5fc5\u987b\u5728 0 \u5230 200 \u4e4b\u95f4");
        }
        if (previous.getLevelDelta() <= 0) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u5347\u7ea7\u6b65\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        Team expectedWinningTeam = previous.getAttackerScore() >= 80 ? Team.EAST_WEST : Team.SOUTH_NORTH;
        if (previous.getWinningTeam() != expectedWinningTeam) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u80dc\u65b9\u4e0e\u95f2\u5bb6\u5206\u4e0d\u4e00\u81f4");
        }
        int expectedLevelDelta = previous.getWinningTeam() == Team.EAST_WEST
                ? ScoreRules.attackerDeltaForAttackersWin(previous.getAttackerScore())
                : ScoreRules.bankerDeltaForDefenderWin(previous.getAttackerScore());
        if (previous.getLevelDelta() != expectedLevelDelta) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u5347\u7ea7\u6b65\u6570\u4e0e\u95f2\u5bb6\u5206\u4e0d\u4e00\u81f4");
        }
        Rank expectedNextLevelRank = ScoreRules.advanceLevelRank(previous.getLevelRank(), previous.getLevelDelta());
        if (previous.getNextLevelRank() != expectedNextLevelRank) {
            throw new IllegalArgumentException("\u4e0b\u4e00\u5c40\u7ea7\u724c\u4e0e\u5347\u7ea7\u6b65\u6570\u4e0d\u4e00\u81f4");
        }
        if (previous.getLevelRank() == Rank.KING && !previous.isCompleted()) {
            throw new IllegalArgumentException("K \u7ea7\u7ed3\u675f\u724c\u5c40\u5fc5\u987b\u6807\u8bb0\u901a\u5173");
        }
        for (PlayerSeat seat : PlayerSeat.values()) {
            List<Card> hand = previous.getHands().get(seat);
            if (hand == null || !hand.isEmpty()) {
                throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u7ed3\u675f\u65f6\u73a9\u5bb6\u624b\u724c\u5fc5\u987b\u4e3a\u7a7a");
            }
        }
        if (previous.getKitty().size() != 8) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u7ed3\u675f\u65f6\u5e95\u724c\u5fc5\u987b\u4e3a 8 \u5f20");
        }
        if (!previous.getCurrentTrick().isEmpty()) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u7ed3\u675f\u65f6\u5f53\u524d\u58a9\u5fc5\u987b\u4e3a\u7a7a");
        }
        if (previous.getCurrentTrickLeader() != null) {
            throw new IllegalArgumentException("\u4e0a\u4e00\u5c40\u7ed3\u675f\u65f6\u4e0d\u5e94\u4fdd\u7559\u5f53\u524d\u5899\u9996\u653b\u5ea7\u4f4d");
        }

        recordFinishedGame(previous);

        GameState next = new GameState();
        next.setLevelRank(previous.getNextLevelRank());
        next.setBanker(representativeSeat(previous.getWinningTeam()));
        next.setCurrentTurn(next.getBanker());
        deal(next);
        sortSouthHand(next);
        return repository.save(next);
    }

    private void deal(GameState game) {
        List<Card> deck = new ArrayList<>(DeckFactory.createDoubleDeck());
        Collections.shuffle(deck);
        int cursor = 0;
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, new ArrayList<>(deck.subList(cursor, cursor + 25)));
            cursor += 25;
        }
        game.getKitty().addAll(deck.subList(cursor, cursor + 8));
    }

    public GameState getGame(String id) {
        return repository.find(id).orElseThrow(() -> new GameNotFoundException("\u724c\u5c40\u4e0d\u5b58\u5728"));
    }

    public GameState declare(String id, Suit suit) {
        return declare(id, suit, null);
    }

    public GameState declare(String id, Suit suit, String playerId) {
        GameState game = getGame(id);
        PlayerSeat seat;
        if (game.getRoomId() != null) {
            seat = resolveHumanSeat(game, playerId);
            if (seat == null) {
                throw new PermissionDeniedException("AI \u5ea7\u4f4d\u4e0d\u5141\u8bb8\u771f\u4eba\u64cd\u4f5c");
            }
        } else {
            seat = PlayerSeat.SOUTH;
            if (game.getBanker() != null && game.getBanker() != PlayerSeat.SOUTH) {
                throw new IllegalArgumentException("\u5f53\u524d\u5e84\u5bb6\u4e0d\u662f\u5357\u5bb6\uff0c\u4e0d\u80fd\u7531\u771f\u4eba\u53eb\u4e3b");
            }
        }
        return declareForSeat(game, seat, suit, false);
    }

    private GameState declareForSeat(GameState game, PlayerSeat seat, Suit suit, boolean autoKitty) {
        if (game.getPhase() != GamePhase.DECLARE) {
            throw new IllegalArgumentException("\u5f53\u524d\u9636\u6bb5\u4e0d\u80fd\u53eb\u4e3b");
        }
        if (suit == null) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u82b1\u8272\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (game.getLevelRank() == null) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u7ea7\u724c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        List<Card> hand = game.getHands().get(seat);
        if (hand == null) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (hand.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
        }
        if (hand.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
        }
        if (hand.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
        }
        if (hand.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
        }
        if (hand.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
        }
        if (hand.stream().distinct().count() != hand.size()) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
        if (!DeclarationRules.availableSuits(hand, game.getLevelRank()).contains(suit)) {
            throw new IllegalArgumentException("\u8be5\u82b1\u8272\u4e0d\u80fd\u53eb\u4e3b");
        }
        if (game.getKitty().size() != 8) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u5fc5\u987b\u4e3a 8 \u5f20");
        }
        if (game.getKitty().stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
        }
        if (game.getKitty().stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
        }
        if (game.getKitty().stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
        }
        if (game.getKitty().stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
        }
        if (game.getKitty().stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
        }
        if (game.getKitty().stream().distinct().count() != game.getKitty().size()) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u5e95\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
        if (game.getKitty().stream().anyMatch(hand::contains)) {
            throw new IllegalArgumentException("\u53eb\u4e3b\u624b\u724c\u548c\u5e95\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
        game.setTrumpSuit(suit);
        game.setBanker(seat);
        game.setCurrentTurn(seat);
        game.setPhase(GamePhase.KITTY);
        game.getHands().get(seat).addAll(game.getKitty());
        game.getKitty().clear();
        if (autoKitty) {
            List<Card> bankerHand = game.getHands().get(seat);
            game.getKitty().addAll(new ArrayList<>(bankerHand.subList(0, 8)));
            bankerHand.subList(0, 8).clear();
            game.setPhase(GamePhase.PLAY);
        }
        sortSouthHand(game);
        return saveAndPublish(game);
    }

    public GameState setKitty(String id, List<Card> cards) {
        return setKitty(id, cards, null);
    }

    public GameState setKitty(String id, List<Card> cards, String playerId) {
        GameState game = getGame(id);
        if (game.getPhase() != GamePhase.KITTY) {
            throw new IllegalArgumentException("\u5f53\u524d\u9636\u6bb5\u4e0d\u80fd\u6263\u5e95");
        }
        if (cards == null || cards.size() != 8) {
            throw new IllegalArgumentException("\u5fc5\u987b\u6263 8 \u5f20\u5e95\u724c");
        }
        if (cards.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("\u5e95\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        if (cards.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("card rank \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
            throw new IllegalArgumentException("card suit \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
            throw new IllegalArgumentException("joker suit \u5fc5\u987b\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
        }
        if (cards.stream().distinct().count() != cards.size()) {
            throw new IllegalArgumentException("cards \u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }

        // Validate cards exist in hand BEFORE checking banker/seat.
        // Single-mode tests check SOUTH's hand, then banker separately.
        List<Card> seatHand = game.getHands().get(PlayerSeat.SOUTH);
        if (seatHand == null) {
            throw new IllegalArgumentException("\u5e95\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        if (!containsEachRequestedCard(seatHand, cards)) {
            throw new IllegalArgumentException("\u5e95\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        if (!game.getKitty().isEmpty()) {
            throw new IllegalArgumentException("\u6263\u5e95\u524d\u5e95\u724c\u533a\u5fc5\u987b\u4e3a\u7a7a");
        }

        // Room-mode: permission check (override seat from playerId)
        if (game.getRoomId() != null) {
            PlayerSeat resolvedSeat = resolveHumanSeat(game, playerId);
            if (resolvedSeat == null) {
                throw new PermissionDeniedException("AI \u5ea7\u4f4d\u4e0d\u5141\u8bb8\u771f\u4eba\u64cd\u4f5c");
            }
            if (resolvedSeat != PlayerSeat.SOUTH) {
                List<Card> ownerHand = game.getHands().get(resolvedSeat);
                if (ownerHand == null) {
                    throw new IllegalArgumentException("\u5e95\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
                }
                if (!containsEachRequestedCard(ownerHand, cards)) {
                    throw new IllegalArgumentException("\u5e95\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
                }
                seatHand = ownerHand;
            }
        } else {
            // Single-mode: banker and currentTurn check AFTER hand validation
            if (game.getBanker() != PlayerSeat.SOUTH) {
                throw new IllegalArgumentException("\u771f\u4eba\u6263\u5e95\u65f6\u5e84\u5bb6\u5fc5\u987b\u4e3a\u5357\u5bb6");
            }
            if (game.getCurrentTurn() != PlayerSeat.SOUTH) {
                throw new IllegalArgumentException("\u771f\u4eba\u6263\u5e95\u65f6\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d\u5fc5\u987b\u4e3a\u5357\u5bb6");
            }
        }
        removeEachRequestedCard(seatHand, cards);
        game.getKitty().addAll(cards);
        game.setPhase(GamePhase.PLAY);
        sortSouthHand(game);
        return saveAndPublish(game);
    }

    public GameState play(String id, PlayerSeat seat, List<Card> cards) {
        return play(id, seat, cards, null);
    }

    public GameState play(String id, PlayerSeat seat, List<Card> cards, String playerId) {
        GameState game = getGame(id);
        if (game.getRoomId() != null) {
            PlayerSeat humanSeat = resolveHumanSeat(game, playerId);
            if (humanSeat == null) {
                throw new PermissionDeniedException("AI 座位不允许真人操作");
            }
            if (humanSeat != seat) {
                throw new PermissionDeniedException("不能操作其他玩家的座位");
            }
        }
        return playForSeat(game, seat, cards);
    }

    private GameState playForSeat(GameState game, PlayerSeat seat, List<Card> cards) {
        if (game.getPhase() != GamePhase.PLAY) {
            throw new IllegalArgumentException("\u5f53\u524d\u9636\u6bb5\u4e0d\u80fd\u51fa\u724c");
        }
        if (seat == null) {
            throw new IllegalArgumentException("\u51fa\u724c\u5ea7\u4f4d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (game.getCurrentTurn() == null) {
            throw new IllegalArgumentException("\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (game.getCurrentTurn() != seat) {
            throw new IllegalArgumentException("\u8fd8\u6ca1\u6709\u8f6e\u5230\u8be5\u73a9\u5bb6");
        }
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("\u81f3\u5c11\u9009\u62e9 1 \u5f20\u724c");
        }
        if (cards.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("\u51fa\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        if (cards.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("card rank \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
            throw new IllegalArgumentException("card suit \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
            throw new IllegalArgumentException("joker suit \u5fc5\u987b\u4e3a\u7a7a");
        }
        if (cards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
        }
        if (cards.stream().distinct().count() != cards.size()) {
            throw new IllegalArgumentException("cards \u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
        List<Card> hand = game.getHands().get(seat);
        if (hand == null) {
            throw new IllegalArgumentException("\u51fa\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        if (!containsEachRequestedCard(hand, cards)) {
            throw new IllegalArgumentException("\u51fa\u724c\u5fc5\u987b\u6765\u81ea\u624b\u724c");
        }
        boolean startsTrick = game.getCurrentTrick().isEmpty();
        if (startsTrick && game.getCurrentTrickLeader() != null) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u4e0d\u5e94\u4fdd\u7559\u9996\u653b\u5ea7\u4f4d");
        }
        if (!startsTrick && game.getCurrentTrickLeader() == null) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u5ea7\u4f4d");
        }
        if (!startsTrick && game.getCurrentTrick().get(trickLeader(game)) == null) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u51fa\u724c");
        }
        if (!startsTrick) {
            List<Card> leadCards = game.getCurrentTrick().get(trickLeader(game));
            if (leadCards.isEmpty()) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (leadCards.stream().anyMatch(card -> card == null)) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
            }
            if (leadCards.stream().anyMatch(card -> card.rank() == null)) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
            }
            if (leadCards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
            }
            if (leadCards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
            }
            if (leadCards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
            }
            if (leadCards.stream().distinct().count() != leadCards.size()) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
            }
        }
        if (!startsTrick && game.getCurrentTrick().containsKey(seat)) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u8be5\u5ea7\u4f4d\u5df2\u51fa\u724c");
        }
        if (!startsTrick && hasLaterSeatAlreadyPlayed(game, seat)) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u987a\u5e8f\u4e0d\u5408\u6cd5");
        }
        if (!startsTrick && hasMissingEarlierSeatWithCards(game, seat)) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u987a\u5e8f\u4e0d\u5408\u6cd5");
        }
        if (!startsTrick) {
            List<Card> leadCards = game.getCurrentTrick().get(trickLeader(game));
            for (Map.Entry<PlayerSeat, List<Card>> entry : game.getCurrentTrick().entrySet()) {
                if (entry.getKey() == trickLeader(game)) {
                    continue;
                }
                List<Card> existingCards = entry.getValue();
                if (existingCards == null || existingCards.isEmpty()) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u4e3a\u7a7a");
                }
                if (existingCards.stream().anyMatch(card -> card == null)) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
                }
                if (existingCards.stream().anyMatch(card -> card.rank() == null)) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
                }
                if (existingCards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
                }
                if (existingCards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
                }
                if (existingCards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
                }
                if (existingCards.size() != leadCards.size()) {
                    throw new IllegalArgumentException("\u5f53\u524d\u58a9\u51fa\u724c\u6570\u91cf\u4e0d\u5408\u6cd5");
                }
            }
            long currentTrickCardCount = game.getCurrentTrick().values().stream()
                    .flatMap(List::stream)
                    .count();
            long distinctCurrentTrickCardCount = game.getCurrentTrick().values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .count();
            if (currentTrickCardCount != distinctCurrentTrickCardCount) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
            }
        }
        List<Card> playedCards = normalizePlay(cards, game, startsTrick);
        if (!startsTrick && playedCards.stream().anyMatch(card -> game.getCurrentTrick().values().stream()
                .flatMap(List::stream)
                .anyMatch(card::equals))) {
            throw new IllegalArgumentException("\u5f53\u524d\u58a9\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
        if (startsTrick) {
            game.setCurrentTrickLeader(seat);
        } else {
            if (game.getCurrentTrickLeader() == null) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u5ea7\u4f4d");
            }
            List<Card> leadCards = game.getCurrentTrick().get(trickLeader(game));
            if (leadCards == null) {
                throw new IllegalArgumentException("\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u51fa\u724c");
            }
            if (playedCards.size() != leadCards.size()) {
                throw new IllegalArgumentException("\u5fc5\u987b\u8ddf\u51fa\u4e0e\u9996\u653b\u76f8\u540c\u6570\u91cf\u7684\u724c");
            }
            if (!followsLead(playedCards, hand, leadCards, game)) {
                throw new IllegalArgumentException("\u6709\u540c\u82b1\u8272\u724c\u65f6\u5fc5\u987b\u8ddf\u51fa\u9996\u653b\u82b1\u8272");
            }
        }
        hand.removeAll(playedCards);
        game.getCurrentTrick().put(seat, new ArrayList<>(playedCards));
        if (seat.next() == trickLeader(game)) {
            settleTrick(game);
        } else {
            game.setCurrentTurn(seat.next());
        }
        sortSouthHand(game);
        return saveAndPublish(game);
    }

    public GameState aiStep(String id) {
        GameState game = getGame(id);
        if (game.getPhase() == null) {
            throw new IllegalArgumentException("\u724c\u5c40\u9636\u6bb5\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (game.getPhase() == GamePhase.DECLARE) {
            if (game.getLevelRank() == null) {
                throw new IllegalArgumentException("AI \u53eb\u4e3b\u7ea7\u724c\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (game.getBanker() != null && game.getBanker() != PlayerSeat.SOUTH) {
                if (game.getCurrentTurn() != game.getBanker()) {
                    throw new IllegalArgumentException("\u9884\u8bbe AI \u5e84\u5bb6\u4e0e\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d\u4e0d\u4e00\u81f4");
                }
                List<Card> bankerHand = game.getHands().get(game.getBanker());
                validateAiDeclareHand(bankerHand);
                List<Suit> options = DeclarationRules.availableSuits(bankerHand, game.getLevelRank());
                if (options.isEmpty()) {
                    throw new IllegalArgumentException("\u9884\u8bbe AI \u5e84\u5bb6\u5f53\u524d\u4e0d\u80fd\u53eb\u4e3b");
                }
                return declareForSeat(game, game.getBanker(), options.get(0), true);
            }
            for (PlayerSeat seat : List.of(PlayerSeat.WEST, PlayerSeat.NORTH, PlayerSeat.EAST)) {
                List<Card> hand = game.getHands().get(seat);
                validateAiDeclareHand(hand);
                List<Suit> options = DeclarationRules.availableSuits(hand, game.getLevelRank());
                if (!options.isEmpty()) {
                    return declareForSeat(game, seat, options.get(0), true);
                }
            }
            throw new IllegalArgumentException("\u5f53\u524d\u65e0\u4eba\u53ef\u53eb\u4e3b\uff0c\u8bf7\u91cd\u65b0\u5f00\u5c40");
        }
        PlayerSeat seat = game.getCurrentTurn();
        if (game.getPhase() == GamePhase.PLAY && seat == null) {
            throw new IllegalArgumentException("AI \u51fa\u724c\u5ea7\u4f4d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (game.getPhase() != GamePhase.PLAY) {
            return game;
        }
        if (seat == PlayerSeat.SOUTH) {
            List<Card> southHand = game.getHands().get(PlayerSeat.SOUTH);
            if (southHand == null) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (southHand.stream().anyMatch(card -> card == null)) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
            }
            if (southHand.stream().anyMatch(card -> card.rank() == null)) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
            }
            if (southHand.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
            }
            if (southHand.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
            }
            if (southHand.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
            }
            if (southHand.stream().distinct().count() != southHand.size()) {
                throw new IllegalArgumentException("AI \u63a8\u8fdb\u771f\u4eba\u624b\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
            }
            if (southHand.isEmpty()) {
                if (game.getCurrentTrick().isEmpty() && game.getCurrentTrickLeader() != null) {
                    throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u4e0d\u5e94\u4fdd\u7559\u9996\u653b\u5ea7\u4f4d");
                }
                if (!game.getCurrentTrick().isEmpty() && game.getCurrentTrickLeader() == null) {
                    throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u5ea7\u4f4d");
                }
                if (!game.getCurrentTrick().isEmpty()) {
                    if (game.getCurrentTrick().containsKey(seat)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u8be5\u5ea7\u4f4d\u5df2\u51fa\u724c");
                    }
                    if (hasLaterSeatAlreadyPlayed(game, seat) || hasMissingEarlierSeatWithCards(game, seat)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u987a\u5e8f\u4e0d\u5408\u6cd5");
                    }
                    List<Card> leadCards = game.getCurrentTrick().get(game.getCurrentTrickLeader());
                    if (leadCards == null) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u7f3a\u5c11\u9996\u653b\u51fa\u724c");
                    }
                    if (leadCards.isEmpty()) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u4e3a\u7a7a");
                    }
                    if (leadCards.stream().anyMatch(card -> card == null)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
                    }
                    if (leadCards.stream().anyMatch(card -> card.rank() == null)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
                    }
                    if (leadCards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
                    }
                    if (leadCards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
                    }
                    if (leadCards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
                    }
                    if (leadCards.stream().distinct().count() != leadCards.size()) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u9996\u653b\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
                    }
                    for (Map.Entry<PlayerSeat, List<Card>> entry : game.getCurrentTrick().entrySet()) {
                        if (entry.getKey() == game.getCurrentTrickLeader()) {
                            continue;
                        }
                        List<Card> playedCards = entry.getValue();
                        if (playedCards == null || playedCards.isEmpty()) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u4e3a\u7a7a");
                        }
                        if (playedCards.stream().anyMatch(card -> card == null)) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
                        }
                        if (playedCards.stream().anyMatch(card -> card.rank() == null)) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
                        }
                        if (playedCards.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
                        }
                        if (playedCards.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
                        }
                        if (playedCards.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
                        }
                        if (playedCards.stream().distinct().count() != playedCards.size()) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
                        }
                        if (playedCards.size() != leadCards.size()) {
                            throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u51fa\u724c\u6570\u91cf\u4e0d\u5408\u6cd5");
                        }
                    }
                    long currentTrickCardCount = game.getCurrentTrick().values().stream()
                            .flatMap(List::stream)
                            .count();
                    long distinctCurrentTrickCardCount = game.getCurrentTrick().values().stream()
                            .flatMap(List::stream)
                            .distinct()
                            .count();
                    if (currentTrickCardCount != distinctCurrentTrickCardCount) {
                        throw new IllegalArgumentException("AI \u63a8\u8fdb\u5f53\u524d\u58a9\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
                    }
                }
                game.setCurrentTurn(seat.next());
                if (!game.getCurrentTrick().isEmpty() && game.getCurrentTurn() == game.getCurrentTrickLeader()) {
                    settleTrick(game);
                }
                return saveAndPublish(game);
            }
            return game;
        }
        List<Card> cards = aiPlayer.choosePlay(game, seat);
        if (cards.isEmpty()) {
            game.setCurrentTurn(seat.next());
            if (!game.getCurrentTrick().isEmpty() && game.getCurrentTurn() == game.getCurrentTrickLeader()) {
                settleTrick(game);
            }
            return saveAndPublish(game);
        }
        return playForSeat(game, seat, cards);
    }

    private void recordFinishedGame(GameState game) {
        if (gameRecordService != null) {
            gameRecordService.recordFinishedGame(game);
        }
    }

    private GameState saveAndPublish(GameState game) {
        GameState saved = repository.save(game);
        if (saved.getRoomId() != null) {
            eventPublisher.publish(RoomEvent.gameUpdated(saved));
        }
        return saved;
    }

    private void sortSouthHand(GameState game) {
        Suit trump = game.getTrumpSuit() == null ? Suit.HEART : game.getTrumpSuit();
        game.getHands().computeIfPresent(PlayerSeat.SOUTH,
                (seat, cards) -> ShengjiSorter.sortHand(cards, game.getLevelRank(), trump));
    }
    private boolean containsEachRequestedCard(List<Card> hand, List<Card> cards) {
        List<Card> remaining = new ArrayList<>(hand);
        for (Card card : cards) {
            if (!remaining.remove(card)) {
                return false;
            }
        }
        return true;
    }

    private void validateAiDeclareHand(List<Card> hand) {
        if (hand == null) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (hand.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u5305\u542b\u7a7a\u724c");
        }
        if (hand.stream().anyMatch(card -> card.rank() == null)) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u7f3a\u5c11\u70b9\u6570");
        }
        if (hand.stream().anyMatch(card -> !card.isJoker() && card.suit() == null)) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u666e\u901a\u724c\u4e0d\u80fd\u7f3a\u5c11\u82b1\u8272");
        }
        if (hand.stream().anyMatch(card -> card.isJoker() && card.suit() != null)) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u5927\u5c0f\u738b\u4e0d\u80fd\u643a\u5e26\u82b1\u8272");
        }
        if (hand.stream().anyMatch(card -> card.deckIndex() < 0 || card.deckIndex() > 1)) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c deckIndex \u5fc5\u987b\u4e3a 0 \u6216 1");
        }
        if (hand.stream().distinct().count() != hand.size()) {
            throw new IllegalArgumentException("AI \u53eb\u4e3b\u624b\u724c\u4e0d\u80fd\u5305\u542b\u91cd\u590d\u724c");
        }
    }


    private PlayerSeat resolveHumanSeat(GameState game, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        Map<PlayerSeat, String> owners = game.getSeatOwners();
        if (owners.isEmpty()) {
            return null;
        }
        for (Map.Entry<PlayerSeat, String> entry : owners.entrySet()) {
            if (playerId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new PermissionDeniedException("该玩家未入座");
    }

    private void removeEachRequestedCard(List<Card> hand, List<Card> cards) {
        for (Card card : cards) {
            hand.remove(card);
        }
    }

    private boolean hasLaterSeatAlreadyPlayed(GameState game, PlayerSeat seat) {
        for (PlayerSeat next = seat.next(); next != trickLeader(game); next = next.next()) {
            if (game.getCurrentTrick().containsKey(next)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMissingEarlierSeatWithCards(GameState game, PlayerSeat seat) {
        for (PlayerSeat previous = trickLeader(game).next(); previous != seat; previous = previous.next()) {
            if (game.getCurrentTrick().containsKey(previous)) {
                continue;
            }
            List<Card> hand = game.getHands().get(previous);
            if (hand == null || !hand.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private TrickRules.PlayPattern leadPattern(GameState game) {
        PlayerSeat leader = trickLeader(game);
        return TrickRules.analyze(game.getCurrentTrick().get(leader), game.getLevelRank(), game.getTrumpSuit());
    }

    private boolean followsLead(List<Card> playedCards, List<Card> handBeforePlay, List<Card> leadCards, GameState game) {
        TrickRules.PlayPattern leadPattern = analyzeOrNull(leadCards, game);
        if (leadPattern != null) {
            TrickRules.PlayPattern playedPattern = analyzeOrNull(playedCards, game);
            return (playedPattern == null || playedPattern.cardCount() == leadPattern.cardCount())
                    && TrickRules.followsLead(playedCards, handBeforePlay, leadPattern, game.getLevelRank(), game.getTrumpSuit());
        }
        Suit leadSuit = TrickRules.effectiveSuit(leadCards.get(0), game.getLevelRank(), game.getTrumpSuit());
        long playableLeadSuitCount = handBeforePlay.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == leadSuit)
                .count();
        long followedCount = playedCards.stream()
                .filter(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == leadSuit)
                .count();
        return followedCount == Math.min(playedCards.size(), playableLeadSuitCount);
    }

    private List<Card> normalizePlay(List<Card> cards, GameState game, boolean startsTrick) {
        try {
            TrickRules.analyze(cards, game.getLevelRank(), game.getTrumpSuit());
            game.setLastActionMessage(null);
            return cards;
        } catch (IllegalArgumentException ex) {
            if (!startsTrick) {
                List<Card> leadCards = game.getCurrentTrick().get(trickLeader(game));
                if (cards.size() == leadCards.size()) {
                    game.setLastActionMessage(null);
                    return cards;
                }
                throw ex;
            }
            if (cards.size() == 1) {
                throw ex;
            }
            if (isSuccessfulThrow(cards, game)) {
                game.setLastActionMessage(null);
                return cards;
            }
            game.setLastActionMessage("甩牌失败，已按最小单张出牌");
            return List.of(TrickRules.lowestSingle(cards, game.getLevelRank(), game.getTrumpSuit()));
        }
    }

    private boolean isSuccessfulThrow(List<Card> cards, GameState game) {
        Suit throwSuit = TrickRules.effectiveSuit(cards.get(0), game.getLevelRank(), game.getTrumpSuit());
        if (cards.stream().anyMatch(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) != throwSuit)) {
            return false;
        }
        for (Card thrownCard : cards) {
            TrickRules.PlayPattern thrownSingle = TrickRules.analyze(List.of(thrownCard), game.getLevelRank(), game.getTrumpSuit());
            for (Map.Entry<PlayerSeat, List<Card>> entry : game.getHands().entrySet()) {
                if (entry.getKey() == game.getCurrentTurn()) {
                    continue;
                }
                for (Card candidate : entry.getValue()) {
                    if (TrickRules.effectiveSuit(candidate, game.getLevelRank(), game.getTrumpSuit()) != throwSuit) {
                        continue;
                    }
                    TrickRules.PlayPattern candidateSingle = TrickRules.analyze(List.of(candidate), game.getLevelRank(), game.getTrumpSuit());
                    if (TrickRules.beats(candidateSingle, thrownSingle)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void settleTrick(GameState game) {
        PlayerSeat winner = trickLeader(game);
        TrickRules.PlayPattern winningPattern = analyzeOrNull(game.getCurrentTrick().get(winner), game);
        if (winningPattern == null) {
            settleTrickForWinner(game, throwTrickWinner(game), null);
            return;
        }
        for (PlayerSeat seat = winner.next(); seat != winner; seat = seat.next()) {
            TrickRules.PlayPattern challenger = analyzeOrNull(game.getCurrentTrick().get(seat), game);
            if (challenger == null) {
                continue;
            }
            if (TrickRules.beats(challenger, winningPattern)) {
                winner = seat;
                winningPattern = challenger;
            }
        }
        settleTrickForWinner(game, winner, winningPattern);
    }

    private TrickRules.PlayPattern analyzeOrNull(List<Card> cards, GameState game) {
        try {
            return TrickRules.analyze(cards, game.getLevelRank(), game.getTrumpSuit());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private PlayerSeat throwTrickWinner(GameState game) {
        PlayerSeat leader = trickLeader(game);
        Suit leadSuit = TrickRules.effectiveSuit(game.getCurrentTrick().get(leader).get(0),
                game.getLevelRank(), game.getTrumpSuit());
        PlayerSeat winner = leader;
        Card highestTrump = null;
        for (PlayerSeat seat = leader.next(); seat != leader; seat = seat.next()) {
            List<Card> playedCards = game.getCurrentTrick().get(seat);
            boolean allTrump = playedCards.stream()
                    .allMatch(card -> TrickRules.effectiveSuit(card, game.getLevelRank(), game.getTrumpSuit()) == game.getTrumpSuit());
            if (leadSuit != game.getTrumpSuit() && allTrump) {
                Card seatHighTrump = highestCard(playedCards, game);
                if (highestTrump == null || TrickRules.beats(
                        TrickRules.analyze(List.of(seatHighTrump), game.getLevelRank(), game.getTrumpSuit()),
                        TrickRules.analyze(List.of(highestTrump), game.getLevelRank(), game.getTrumpSuit()))) {
                    winner = seat;
                    highestTrump = seatHighTrump;
                }
            }
        }
        return winner;
    }

    private Card highestCard(List<Card> cards, GameState game) {
        Card highest = cards.get(0);
        for (Card card : cards.subList(1, cards.size())) {
            if (TrickRules.beats(TrickRules.analyze(List.of(card), game.getLevelRank(), game.getTrumpSuit()),
                    TrickRules.analyze(List.of(highest), game.getLevelRank(), game.getTrumpSuit()))) {
                highest = card;
            }
        }
        return highest;
    }

    private void settleTrickForWinner(GameState game, PlayerSeat winner, TrickRules.PlayPattern winningPattern) {
        int points = 0;
        if (isAttacker(winner)) {
            points = game.getCurrentTrick().values().stream()
                    .flatMap(List::stream)
                    .mapToInt(ScoreRules::cardPoints)
                    .sum();
            if (isLastTrick(game)) {
                points += kittyPoints(game) * (winningPattern == null ? throwKittyMultiplier(game) : kittyMultiplier(winningPattern));
            }
            game.addAttackerScore(points);
        }
        recordPlayedTrick(game, winner, points);
        game.getCurrentTrick().clear();
        game.setCurrentTrickLeader(null);
        game.setCurrentTurn(winner);
        if (isLastTrick(game)) {
            settleGame(game);
            game.setPhase(GamePhase.FINISHED);
            recordFinishedGame(game);
        }
    }

    private void recordPlayedTrick(GameState game, PlayerSeat winner, int points) {
        PlayerSeat leader = trickLeader(game);
        List<GameState.TrickPlay> plays = new ArrayList<>();
        for (PlayerSeat seat = leader; ; seat = seat.next()) {
            List<Card> cards = game.getCurrentTrick().get(seat);
            if (cards != null) {
                plays.add(new GameState.TrickPlay(seat, cards));
            }
            if (seat.next() == leader) {
                break;
            }
        }
        game.getPlayedTricks().add(new GameState.PlayedTrick(
                game.getPlayedTricks().size() + 1,
                leader,
                winner,
                points,
                plays
        ));
    }

    private PlayerSeat trickLeader(GameState game) {
        if (game.getCurrentTrickLeader() == null) {
            return game.getCurrentTurn();
        }
        return game.getCurrentTrickLeader();
    }

    private boolean isAttacker(PlayerSeat seat) {
        return seat == PlayerSeat.WEST || seat == PlayerSeat.EAST;
    }

    private PlayerSeat representativeSeat(Team team) {
        return team == Team.EAST_WEST ? PlayerSeat.WEST : PlayerSeat.SOUTH;
    }

    private boolean isLastTrick(GameState game) {
        return game.getHands().values().stream().allMatch(List::isEmpty);
    }

    private int kittyPoints(GameState game) {
        return game.getKitty().stream().mapToInt(ScoreRules::cardPoints).sum();
    }

    private int kittyMultiplier(TrickRules.PlayPattern winningPattern) {
        int tractorLength = winningPattern.type() == TrickRules.PlayType.TRACTOR ? winningPattern.cardCount() / 2 : 1;
        return ScoreRules.kittyMultiplier(winningPattern.type().name(), tractorLength);
    }

    private int throwKittyMultiplier(GameState game) {
        return game.getCurrentTrick().get(trickLeader(game)).size() * 2;
    }

    private void settleGame(GameState game) {
        if (game.getAttackerScore() >= 80) {
            game.setWinningTeam(Team.EAST_WEST);
            game.setLevelDelta(ScoreRules.attackerDeltaForAttackersWin(game.getAttackerScore()));
        } else {
            game.setWinningTeam(Team.SOUTH_NORTH);
            game.setLevelDelta(ScoreRules.bankerDeltaForDefenderWin(game.getAttackerScore()));
        }
        game.setNextLevelRank(ScoreRules.advanceLevelRank(game.getLevelRank(), game.getLevelDelta()));
        game.setCompleted(game.getLevelRank() == Rank.KING && game.getWinningTeam() != null);
    }
}





