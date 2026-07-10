# 升级棋牌 MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成一个可本机运行的升级棋牌 MVP，包含 Java Spring Boot 后台规则服务和 Flutter App 牌桌客户端。

**Architecture:** 后台按领域层、应用服务层、API 层拆分；规则核心不依赖 Spring，便于测试。Flutter 使用单页牌桌 MVP，直接通过 HTTP 调用后台，后台暂用内存保存牌局。

**Tech Stack:** Java 17、Spring Boot 3、JUnit 5、Maven、Flutter、Dart、Material 3、HTTP。

---

## File Structure

```text
D:\Project\AI\ChessCard\
  server\
    pom.xml
    src\main\java\com\chesscard\shengji\
      ShengjiServerApplication.java
      api\GameController.java
      api\dto\CardDto.java
      api\dto\GameStateDto.java
      api\dto\DeclareRequest.java
      api\dto\KittyRequest.java
      api\dto\PlayRequest.java
      domain\Card.java
      domain\GamePhase.java
      domain\GameState.java
      domain\PlayerSeat.java
      domain\Rank.java
      domain\Suit.java
      domain\Team.java
      rules\DeckFactory.java
      rules\DeclarationRules.java
      rules\ScoreRules.java
      rules\ShengjiSorter.java
      service\AiPlayer.java
      service\GameService.java
      service\InMemoryGameRepository.java
    src\test\java\com\chesscard\shengji\rules\
      DeckFactoryTest.java
      DeclarationRulesTest.java
      ScoreRulesTest.java
      ShengjiSorterTest.java
  app\
    pubspec.yaml
    lib\
      main.dart
      models.dart
      api_client.dart
      game_page.dart
```

## Task 1: Scaffold Java Server

**Files:**
- Create: `server/pom.xml`
- Create: `server/src/main/java/com/chesscard/shengji/ShengjiServerApplication.java`

- [x] **Step 1: Create Spring Boot Maven project files**

