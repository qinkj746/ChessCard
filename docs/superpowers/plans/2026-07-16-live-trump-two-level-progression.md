# 活主 2 与级牌推进 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 2 始终仅作为活主牌，级牌从 A 升级时跳至 3，并拒绝历史 2 级牌局创建下一局。

**Architecture:** `ScoreRules` 是可打级牌序列的唯一来源，移除 `Rank.TWO`。`GameService.createNextGame` 在创建新局前拒绝当前或下一局级牌为 2 的旧牌局。客户端 API 和数据模型不变。

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Maven.

---

## File Structure

- `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`: 不含 2 的级牌推进序列。
- `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`: A 后跳过 2 与非法级牌测试。
- `server/src/main/java/com/chesscard/shengji/service/GameService.java`: 拒绝旧的 2 级牌局。
- `server/src/test/java/com/chesscard/shengji/service/GameServiceNextGameTest.java`: 下一局校验回归测试。
- `README.md`: 对外说明活主规则。

### Task 1: 修正级牌推进规则

**Files:**

- Modify: `server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`

- [ ] **Step 1: 写入失败的规则测试**

将已有升级测试替换为：

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

- [ ] **Step 2: 验证测试失败**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `A + 1` 实际为 `TWO`，并且 `Rank.TWO` 未被拒绝。

- [ ] **Step 3: 实现最小规则修正**

将 `ScoreRules.advanceLevelRank` 的级牌数组替换为以下内容；其余推进和 K 封顶逻辑保持不变：

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

- [ ] **Step 4: 验证规则测试通过**

Run: `mvn -Dtest=ScoreRulesTest test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交规则层改动**

Run: `git add server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java server/src/test/java/com/chesscard/shengji/rules/ScoreRulesTest.java`

Run: `git commit -m "fix: skip live trump two in level progression"`
