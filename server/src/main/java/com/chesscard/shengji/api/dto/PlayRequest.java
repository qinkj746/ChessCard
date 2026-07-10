package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.PlayerSeat;

import java.util.List;

public record PlayRequest(PlayerSeat seat, List<CardDto> cards, String playerId) {
}
