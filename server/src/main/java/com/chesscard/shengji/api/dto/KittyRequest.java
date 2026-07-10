package com.chesscard.shengji.api.dto;

import java.util.List;

public record KittyRequest(List<CardDto> cards, String playerId) {
}
