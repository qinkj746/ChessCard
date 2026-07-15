package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceCreateTest {
    @Test
    void createGameForRoomMapsBotsToAiOwners() {
        GameService service = new GameService(new FakeGameRepository(), new AiPlayer());
        Map<PlayerSeat, RoomSeat> roomSeats = new EnumMap<>(PlayerSeat.class);
        roomSeats.put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, "player-1", Instant.EPOCH));
        roomSeats.put(PlayerSeat.NORTH, new RoomSeat(PlayerSeat.NORTH, "player-2", Instant.EPOCH));
        RoomSeat westBot = RoomSeat.bot(PlayerSeat.WEST, Instant.EPOCH);
        westBot.setPlayerId("stale-bot-id");
        roomSeats.put(PlayerSeat.WEST, westBot);
        roomSeats.put(PlayerSeat.EAST, RoomSeat.bot(PlayerSeat.EAST, Instant.EPOCH));

        GameState game = service.createGameForRoom("room-1", roomSeats);

        assertThat(game.getSeatOwners().get(PlayerSeat.SOUTH)).isEqualTo("player-1");
        assertThat(game.getSeatOwners().get(PlayerSeat.NORTH)).isEqualTo("player-2");
        assertThat(game.getSeatOwners().get(PlayerSeat.WEST)).isNull();
        assertThat(game.getSeatOwners().get(PlayerSeat.EAST)).isNull();
    }

    @Test
    void createGameDealsTwentyFiveCardsToEachPlayerAndEightKittyCardsWithoutDuplicates() {
        GameService service = new GameService(new FakeGameRepository(), new AiPlayer());

        GameState game = service.createGame();

        assertThat(game.getHands()).containsOnlyKeys(PlayerSeat.values());
        assertThat(game.getHands().values()).allSatisfy(hand -> assertThat(hand).hasSize(25));
        assertThat(game.getKitty()).hasSize(8);
        List<Card> allCards = Stream.concat(
                        game.getHands().values().stream().flatMap(List::stream),
                        game.getKitty().stream()
                )
                .toList();
        assertThat(allCards).hasSize(108);
        assertThat(allCards.stream().distinct()).hasSize(108);
    }

    @Test
    void rejectsMissingDeclareSuitWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card bigJoker = card(null, Rank.BIG_JOKER, 0);
        Card heartAce = card(Suit.HEART, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(bigJoker, heartAce)));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u82b1\u8272");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(bigJoker, heartAce);
    }

    @Test
    void rejectsMissingSouthHandWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.getHands().remove(PlayerSeat.SOUTH);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u624b\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands()).doesNotContainKey(PlayerSeat.SOUTH);
    }

    @Test
    void rejectsNullCardWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        List<Card> hand = new ArrayList<>();
        hand.add(null);
        hand.add(card(Suit.HEART, Rank.ACE, 0));
        game.getHands().put(PlayerSeat.SOUTH, hand);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u7a7a\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(hand);
    }

    @Test
    void rejectsMissingRankWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card brokenCard = card(Suit.HEART, null, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u70b9\u6570");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsMissingSuitForNonJokerCardWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card brokenCard = card(null, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u82b1\u8272");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsSuitForJokerWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card brokenJoker = card(Suit.SPADE, Rank.BIG_JOKER, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                brokenJoker,
                card(Suit.HEART, Rank.ACE, 0)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5927\u5c0f\u738b");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(brokenJoker, card(Suit.HEART, Rank.ACE, 0));
    }

    @Test
    void rejectsInvalidDeckIndexWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card brokenCard = card(Suit.HEART, Rank.ACE, 2);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                brokenCard
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deckIndex");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), brokenCard);
    }

    @Test
    void rejectsDuplicateCardWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card heartAce = card(Suit.HEART, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                heartAce,
                heartAce
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u91cd\u590d\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), heartAce, heartAce);
    }

    @Test
    void rejectsMissingLevelRankWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setLevelRank(null);
        Card heartAce = card(Suit.HEART, Rank.ACE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                heartAce
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u7ea7\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), heartAce);
    }

    @Test
    void rejectsInvalidKittySizeWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        Card bigJoker = card(null, Rank.BIG_JOKER, 0);
        Card heartAce = card(Suit.HEART, Rank.ACE, 0);
        Card staleKittyCard = card(Suit.CLUB, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(bigJoker, heartAce)));
        game.getKitty().add(staleKittyCard);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(bigJoker, heartAce);
        assertThat(game.getKitty()).containsExactly(staleKittyCard);
    }

    @Test
    void rejectsNullCardInKittyWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        List<Card> kittyCards = validKittyCards();
        kittyCards.set(0, null);
        GameState game = declareGameWithSouthCallableHandAndKitty(kittyCards);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u7a7a\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getKitty()).containsExactlyElementsOf(kittyCards);
    }

    @Test
    void rejectsMissingRankInKittyWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        List<Card> kittyCards = validKittyCards();
        Card brokenCard = card(Suit.CLUB, null, 0);
        kittyCards.set(0, brokenCard);
        GameState game = declareGameWithSouthCallableHandAndKitty(kittyCards);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u70b9\u6570");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getKitty()).containsExactlyElementsOf(kittyCards);
    }

    @Test
    void rejectsDuplicateCardInKittyWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        List<Card> kittyCards = validKittyCards();
        kittyCards.set(1, kittyCards.get(0));
        GameState game = declareGameWithSouthCallableHandAndKitty(kittyCards);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u91cd\u590d\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getKitty()).containsExactlyElementsOf(kittyCards);
    }

    @Test
    void rejectsDuplicateCardAcrossHandAndKittyWhenDeclaring() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        List<Card> kittyCards = validKittyCards();
        Card heartAce = card(Suit.HEART, Rank.ACE, 0);
        kittyCards.set(0, heartAce);
        GameState game = declareGameWithSouthCallableHandAndKitty(kittyCards);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u91cd\u590d\u724c");

        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getBanker()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactly(card(null, Rank.BIG_JOKER, 0), heartAce);
        assertThat(game.getKitty()).containsExactlyElementsOf(kittyCards);
    }

    @Test
    void rejectsSouthDeclarationWhenExistingBankerIsAiSeat() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = declareGameWithSouthCallableHandAndKitty(validKittyCards());
        game.setBanker(PlayerSeat.WEST);
        game.setCurrentTurn(PlayerSeat.WEST);
        repository.save(game);

        assertThatThrownBy(() -> service.declare(game.getId(), Suit.HEART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5357\u5bb6");

        assertThat(game.getBanker()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
    }

    @Test
    void aiStepDeclaresAndSetsKittyWhenExistingBankerIsWest() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = declareGameWithSouthCallableHandAndKitty(validKittyCards());
        game.setBanker(PlayerSeat.WEST);
        game.setCurrentTurn(PlayerSeat.WEST);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                card(Suit.HEART, Rank.ACE, 0),
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0)
        )));
        repository.save(game);

        GameState declared = service.aiStep(game.getId());

        assertThat(declared.getPhase()).isEqualTo(GamePhase.PLAY);
        assertThat(declared.getBanker()).isEqualTo(PlayerSeat.WEST);
        assertThat(declared.getTrumpSuit()).isEqualTo(Suit.HEART);
        assertThat(declared.getKitty()).hasSize(8);
        assertThat(declared.getHands().get(PlayerSeat.WEST)).hasSize(8);
    }

    @Test
    void rejectsAiStepWhenExistingAiBankerDoesNotMatchCurrentTurn() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = declareGameWithSouthCallableHandAndKitty(validKittyCards());
        game.setBanker(PlayerSeat.WEST);
        game.setCurrentTurn(PlayerSeat.NORTH);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                card(Suit.HEART, Rank.ACE, 0)
        )));
        game.getHands().put(PlayerSeat.NORTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                card(Suit.HEART, Rank.ACE, 1)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d");

        assertThat(game.getBanker()).isEqualTo(PlayerSeat.WEST);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.NORTH);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
        assertThat(game.getKitty()).containsExactlyElementsOf(validKittyCards());
    }

    @Test
    void rejectsAiStepWhenExistingAiBankerCannotDeclareButOtherAiCan() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = declareGameWithSouthCallableHandAndKitty(validKittyCards());
        game.setBanker(PlayerSeat.EAST);
        game.setCurrentTurn(PlayerSeat.EAST);
        game.getHands().put(PlayerSeat.WEST, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                card(Suit.HEART, Rank.ACE, 0)
        )));
        game.getHands().put(PlayerSeat.EAST, new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.aiStep(game.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u4e0d\u80fd\u53eb\u4e3b");

        assertThat(game.getBanker()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getCurrentTurn()).isEqualTo(PlayerSeat.EAST);
        assertThat(game.getTrumpSuit()).isNull();
        assertThat(game.getPhase()).isEqualTo(GamePhase.DECLARE);
    }

    @Test
    void rejectsKittyDuplicateCardMoreTimesThanHeld() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                spadeFive,
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), List.of(
                spadeFive,
                spadeFive,
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).hasSize(8);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsNullKittyCards() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        )));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).hasSize(8);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsMissingSouthHandWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        game.getHands().remove(PlayerSeat.SOUTH);
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u624b\u724c");

        assertThat(game.getHands()).doesNotContainKey(PlayerSeat.SOUTH);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsNullCardWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        selectedCards.set(0, null);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u624b\u724c");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsMissingRankWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(Suit.SPADE, null, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("card rank");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsMissingSuitForNonJokerCardWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(null, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("card suit");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsSuitForJokerWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.SMALL_JOKER, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joker suit");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsInvalidDeckIndexWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 2),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deckIndex");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsDuplicateCardWhenSettingKittyEvenIfHandContainsDuplicate() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        Card spadeFive = card(Suit.SPADE, Rank.FIVE, 0);
        List<Card> selectedCards = new ArrayList<>(List.of(
                spadeFive,
                spadeFive,
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cards");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsExistingKittyCardsWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        Card staleKittyCard = card(Suit.HEART, Rank.FIVE, 0);
        List<Card> selectedCards = new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        ));
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(selectedCards));
        game.getKitty().add(staleKittyCard);
        repository.save(game);

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5e95\u724c\u533a");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).containsExactly(staleKittyCard);
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsNonSouthBankerWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = kittyGameWithSouthHand();
        game.setBanker(PlayerSeat.WEST);
        repository.save(game);
        List<Card> selectedCards = new ArrayList<>(game.getHands().get(PlayerSeat.SOUTH));

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5e84\u5bb6");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    @Test
    void rejectsNonSouthCurrentTurnWhenSettingKitty() {
        FakeGameRepository repository = new FakeGameRepository();
        GameService service = new GameService(repository, new AiPlayer());
        GameState game = kittyGameWithSouthHand();
        game.setCurrentTurn(PlayerSeat.WEST);
        repository.save(game);
        List<Card> selectedCards = new ArrayList<>(game.getHands().get(PlayerSeat.SOUTH));

        assertThatThrownBy(() -> service.setKitty(game.getId(), selectedCards))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u5f53\u524d\u884c\u52a8\u5ea7\u4f4d");

        assertThat(game.getHands().get(PlayerSeat.SOUTH)).containsExactlyElementsOf(selectedCards);
        assertThat(game.getKitty()).isEmpty();
        assertThat(game.getPhase()).isEqualTo(GamePhase.KITTY);
    }

    private static GameState kittyGameWithSouthHand() {
        GameState game = new GameState();
        game.setPhase(GamePhase.KITTY);
        game.setBanker(PlayerSeat.SOUTH);
        game.setCurrentTurn(PlayerSeat.SOUTH);
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(Suit.SPADE, Rank.FIVE, 0),
                card(Suit.SPADE, Rank.SIX, 0),
                card(Suit.SPADE, Rank.SEVEN, 0),
                card(Suit.SPADE, Rank.EIGHT, 0),
                card(Suit.SPADE, Rank.NINE, 0),
                card(Suit.SPADE, Rank.TEN, 0),
                card(Suit.SPADE, Rank.JACK, 0),
                card(Suit.SPADE, Rank.QUEEN, 0)
        )));
        return game;
    }

    private static GameState declareGameWithSouthCallableHandAndKitty(List<Card> kittyCards) {
        GameState game = new GameState();
        game.getHands().put(PlayerSeat.SOUTH, new ArrayList<>(List.of(
                card(null, Rank.BIG_JOKER, 0),
                card(Suit.HEART, Rank.ACE, 0)
        )));
        game.getKitty().addAll(kittyCards);
        return game;
    }

    private static List<Card> validKittyCards() {
        return new ArrayList<>(List.of(
                card(Suit.CLUB, Rank.FIVE, 0),
                card(Suit.CLUB, Rank.SIX, 0),
                card(Suit.CLUB, Rank.SEVEN, 0),
                card(Suit.CLUB, Rank.EIGHT, 0),
                card(Suit.CLUB, Rank.NINE, 0),
                card(Suit.CLUB, Rank.TEN, 0),
                card(Suit.CLUB, Rank.JACK, 0),
                card(Suit.CLUB, Rank.QUEEN, 0)
        ));
    }

    private static Card card(Suit suit, Rank rank, int deckIndex) {
        return new Card(suit, rank, deckIndex);
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
    }
}