Create `server/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>
    <groupId>com.chesscard</groupId>
    <artifactId>shengji-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Create `server/src/main/java/com/chesscard/shengji/ShengjiServerApplication.java`:

```java
package com.chesscard.shengji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShengjiServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShengjiServerApplication.class, args);
    }
}
```

- [x] **Step 2: Verify Maven recognizes the project**

Run:

```powershell
cd server
mvn -q -DskipTests compile
```

Expected: build succeeds.

## Task 2: Domain Model and Deck

**Files:**
- Create: `server/src/test/java/com/chesscard/shengji/rules/DeckFactoryTest.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/Card.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/Rank.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/Suit.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/PlayerSeat.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/Team.java`
- Create: `server/src/main/java/com/chesscard/shengji/rules/DeckFactory.java`

- [x] **Step 1: Write failing deck test**

Create `DeckFactoryTest.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeckFactoryTest {
    @Test
    void createsTwoDecksWith108UniqueCards() {
        List<Card> cards = DeckFactory.createDoubleDeck();

        assertThat(cards).hasSize(108);
        assertThat(Set.copyOf(cards)).hasSize(108);
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```powershell
cd server
mvn -q -Dtest=DeckFactoryTest test
```

Expected: FAIL because domain classes do not exist.

- [x] **Step 3: Add minimal domain and deck implementation**

Create `Suit.java`:

```java
package com.chesscard.shengji.domain;

public enum Suit {
    SPADE, HEART, CLUB, DIAMOND
}
```

Create `Rank.java`:

```java
package com.chesscard.shengji.domain;

public enum Rank {
    THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE, TWO, SMALL_JOKER, BIG_JOKER
}
```

Create `Card.java`:

```java
package com.chesscard.shengji.domain;

public record Card(Suit suit, Rank rank, int deckIndex) {
    public boolean isJoker() {
        return rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER;
    }
}
```

Create `PlayerSeat.java`:

```java
package com.chesscard.shengji.domain;

public enum PlayerSeat {
    SOUTH, WEST, NORTH, EAST;

    public PlayerSeat next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
```

Create `Team.java`:

```java
package com.chesscard.shengji.domain;

public enum Team {
    SOUTH_NORTH, EAST_WEST
}
```

Create `DeckFactory.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.List;

public final class DeckFactory {
    private DeckFactory() {
    }

    public static List<Card> createDoubleDeck() {
        List<Card> cards = new ArrayList<>();
        for (int deck = 0; deck < 2; deck++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    if (rank != Rank.SMALL_JOKER && rank != Rank.BIG_JOKER) {
                        cards.add(new Card(suit, rank, deck));
                    }
                }
            }
            cards.add(new Card(null, Rank.SMALL_JOKER, deck));
            cards.add(new Card(null, Rank.BIG_JOKER, deck));
        }
        return cards;
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run:

```powershell
cd server
mvn -q -Dtest=DeckFactoryTest test
```

Expected: PASS.

## Task 3: Declaration Rules

**Files:**
- Create: `server/src/test/java/com/chesscard/shengji/rules/DeclarationRulesTest.java`
- Create: `server/src/main/java/com/chesscard/shengji/rules/DeclarationRules.java`

- [x] **Step 1: Write failing declaration tests**

Create `DeclarationRulesTest.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeclarationRulesTest {
    @Test
    void cannotDeclareWithoutJoker() {
        List<Card> hand = List.of(new Card(Suit.HEART, Rank.ACE, 0));

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).isEmpty();
    }

    @Test
    void bigJokerAllowsOnlyRedLevelSuitsPresentInHand() {
        List<Card> hand = List.of(
                new Card(null, Rank.BIG_JOKER, 0),
                new Card(Suit.HEART, Rank.ACE, 0),
                new Card(Suit.SPADE, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.HEART);
    }

    @Test
    void smallJokerAllowsOnlyBlackLevelSuitsPresentInHand() {
        List<Card> hand = List.of(
                new Card(null, Rank.SMALL_JOKER, 0),
                new Card(Suit.SPADE, Rank.ACE, 0),
                new Card(Suit.DIAMOND, Rank.ACE, 0)
        );

        assertThat(DeclarationRules.availableSuits(hand, Rank.ACE)).containsExactly(Suit.SPADE);
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```powershell
cd server
mvn -q -Dtest=DeclarationRulesTest test
```

Expected: FAIL because `DeclarationRules` does not exist.

- [x] **Step 3: Implement declaration rules**

Create `DeclarationRules.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.List;

public final class DeclarationRules {
    private DeclarationRules() {
    }

    public static List<Suit> availableSuits(List<Card> hand, Rank levelRank) {
        boolean hasBigJoker = hand.stream().anyMatch(card -> card.rank() == Rank.BIG_JOKER);
        boolean hasSmallJoker = hand.stream().anyMatch(card -> card.rank() == Rank.SMALL_JOKER);
        List<Suit> result = new ArrayList<>();
        if (hasSmallJoker) {
            addIfHasLevelCard(result, hand, levelRank, Suit.SPADE);
            addIfHasLevelCard(result, hand, levelRank, Suit.CLUB);
        }
        if (hasBigJoker) {
            addIfHasLevelCard(result, hand, levelRank, Suit.HEART);
            addIfHasLevelCard(result, hand, levelRank, Suit.DIAMOND);
        }
        return result;
    }

    private static void addIfHasLevelCard(List<Suit> result, List<Card> hand, Rank levelRank, Suit suit) {
        boolean present = hand.stream().anyMatch(card -> card.suit() == suit && card.rank() == levelRank);
        if (present) {
            result.add(suit);
        }
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run:

```powershell
cd server
mvn -q -Dtest=DeclarationRulesTest test
```

Expected: PASS.

## Task 4: Shengji Card Sorting

**Files:**
- Create: `server/src/test/java/com/chesscard/shengji/rules/ShengjiSorterTest.java`
- Create: `server/src/main/java/com/chesscard/shengji/rules/ShengjiSorter.java`

- [x] **Step 1: Write failing sorter test**

Create `ShengjiSorterTest.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShengjiSorterTest {
    @Test
    void sortsByTrumpRulesForHeartAceGame() {
        Card big = new Card(null, Rank.BIG_JOKER, 0);
        Card small = new Card(null, Rank.SMALL_JOKER, 0);
        Card heartAce = new Card(Suit.HEART, Rank.ACE, 0);
        Card spadeAce = new Card(Suit.SPADE, Rank.ACE, 0);
        Card heartTwo = new Card(Suit.HEART, Rank.TWO, 0);
        Card clubTwo = new Card(Suit.CLUB, Rank.TWO, 0);
        Card heartKing = new Card(Suit.HEART, Rank.KING, 0);
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 0);

        List<Card> sorted = ShengjiSorter.sortHand(
                List.of(spadeKing, clubTwo, heartKing, small, heartTwo, spadeAce, big, heartAce),
                Rank.ACE,
                Suit.HEART
        );

        assertThat(sorted).containsExactly(big, small, heartAce, spadeAce, heartTwo, clubTwo, heartKing, spadeKing);
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```powershell
cd server
mvn -q -Dtest=ShengjiSorterTest test
```

Expected: FAIL because `ShengjiSorter` does not exist.

- [x] **Step 3: Implement sorter**

Create `ShengjiSorter.java`:

```java
package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShengjiSorter {
    private ShengjiSorter() {
    }

    public static List<Card> sortHand(List<Card> hand, Rank levelRank, Suit trumpSuit) {
        List<Card> copy = new ArrayList<>(hand);
        copy.sort(Comparator.comparingInt(card -> sortWeight(card, levelRank, trumpSuit)));
        return copy;
    }

    private static int sortWeight(Card card, Rank levelRank, Suit trumpSuit) {
        if (card.rank() == Rank.BIG_JOKER) return 0;
        if (card.rank() == Rank.SMALL_JOKER) return 1;
        if (card.suit() == trumpSuit && card.rank() == levelRank) return 10;
        if (card.rank() == levelRank) return 20 + suitOrder(card.suit());
        if (card.suit() == trumpSuit && card.rank() == Rank.TWO) return 30;
        if (card.rank() == Rank.TWO) return 40 + suitOrder(card.suit());
        if (card.suit() == trumpSuit) return 50 + rankDescending(card.rank());
        return 100 + suitOrder(card.suit()) * 20 + rankDescending(card.rank());
    }

    private static int suitOrder(Suit suit) {
        return switch (suit) {
            case SPADE -> 0;
            case HEART -> 1;
            case CLUB -> 2;
            case DIAMOND -> 3;
        };
    }

    private static int rankDescending(Rank rank) {
        return switch (rank) {
            case ACE -> 0;
            case KING -> 1;
            case QUEEN -> 2;
            case JACK -> 3;
            case TEN -> 4;
            case NINE -> 5;
            case EIGHT -> 6;
            case SEVEN -> 7;
            case SIX -> 8;
            case FIVE -> 9;
            case FOUR -> 10;
            case THREE -> 11;
            case TWO, SMALL_JOKER, BIG_JOKER -> 99;
        };
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run:

```powershell
cd server
mvn -q -Dtest=ShengjiSorterTest test
```

Expected: PASS.

## Task 5: Score Rules

**Files:**
- Create: `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`
- Create: `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`

- [x] **Step 1: Write failing score tests**

Create `ScoreRulesTest.java`:

```java
package com.chesscard.shengji.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreRulesTest {
    @Test
    void scoreCardsAreWorthExpectedPoints() {
        assertThat(ScoreRules.cardPoints("5")).isEqualTo(5);
        assertThat(ScoreRules.cardPoints("10")).isEqualTo(10);
        assertThat(ScoreRules.cardPoints("K")).isEqualTo(10);
        assertThat(ScoreRules.cardPoints("A")).isZero();
    }

    @Test
    void bankerUpgradeStepsFollowShengjiThresholds() {
        assertThat(ScoreRules.bankerDeltaForDefenderWin(0)).isEqualTo(3);
        assertThat(ScoreRules.bankerDeltaForDefenderWin(35)).isEqualTo(2);
        assertThat(ScoreRules.bankerDeltaForDefenderWin(75)).isEqualTo(1);
    }

    @Test
    void attackersUpgradeIsCappedAtThree() {
        assertThat(ScoreRules.attackerDeltaForAttackersWin(80)).isEqualTo(1);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(120)).isEqualTo(2);
        assertThat(ScoreRules.attackerDeltaForAttackersWin(200)).isEqualTo(3);
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run:

```powershell
cd server
mvn -q -Dtest=ScoreRulesTest test
```

Expected: FAIL because `ScoreRules` does not exist.

- [x] **Step 3: Implement score rules**

Create `ScoreRules.java`:

```java
package com.chesscard.shengji.rules;

public final class ScoreRules {
    private ScoreRules() {
    }

    public static int cardPoints(String rankLabel) {
        return switch (rankLabel) {
            case "5" -> 5;
            case "10", "K" -> 10;
            default -> 0;
        };
    }

    public static int bankerDeltaForDefenderWin(int attackerScore) {
        if (attackerScore == 0) return 3;
        if (attackerScore < 40) return 2;
        return 1;
    }

    public static int attackerDeltaForAttackersWin(int attackerScore) {
        if (attackerScore < 80) return 0;
        int delta = 1 + Math.max(0, (attackerScore - 80) / 40);
        return Math.min(3, delta);
    }

    public static int kittyMultiplier(String pattern, int tractorLength) {
        return switch (pattern) {
            case "SINGLE" -> 2;
            case "PAIR" -> 4;
            case "TRACTOR" -> 2 * tractorLength;
            default -> 1;
        };
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run:

```powershell
cd server
mvn -q -Dtest=ScoreRulesTest test
```

Expected: PASS.

## Task 6: Game State and Service

**Files:**
- Create: `server/src/main/java/com/chesscard/shengji/domain/GamePhase.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/GameState.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/InMemoryGameRepository.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/AiPlayer.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/GameService.java`

- [x] **Step 1: Add game phase and state**

Create `GamePhase.java`:

```java
package com.chesscard.shengji.domain;

public enum GamePhase {
    DECLARE, KITTY, PLAY, FINISHED
}
```

Create `GameState.java`:

```java
package com.chesscard.shengji.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameState {
    private final String id = UUID.randomUUID().toString();
    private GamePhase phase = GamePhase.DECLARE;
    private Rank levelRank = Rank.ACE;
    private Suit trumpSuit;
    private PlayerSeat banker;
    private PlayerSeat currentTurn = PlayerSeat.SOUTH;
    private int attackerScore;
    private final Map<PlayerSeat, List<Card>> hands = new EnumMap<>(PlayerSeat.class);
    private final List<Card> kitty = new ArrayList<>();
    private final Map<PlayerSeat, List<Card>> currentTrick = new EnumMap<>(PlayerSeat.class);

    public String getId() { return id; }
    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public Rank getLevelRank() { return levelRank; }
    public Suit getTrumpSuit() { return trumpSuit; }
    public void setTrumpSuit(Suit trumpSuit) { this.trumpSuit = trumpSuit; }
    public PlayerSeat getBanker() { return banker; }
    public void setBanker(PlayerSeat banker) { this.banker = banker; }
    public PlayerSeat getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(PlayerSeat currentTurn) { this.currentTurn = currentTurn; }
    public int getAttackerScore() { return attackerScore; }
    public void addAttackerScore(int points) { this.attackerScore += points; }
    public Map<PlayerSeat, List<Card>> getHands() { return hands; }
    public List<Card> getKitty() { return kitty; }
    public Map<PlayerSeat, List<Card>> getCurrentTrick() { return currentTrick; }
}
```

- [x] **Step 2: Add repository, AI, and service**

Create `InMemoryGameRepository.java`:

```java
package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.GameState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGameRepository {
    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    public GameState save(GameState game) {
        games.put(game.getId(), game);
        return game;
    }

    public Optional<GameState> find(String id) {
        return Optional.ofNullable(games.get(id));
    }
}
```

Create `AiPlayer.java`:

```java
package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiPlayer {
    public List<Card> choosePlay(GameState game, PlayerSeat seat) {
        return List.of(game.getHands().get(seat).get(0));
    }
}
```

Create `GameService.java`:

```java
package com.chesscard.shengji.service;

import com.chesscard.shengji.domain.*;
import com.chesscard.shengji.rules.DeckFactory;
import com.chesscard.shengji.rules.ShengjiSorter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GameService {
    private final InMemoryGameRepository repository;
    private final AiPlayer aiPlayer;

    public GameService(InMemoryGameRepository repository, AiPlayer aiPlayer) {
        this.repository = repository;
        this.aiPlayer = aiPlayer;
    }

    public GameState createGame() {
        GameState game = new GameState();
        List<Card> deck = DeckFactory.createDoubleDeck();
        Collections.shuffle(deck);
        int cursor = 0;
        for (PlayerSeat seat : PlayerSeat.values()) {
            game.getHands().put(seat, deck.subList(cursor, cursor + 25).stream().toList());
            cursor += 25;
        }
        game.getKitty().addAll(deck.subList(cursor, cursor + 8));
        sortSouthHand(game);
        return repository.save(game);
    }

    public GameState getGame(String id) {
        return repository.find(id).orElseThrow(() -> new IllegalArgumentException("牌局不存在"));
    }

    public GameState declare(String id, Suit suit) {
        GameState game = getGame(id);
        game.setTrumpSuit(suit);
        game.setBanker(PlayerSeat.SOUTH);
        game.setCurrentTurn(PlayerSeat.SOUTH);
        game.setPhase(GamePhase.KITTY);
        game.getHands().get(PlayerSeat.SOUTH).addAll(game.getKitty());
        game.getKitty().clear();
        sortSouthHand(game);
        return game;
    }

    public GameState setKitty(String id, List<Card> cards) {
        GameState game = getGame(id);
        if (cards.size() != 8) {
            throw new IllegalArgumentException("必须扣 8 张底牌");
        }
        game.getHands().get(PlayerSeat.SOUTH).removeAll(cards);
        game.getKitty().addAll(cards);
        game.setPhase(GamePhase.PLAY);
        sortSouthHand(game);
        return game;
    }

    public GameState play(String id, PlayerSeat seat, List<Card> cards) {
        GameState game = getGame(id);
        game.getHands().get(seat).removeAll(cards);
        game.getCurrentTrick().put(seat, cards);
        game.setCurrentTurn(seat.next());
        sortSouthHand(game);
        return game;
    }

    public GameState aiStep(String id) {
        GameState game = getGame(id);
        PlayerSeat seat = game.getCurrentTurn();
        if (seat == PlayerSeat.SOUTH) {
            return game;
        }
        return play(id, seat, aiPlayer.choosePlay(game, seat));
    }

    private void sortSouthHand(GameState game) {
        Suit trump = game.getTrumpSuit() == null ? Suit.HEART : game.getTrumpSuit();
        game.getHands().computeIfPresent(PlayerSeat.SOUTH,
                (seat, cards) -> ShengjiSorter.sortHand(cards, game.getLevelRank(), trump));
    }
}
```

- [x] **Step 3: Run full server tests**

Run:

```powershell
cd server
mvn test
```

Expected: all rule tests pass.

## Task 7: REST API

**Files:**
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/CardDto.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/GameStateDto.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/DeclareRequest.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/KittyRequest.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/PlayRequest.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/GameController.java`

- [x] **Step 1: Add DTOs**

Create `CardDto.java`:

```java
package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;

public record CardDto(Suit suit, Rank rank, int deckIndex) {
    public static CardDto from(Card card) {
        return new CardDto(card.suit(), card.rank(), card.deckIndex());
    }

    public Card toCard() {
        return new Card(suit, rank, deckIndex);
    }
}
```

Create `GameStateDto.java`:

```java
package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.*;
import com.chesscard.shengji.rules.DeclarationRules;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GameStateDto(
        String id,
        GamePhase phase,
        Rank levelRank,
        Suit trumpSuit,
        PlayerSeat banker,
        PlayerSeat currentTurn,
        int attackerScore,
        Map<PlayerSeat, Integer> handCounts,
        List<CardDto> southHand,
        List<CardDto> kitty,
        Map<PlayerSeat, List<CardDto>> currentTrick,
        List<Suit> declarationOptions
) {
    public static GameStateDto from(GameState game) {
        List<Card> southHand = game.getHands().getOrDefault(PlayerSeat.SOUTH, List.of());
        return new GameStateDto(
                game.getId(),
                game.getPhase(),
                game.getLevelRank(),
                game.getTrumpSuit(),
                game.getBanker(),
                game.getCurrentTurn(),
                game.getAttackerScore(),
                game.getHands().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())),
                southHand.stream().map(CardDto::from).toList(),
                game.getKitty().stream().map(CardDto::from).toList(),
                game.getCurrentTrick().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map(CardDto::from).toList())),
                DeclarationRules.availableSuits(southHand, game.getLevelRank())
        );
    }
}
```

Create `DeclareRequest.java`:

```java
package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Suit;

public record DeclareRequest(Suit suit) {
}
```

Create `KittyRequest.java`:

```java
package com.chesscard.shengji.api.dto;

import java.util.List;

public record KittyRequest(List<CardDto> cards) {
}
```

Create `PlayRequest.java`:

```java
package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.PlayerSeat;

import java.util.List;

public record PlayRequest(PlayerSeat seat, List<CardDto> cards) {
}
```

- [x] **Step 2: Add controller**

Create `GameController.java`:

```java
package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.*;
import com.chesscard.shengji.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {
    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    @PostMapping
    public GameStateDto create() {
        return GameStateDto.from(service.createGame());
    }

    @GetMapping("/{id}")
    public GameStateDto get(@PathVariable String id) {
        return GameStateDto.from(service.getGame(id));
    }

    @PostMapping("/{id}/declare")
    public GameStateDto declare(@PathVariable String id, @RequestBody DeclareRequest request) {
        return GameStateDto.from(service.declare(id, request.suit()));
    }

    @PostMapping("/{id}/kitty")
    public GameStateDto kitty(@PathVariable String id, @RequestBody KittyRequest request) {
        return GameStateDto.from(service.setKitty(id, request.cards().stream().map(CardDto::toCard).toList()));
    }

    @PostMapping("/{id}/play")
    public GameStateDto play(@PathVariable String id, @RequestBody PlayRequest request) {
        return GameStateDto.from(service.play(id, request.seat(), request.cards().stream().map(CardDto::toCard).toList()));
    }

    @PostMapping("/{id}/ai/step")
    public GameStateDto aiStep(@PathVariable String id) {
        return GameStateDto.from(service.aiStep(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

- [x] **Step 3: Verify API compiles**

Run:

```powershell
cd server
mvn test
```

Expected: all tests pass and API compiles.

## Task 8: Flutter App Scaffold

**Files:**
- Create: `app/pubspec.yaml`
- Create: `app/lib/main.dart`
- Create: `app/lib/models.dart`
- Create: `app/lib/api_client.dart`
- Create: `app/lib/game_page.dart`

- [x] **Step 1: Create Flutter package**

Create `app/pubspec.yaml`:

```yaml
name: chess_card_app
description: Shengji MVP client.
publish_to: "none"
version: 0.1.0

environment:
  sdk: ">=3.4.0 <4.0.0"

dependencies:
  flutter:
    sdk: flutter
  http: ^1.2.2

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^4.0.0

flutter:
  uses-material-design: true
```

- [x] **Step 2: Add Dart models**

Create `models.dart`:

```dart
class CardModel {
  const CardModel({required this.suit, required this.rank, required this.deckIndex});

  final String? suit;
  final String rank;
  final int deckIndex;

  factory CardModel.fromJson(Map<String, dynamic> json) {
    return CardModel(suit: json['suit'] as String?, rank: json['rank'] as String, deckIndex: json['deckIndex'] as int);
  }

  Map<String, dynamic> toJson() => {'suit': suit, 'rank': rank, 'deckIndex': deckIndex};
}

class GameStateModel {
  const GameStateModel({
    required this.id,
    required this.phase,
    required this.levelRank,
    required this.trumpSuit,
    required this.banker,
    required this.currentTurn,
    required this.attackerScore,
    required this.southHand,
    required this.declarationOptions,
  });

  final String id;
  final String phase;
  final String levelRank;
  final String? trumpSuit;
  final String? banker;
  final String currentTurn;
  final int attackerScore;
  final List<CardModel> southHand;
  final List<String> declarationOptions;

  factory GameStateModel.fromJson(Map<String, dynamic> json) {
    return GameStateModel(
      id: json['id'] as String,
      phase: json['phase'] as String,
      levelRank: json['levelRank'] as String,
      trumpSuit: json['trumpSuit'] as String?,
      banker: json['banker'] as String?,
      currentTurn: json['currentTurn'] as String,
      attackerScore: json['attackerScore'] as int,
      southHand: (json['southHand'] as List).map((item) => CardModel.fromJson(item as Map<String, dynamic>)).toList(),
      declarationOptions: (json['declarationOptions'] as List).cast<String>(),
    );
  }
}
```

- [x] **Step 3: Add API client**

Create `api_client.dart`:

```dart
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'models.dart';

class ApiClient {
  ApiClient({this.baseUrl = 'http://localhost:8080'});

  final String baseUrl;

  Future<GameStateModel> createGame() async {
    final response = await http.post(Uri.parse('$baseUrl/api/games'));
    return _decode(response);
  }

  Future<GameStateModel> declare(String gameId, String suit) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/games/$gameId/declare'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'suit': suit}),
    );
    return _decode(response);
  }

  Future<GameStateModel> kitty(String gameId, List<CardModel> cards) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/games/$gameId/kitty'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'cards': cards.map((card) => card.toJson()).toList()}),
    );
    return _decode(response);
  }

  Future<GameStateModel> play(String gameId, List<CardModel> cards) async {
    final response = await http.post(
      Uri.parse('$baseUrl/api/games/$gameId/play'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'seat': 'SOUTH', 'cards': cards.map((card) => card.toJson()).toList()}),
    );
    return _decode(response);
  }

  Future<GameStateModel> aiStep(String gameId) async {
    final response = await http.post(Uri.parse('$baseUrl/api/games/$gameId/ai/step'));
    return _decode(response);
  }

  GameStateModel _decode(http.Response response) {
    if (response.statusCode >= 400) {
      throw Exception(response.body);
    }
    return GameStateModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>);
  }
}
```

- [x] **Step 4: Add main and page**

Create `main.dart`:

```dart
import 'package:flutter/material.dart';

