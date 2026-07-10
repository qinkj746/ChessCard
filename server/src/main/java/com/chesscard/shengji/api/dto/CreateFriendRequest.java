package com.chesscard.shengji.api.dto;

public record CreateFriendRequest(String requesterPlayerId, String addresseePlayerId) {
}