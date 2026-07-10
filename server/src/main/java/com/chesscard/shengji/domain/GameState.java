package com.chesscard.shengji.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameState {
    private String id = UUID.randomUUID().toString();
    private String roomId;
    private GamePhase phase = GamePhase.DECLARE;
    private Rank levelRank = Rank.ACE;
    private Suit trumpSuit;
    private PlayerSeat banker;
    private PlayerSeat currentTurn = PlayerSeat.SOUTH;
    private PlayerSeat currentTrickLeader;
    private int attackerScore;
    private Team winningTeam;
    private int levelDelta;
    private Rank nextLevelRank;
    private boolean completed;
    private final Map<PlayerSeat, List<Card>> hands = new EnumMap<>(PlayerSeat.class);
    private final List<Card> kitty = new ArrayList<>();
    private final Map<PlayerSeat, List<Card>> currentTrick = new EnumMap<>(PlayerSeat.class);
    private final List<PlayedTrick> playedTricks = new ArrayList<>();
    private final Map<PlayerSeat, String> seatOwners = new EnumMap<>(PlayerSeat.class);
    private String lastActionMessage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Rank getLevelRank() {
        return levelRank;
    }

    public void setLevelRank(Rank levelRank) {
        this.levelRank = levelRank;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public void setTrumpSuit(Suit trumpSuit) {
        this.trumpSuit = trumpSuit;
    }

    public PlayerSeat getBanker() {
        return banker;
    }

    public void setBanker(PlayerSeat banker) {
        this.banker = banker;
    }

    public PlayerSeat getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerSeat currentTurn) {
        this.currentTurn = currentTurn;
    }

    public PlayerSeat getCurrentTrickLeader() {
        return currentTrickLeader;
    }

    public void setCurrentTrickLeader(PlayerSeat currentTrickLeader) {
        this.currentTrickLeader = currentTrickLeader;
    }

    public int getAttackerScore() {
        return attackerScore;
    }

    public void setAttackerScore(int attackerScore) {
        this.attackerScore = attackerScore;
    }

    public void addAttackerScore(int points) {
        attackerScore += points;
    }

    public Team getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(Team winningTeam) {
        this.winningTeam = winningTeam;
    }

    public int getLevelDelta() {
        return levelDelta;
    }

    public void setLevelDelta(int levelDelta) {
        this.levelDelta = levelDelta;
    }

    public Rank getNextLevelRank() {
        return nextLevelRank;
    }

    public void setNextLevelRank(Rank nextLevelRank) {
        this.nextLevelRank = nextLevelRank;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Map<PlayerSeat, List<Card>> getHands() {
        return hands;
    }

    public List<Card> getKitty() {
        return kitty;
    }

    public Map<PlayerSeat, List<Card>> getCurrentTrick() {
        return currentTrick;
    }

    public List<PlayedTrick> getPlayedTricks() {
        return playedTricks;
    }

    public Map<PlayerSeat, String> getSeatOwners() {
        return seatOwners;
    }

    public String getLastActionMessage() {
        return lastActionMessage;
    }

    public void setLastActionMessage(String lastActionMessage) {
        this.lastActionMessage = lastActionMessage;
    }

    public static class TrickPlay {
        private PlayerSeat seat;
        private List<Card> cards = new ArrayList<>();

        public TrickPlay() {
        }

        public TrickPlay(PlayerSeat seat, List<Card> cards) {
            this.seat = seat;
            this.cards = new ArrayList<>(cards);
        }

        public PlayerSeat getSeat() {
            return seat;
        }

        public void setSeat(PlayerSeat seat) {
            this.seat = seat;
        }

        public List<Card> getCards() {
            return cards;
        }

        public void setCards(List<Card> cards) {
            this.cards = cards;
        }
    }

    public static class PlayedTrick {
        private int index;
        private PlayerSeat leader;
        private PlayerSeat winner;
        private int points;
        private List<TrickPlay> plays = new ArrayList<>();

        public PlayedTrick() {
        }

        public PlayedTrick(int index, PlayerSeat leader, PlayerSeat winner, int points, List<TrickPlay> plays) {
            this.index = index;
            this.leader = leader;
            this.winner = winner;
            this.points = points;
            this.plays = new ArrayList<>(plays);
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public PlayerSeat getLeader() {
            return leader;
        }

        public void setLeader(PlayerSeat leader) {
            this.leader = leader;
        }

        public PlayerSeat getWinner() {
            return winner;
        }

        public void setWinner(PlayerSeat winner) {
            this.winner = winner;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public List<TrickPlay> getPlays() {
            return plays;
        }

        public void setPlays(List<TrickPlay> plays) {
            this.plays = plays;
        }
    }
}