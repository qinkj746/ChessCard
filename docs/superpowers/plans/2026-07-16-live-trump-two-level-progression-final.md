# Live Trump Two Level Progression Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Skip the always-trump two when advancing level ranks, and reject legacy games that store two as a level rank.

**Architecture:** `ScoreRules.advanceLevelRank` owns the progression order and will exclude `Rank.TWO`. `GameService.createNextGame` validates both persisted rank fields before a new game is created. The API and Flutter client remain unchanged.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Maven.

---

## File Structure

- `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`: playable rank order.
- `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`: rule regression coverage.
- `server/src/main/java/com/chesscard/shengji/service/GameService.java`: legacy game validation.
- `server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`: next-game validation coverage.
- `README.md`: public rule statement.

### Task 1: Correct the playable level sequence

**Files:**

- Modify: `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`

- [ ] **Step 1: Write failing tests**

Replace the existing A-advance assertions and add this test:

```java
assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 1)).isEqualTo(Rank.THREE);
assertThat(ScoreRules.advanceLevelRank(Rank.ACE, 3)).isEqualTo(Rank.FIVE);
assertThatThrownBy(() -> ScoreRules.advanceLevelRank(Rank.TWO, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2");
```

- [ ] **Step 2: Verify the test fails**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `ACE + 1` is reported as `TWO`, and `Rank.TWO` is not rejected.

- [ ] **Step 3: Implement the minimal rule change**

Use this progression array and reject ranks not found in it:

```java
Rank[] levels = {
        Rank.ACE, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN,
        Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING
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

- [ ] **Step 4: Verify rules and commit**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `BUILD SUCCESS`.

Run: `git add server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`

Run: `git commit -m "fix: skip live trump two in level progression"`

### Task 2: Reject legacy two-level games before creation

**Files:**

- Modify: `server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`

- [ ] **Step 1: Update fixtures and write failing tests**

Change every valid A-level, delta-one fixture from `Rank.TWO` to `Rank.THREE`; use `Rank.TWO` in the existing next-rank mismatch test. Add one test with `previous.setLevelRank(Rank.TWO)` and one with `previous.setNextLevelRank(Rank.TWO)`. Both must assert `createNextGame` throws `IllegalArgumentException` containing `活主`.

- [ ] **Step 2: Verify the tests fail**

Run: `mvn -Dtest=GameServiceNextGameTest test`

Expected: the two new tests fail because no explicit live-trump validation exists.

- [ ] **Step 3: Add the legacy-data guard**

After the non-null `nextLevelRank` check and before calculating `expectedNextLevelRank`, add:

```java
if (previous.getLevelRank() == Rank.TWO || previous.getNextLevelRank() == Rank.TWO) {
    throw new IllegalArgumentException("2 是活主，不能作为级牌创建下一局");
}
```

- [ ] **Step 4: Verify service tests and commit**

Run: `mvn -Dtest=GameServiceNextGameTest test`

Expected: `BUILD SUCCESS`.

Run: `git add server/src/main/java/com/chesscard/shengji/service/GameService.java server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`

Run: `git commit -m "fix: reject live trump two as a level rank"`

### Task 3: Document and run the backend regression suite

**Files:**

- Modify: `README.md`

- [ ] **Step 1: Add the rule statement**

Add this backend-rules bullet:

```markdown
- `2` 为始终生效的活主，不属于可打级牌；级牌按 `A -> 3 -> ... -> K` 推进。
```

- [ ] **Step 2: Verify all backend tests and commit**

Run: `mvn test`

Expected: `BUILD SUCCESS`, with no failures or errors.

Run: `git add README.md`

Run: `git commit -m "docs: clarify live trump two rule"`

## Plan Self-Review

- The tasks cover rank progression, both legacy persisted rank fields, documentation, and full backend verification.
- Tests are written before implementation and each step has an exact command and expected result.
- The plan uses existing `Rank`, `ScoreRules`, and `GameService` APIs only.
