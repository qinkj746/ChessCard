package com.chesscard.shengji.api.dto;

import com.chesscard.shengji.domain.Suit;

public record DeclareRequest(Suit suit, String playerId) {
}
