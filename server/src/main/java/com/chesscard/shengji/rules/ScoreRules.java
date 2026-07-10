package com.chesscard.shengji.rules;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.Rank;

public final class ScoreRules {
    private ScoreRules() {
    }

    public static int cardPoints(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("牌不能为空");
        }
        return switch (card.rank()) {
            case FIVE -> 5;
            case TEN, KING -> 10;
            default -> 0;
        };
    }

    public static int bankerDeltaForDefenderWin(int attackerScore) {
        if (attackerScore < 0) {
            throw new IllegalArgumentException("闲家分数不能为负数");
        }
        if (attackerScore == 0) {
            return 3;
        }
        if (attackerScore < 40) {
            return 2;
        }
        return 1;
    }

    public static int attackerDeltaForAttackersWin(int attackerScore) {
        if (attackerScore < 0) {
            throw new IllegalArgumentException("闲家分数不能为负数");
        }
        if (attackerScore < 80) {
            return 0;
        }
        int delta = 1 + Math.max(0, (attackerScore - 80) / 40);
        return Math.min(3, delta);
    }

    public static int kittyMultiplier(String pattern, int tractorLength) {
        if (pattern == null) {
            throw new IllegalArgumentException("牌型不能为空");
        }
        if ("TRACTOR".equals(pattern) && tractorLength <= 0) {
            throw new IllegalArgumentException("拖拉机长度必须为正数");
        }
        return switch (pattern) {
            case "SINGLE" -> 2;
            case "PAIR" -> 4;
            case "TRACTOR" -> 2 * tractorLength;
            default -> 1;
        };
    }

    public static Rank advanceLevelRank(Rank current, int delta) {
        if (current == null) {
            throw new IllegalArgumentException("当前级牌不能为空");
        }
        Rank[] levels = {
                Rank.ACE,
                Rank.TWO,
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
        int index = 0;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] == current) {
                index = i;
                break;
            }
        }
        return levels[Math.min(levels.length - 1, index + Math.max(0, delta))];
    }
}
