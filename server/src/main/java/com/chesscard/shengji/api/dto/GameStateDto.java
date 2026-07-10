package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Card;
import com.chesscard.shengji.domain.GamePhase;
import com.chesscard.shengji.domain.GameState;
import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.Rank;
import com.chesscard.shengji.domain.Suit;
import com.chesscard.shengji.domain.Team;
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
        Team winningTeam,
        int levelDelta,
        Rank nextLevelRank,
        boolean completed,
        Map<PlayerSeat, Integer> handCounts,
        List<CardDto> southHand,
        List<CardDto> kitty,
        Map<PlayerSeat, List<CardDto>> currentTrick,
        List<TrickPlayDto> currentTrickPlays,
        List<PlayedTrickDto> playedTricks,
        List<Suit> declarationOptions,
        String lastActionMessage
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
                game.getWinningTeam(),
                game.getLevelDelta(),
                game.getNextLevelRank(),
                game.isCompleted(),
                game.getHands().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size())),
                southHand.stream().map(CardDto::from).toList(),
                game.getKitty().stream().map(CardDto::from).toList(),
                game.getCurrentTrick().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue().stream().map(CardDto::from).toList())),
                orderedCurrentTrick(game),
                game.getPlayedTricks().stream().map(PlayedTrickDto::from).toList(),
                game.getPhase() == GamePhase.DECLARE
                        ? DeclarationRules.availableSuits(southHand, game.getLevelRank())
                        : List.of(),
                game.getLastActionMessage()
        );
    }

    private static List<TrickPlayDto> orderedCurrentTrick(GameState game) {
        if (game.getCurrentTrick().isEmpty()) {
            return List.of();
        }
        PlayerSeat leader = game.getCurrentTrickLeader() == null ? game.getCurrentTurn() : game.getCurrentTrickLeader();
        return orderedPlays(leader, game.getCurrentTrick());
    }

    private static List<TrickPlayDto> orderedPlays(PlayerSeat leader, Map<PlayerSeat, List<Card>> trick) {
        return java.util.stream.Stream.iterate(leader, PlayerSeat::next)
                .limit(PlayerSeat.values().length)
                .filter(trick::containsKey)
                .map(seat -> TrickPlayDto.from(seat, trick.get(seat)))
                .toList();
    }

    public record TrickPlayDto(PlayerSeat seat, List<CardDto> cards) {
        public static TrickPlayDto from(GameState.TrickPlay play) {
            return from(play.getSeat(), play.getCards());
        }

        public static TrickPlayDto from(PlayerSeat seat, List<Card> cards) {
            return new TrickPlayDto(seat, cards.stream().map(CardDto::from).toList());
        }
    }

    public record PlayedTrickDto(
            int index,
            PlayerSeat leader,
            PlayerSeat winner,
            int points,
            List<TrickPlayDto> plays
    ) {
        public static PlayedTrickDto from(GameState.PlayedTrick trick) {
            return new PlayedTrickDto(
                    trick.getIndex(),
                    trick.getLeader(),
                    trick.getWinner(),
                    trick.getPoints(),
                    trick.getPlays().stream().map(TrickPlayDto::from).toList()
            );
        }
    }
}