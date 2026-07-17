# Live Trump Two Level Progression Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `2` an always-trump card only: skip it when progressing level ranks and reject legacy games that store it as either level-rank field.

**Architecture:** Keep the ordered playable ranks inside `ScoreRules.advanceLevelRank`; remove `Rank.TWO` and reject ranks absent from that sequence. Before `GameService.createNextGame` calculates its expected rank, explicitly reject persisted `Rank.TWO` values. No API, DTO, Flutter, card ordering, or trick-rule change is needed.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Maven.

---

### Task 1: Make the progression skip the live trump two

**Files:**

- Modify: `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`

- [ ] **Step 1: Write the failing rule tests**

Replace the A-level assertions and add the invalid-rank test:

```java
@Test
void advancesLevelRankPastLiveTrumpTwoAndCapsAtKing() {
    assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 1)).isEqualTo(Rank.THREE);
    assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 3)).isEqualTo(Rank.FIVE);
    assertThat(ScoreRules.advanceLevelRank(Rank.QUEEN, 3)).isEqualTo(Rank.KING);
}

@Test
void rejectsLiveTrumpTwoAsCurrentLevelRank() {
    assertThatThrownBy(() -> ScoreRules.advanceLevelRank(Rank.TWO, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2");
}
```

- [ ] **Step 2: Verify the tests fail**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `ACE + 1` returns `TWO` and `Rank.TWO` is not rejected.

- [ ] **Step 3: Implement the minimal sequence change**

Replace the `levels` lookup and index initialization in `advanceLevelRank` with:

```java
Rank[] levels = {
        Rank.ACE,
        Rank.THREE,
        Rank.FOUR,
        Rank.FIVE,
        Rank.SIX,
        Rank.SEVEN,
        Rank.EIGHT,
        Rank.NINE,
        Rank.TEN,
        Rank.JACK,
        Rank.QUEEN,
        Rank.KING
};
int index = -1;
for (int i = 0; i < levels.length; i++) {
    if (levels[i] == current) {
        index = i;
        break;
    }
}
if (index < 0) {
    throw new IllegalArgumentException("2 是活主，不能作为级牌");
}
return levels[Math.min(levels.length - 1, index + Math.max(0, delta))];
```

- [ ] **Step 4: Verify and commit the rule layer**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `BUILD SUCCESS`.

Run: `git add server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`

Run: `git commit -m "fix: skip live trump two in level progression"`

### Task 2: Reject legacy two-level games before new-game creation

**Files:**

- Modify: `server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`

- [ ] **Step 1: Update valid fixtures and write the failing tests**

Change every valid A-level, delta-one fixture and expectation from `Rank.TWO` to `Rank.THREE`. In `rejectsNextGameWhenPreviousNextLevelRankDoesNotMatchLevelDelta`, make `Rank.TWO` the deliberately mismatched value. Add these tests:

```java
@Test
void rejectsNextGameWhenPreviousLevelRankIsLiveTrumpTwo() {
    FakeGameRepository repository = new FakeGameRepository();
    GameService service = new GameService(repository, new AiPlayer());
    GameState previous = validFinishedGame();
    previous.setLevelRank(Rank.TWO);
    previous.setNextLevelRank(Rank.THREE);
    repository.save(previous);

    assertThatThrownBy(() -> service.createNextGame(previous.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("活主");
}

@Test
void rejectsNextGameWhenPreviousNextLevelRankIsLiveTrumpTwo() {
    FakeGameRepository repository = new FakeGameRepository();
    GameService service = new GameService(repository, new AiPlayer());
    GameState previous = validFinishedGame();
    previous.setNextLevelRank(Rank.TWO);
    repository.save(previous);

    assertThatThrownBy(() -> service.createNextGame(previous.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("活主");
}
```

- [ ] **Step 2: Verify the service tests fail**

Run: `mvn -Dtest=GameServiceNextGameTest test`

Expected: the new tests do not receive the explicit `活主` business error.

- [ ] **Step 3: Add the pre-creation legacy-data guard**

After the existing null check for `previous.getNextLevelRank()` and before the `expectedNextLevelRank` calculation, add:

```java
if (previous.getLevelRank() == Rank.TWO || previous.getNextLevelRank() == Rank.TWO) {
    throw new IllegalArgumentException("2 是活主，不能作为级牌创建下一局");
}
```

- [ ] **Step 4: Verify and commit the service layer**

Run: `mvn -Dtest=GameServiceNextGameTest test`

Expected: `BUILD SUCCESS`.

Run: `git add server/src/main/java/com/chesscard/shengji/service/GameService.java server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`

Run: `git commit -m "fix: reject live trump two as a level rank"`

### Task 3: Document the rule and run the backend suite

**Files:**

- Modify: `README.md`

- [ ] **Step 1: Add the public rule text**

Add this bullet under the backend game-rules capability list:

```markdown
- `2` 为始终生效的活主，不属于可打级牌；级牌按 `A -> 3 -> ... -> K` 推进。
```

- [ ] **Step 2: Verify the full backend suite and commit**

Run: `mvn test`

Expected: `BUILD SUCCESS`, with no failures or errors.

Run: `git add README.md`

Run: `git commit -m "docs: clarify live trump two rule"`

## Plan Self-Review

- Spec coverage: Task 1 covers A-to-3, multi-level progression, K capping, and illegal rank two. Task 2 rejects both persisted legacy fields before creating a game. Task 3 documents the rule and runs the backend suite.
- Placeholder scan: Each task provides concrete test code, implementation code, file paths, commands, and expected results.
- Type consistency: The plan only uses existing `Rank`, `ScoreRules.advanceLevelRank`, `GameService.createNextGame`, JUnit 5, AssertJ, and Maven APIs.