import 'game_page.dart';

void main() {
  runApp(const ChessCardApp());
}

class ChessCardApp extends StatelessWidget {
  const ChessCardApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.green),
      home: const GamePage(),
    );
  }
}
```

Create `game_page.dart`:

```dart
import 'package:flutter/material.dart';

import 'api_client.dart';
import 'models.dart';

class GamePage extends StatefulWidget {
  const GamePage({super.key});

  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  final ApiClient api = ApiClient();
  GameStateModel? game;
  final Set<CardModel> selected = {};
  String? error;

  Future<void> _run(Future<GameStateModel> Function() action) async {
    try {
      final next = await action();
      setState(() {
        game = next;
        selected.clear();
        error = null;
      });
    } catch (e) {
      setState(() => error = e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    final current = game;
    return Scaffold(
      appBar: AppBar(title: const Text('升级')),
      body: current == null ? _buildStart() : _buildTable(current),
    );
  }

  Widget _buildStart() {
    return Center(
      child: FilledButton(
        onPressed: () => _run(api.createGame),
        child: const Text('创建游戏'),
      ),
    );
  }

  Widget _buildTable(GameStateModel game) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: Wrap(
            spacing: 12,
            runSpacing: 8,
            children: [
              Text('阶段: ${game.phase}'),
              Text('级牌: ${_rank(game.levelRank)}'),
              Text('主花色: ${game.trumpSuit == null ? '未定' : _suit(game.trumpSuit!)}'),
              Text('庄家: ${game.banker ?? '未定'}'),
              Text('闲家分: ${game.attackerScore}'),
              Text('当前: ${game.currentTurn}'),
            ],
          ),
        ),
        if (error != null) Text(error!, style: const TextStyle(color: Colors.red)),
        Expanded(
          child: Center(
            child: Text(
              game.phase == 'DECLARE' ? '等待叫主' : '出牌区',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ),
        ),
        if (game.phase == 'DECLARE') _buildDeclare(game),
        if (game.phase == 'KITTY') _buildKitty(game),
        if (game.phase == 'PLAY') _buildPlay(game),
        _buildHand(game.southHand),
      ],
    );
  }

  Widget _buildDeclare(GameStateModel game) {
    if (game.declarationOptions.isEmpty) {
      return const Padding(padding: EdgeInsets.all(12), child: Text('当前手牌不能叫主'));
    }
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Wrap(
        spacing: 8,
        children: game.declarationOptions
            .map((suit) => FilledButton(onPressed: () => _run(() => api.declare(game.id, suit)), child: Text('叫 ${_suit(suit)}')))
            .toList(),
      ),
    );
  }

  Widget _buildKitty(GameStateModel game) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: FilledButton(
        onPressed: selected.length == 8 ? () => _run(() => api.kitty(game.id, selected.toList())) : null,
        child: Text('扣底 ${selected.length}/8'),
      ),
    );
  }

  Widget _buildPlay(GameStateModel game) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Wrap(
        spacing: 8,
        children: [
          FilledButton(onPressed: selected.isEmpty ? null : () => _run(() => api.play(game.id, selected.toList())), child: const Text('出牌')),
          OutlinedButton(onPressed: () => _run(() => api.aiStep(game.id)), child: const Text('推进 AI')),
        ],
      ),
    );
  }

  Widget _buildHand(List<CardModel> cards) {
    return SizedBox(
      height: 132,
      child: ListView.separated(
        padding: const EdgeInsets.all(12),
        scrollDirection: Axis.horizontal,
        itemCount: cards.length,
        separatorBuilder: (_, __) => const SizedBox(width: 6),
        itemBuilder: (context, index) {
          final card = cards[index];
          final isSelected = selected.contains(card);
          return GestureDetector(
            onTap: () {
              setState(() {
                isSelected ? selected.remove(card) : selected.add(card);
              });
            },
            child: Transform.translate(
              offset: Offset(0, isSelected ? -12 : 0),
              child: Container(
                width: 58,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: isSelected ? Colors.green : Colors.black26, width: 2),
                ),
                child: Center(
                  child: Text(
                    '${_suit(card.suit)}\n${_rank(card.rank)}',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _isRed(card.suit) ? Colors.red : Colors.black, fontWeight: FontWeight.w700),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  bool _isRed(String? suit) => suit == 'HEART' || suit == 'DIAMOND';
  String _suit(String? suit) => switch (suit) {
        'SPADE' => '黑桃',
        'HEART' => '红心',
        'CLUB' => '梅花',
        'DIAMOND' => '方块',
        _ => '',
      };
  String _rank(String rank) => switch (rank) {
        'THREE' => '3',
        'FOUR' => '4',
        'FIVE' => '5',
        'SIX' => '6',
        'SEVEN' => '7',
        'EIGHT' => '8',
        'NINE' => '9',
        'TEN' => '10',
        'JACK' => 'J',
        'QUEEN' => 'Q',
        'KING' => 'K',
        'ACE' => 'A',
        'TWO' => '2',
        'SMALL_JOKER' => '小王',
        'BIG_JOKER' => '大王',
        _ => rank,
      };
}
```

- [x] **Step 5: Verify Flutter analyzes**

Run:

```powershell
cd app
flutter pub get
flutter analyze
```

Expected: dependencies resolve and analyzer reports no errors.

## Task 9: End-to-End Verification

**Files:**
- Modify as needed based on compile/analyze output.

- [x] **Step 1: Run backend tests**

Run:

```powershell
cd server
mvn test
```

Expected: all tests pass.

- [x] **Step 2: Start backend**

Run:

```powershell
cd server
mvn spring-boot:run
```

Expected: server listens on `http://localhost:8080`.

- [x] **Step 3: Smoke test API**

Run in a second terminal:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/games
```

Expected: JSON contains `id`, `phase` as `DECLARE`, `southHand` with 25 cards, and `declarationOptions`.

- [x] **Step 4: Run Flutter app**

Run:

```powershell
cd app
flutter run -d windows
```

Expected: App launches, “创建游戏” creates a game, and the hand appears at the bottom.

Actual verification: Windows desktop project was generated, but `flutter run -d windows` is blocked on this machine because Visual Studio with the "Desktop development with C++" workload is not installed. Flutter Web was generated and verified instead with `flutter run -d edge`; Edge launched, “创建游戏” created a game, and the game table rendered.

## Self-Review

- Spec coverage: project structure, Java backend, Flutter app, declaration limits, trump sorting, score thresholds, API, local MVP, and verification are covered by Tasks 1-9.
- Placeholder scan: no `TBD`, `TODO`, or “implement later” text remains.
- Type consistency: Java enum names and Dart JSON field names match the DTOs. `CardDto` maps exactly to `Card`, and Flutter sends the same card shape back to the backend.
