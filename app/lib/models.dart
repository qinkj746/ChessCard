class CardModel {
  const CardModel({
    required this.suit,
    required this.rank,
    required this.deckIndex,
  });

  final String? suit;
  final String rank;
  final int deckIndex;

  factory CardModel.fromJson(Map<String, dynamic> json) {
    return CardModel(
      suit: json['suit'] as String?,
      rank: json['rank'] as String,
      deckIndex: json['deckIndex'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'suit': suit,
      'rank': rank,
      'deckIndex': deckIndex,
    };
  }

  @override
  bool operator ==(Object other) {
    return other is CardModel &&
        other.suit == suit &&
        other.rank == rank &&
        other.deckIndex == deckIndex;
  }

  @override
  int get hashCode => Object.hash(suit, rank, deckIndex);
}

class PlayerProfileModel {
  const PlayerProfileModel({
    required this.playerId,
    required this.displayName,
    required this.guest,
    required this.sessionToken,
  });

  final String playerId;
  final String displayName;
  final bool guest;
  final String sessionToken;

  factory PlayerProfileModel.fromJson(Map<String, dynamic> json) {
    return PlayerProfileModel(
      playerId: json['playerId'] as String,
      displayName: json['displayName'] as String,
      guest: json['guest'] as bool,
      sessionToken: json['sessionToken'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is PlayerProfileModel &&
        other.playerId == playerId &&
        other.displayName == displayName &&
        other.guest == guest &&
        other.sessionToken == sessionToken;
  }

  @override
  int get hashCode => Object.hash(playerId, displayName, guest, sessionToken);
}

class SeatInfo {
  const SeatInfo({required this.playerId, String? displayName})
      : _displayName = displayName;

  final String playerId;
  final String? _displayName;

  String get displayName {
    final value = _displayName?.trim();
    return value == null || value.isEmpty ? playerId : value;
  }

  factory SeatInfo.fromJson(Map<String, dynamic> json) {
    return SeatInfo(
      playerId: json['playerId'] as String,
      displayName: json['displayName'] as String?,
    );
  }
}

class RoomStateModel {
  const RoomStateModel({
    required this.roomId,
    required this.phase,
    required this.ownerPlayerId,
    this.gameId,
    this.version = 0,
    required this.seats,
  });

  final String roomId;
  final String phase;
  final String ownerPlayerId;
  final String? gameId;
  final int version;
  final Map<String, SeatInfo> seats;

  factory RoomStateModel.fromJson(Map<String, dynamic> json) {
    return RoomStateModel(
      roomId: json['roomId'] as String,
      phase: json['phase'] as String,
      ownerPlayerId: json['ownerPlayerId'] as String,
      gameId: json['gameId'] as String?,
      version: json['version'] as int? ?? 0,
      seats: (json['seats'] as Map<String, dynamic>).map(
        (key, value) =>
            MapEntry(key, SeatInfo.fromJson(value as Map<String, dynamic>)),
      ),
    );
  }
}

class RoomEventModel {
  const RoomEventModel({
    required this.type,
    required this.roomId,
    this.gameId,
    this.version,
    this.payload,
    this.occurredAt,
  });

  final String type;
  final String roomId;
  final String? gameId;
  final int? version;
  final Object? payload;
  final DateTime? occurredAt;

  factory RoomEventModel.fromJson(Map<String, dynamic> json) {
    return RoomEventModel(
      type: json['type'] as String,
      roomId: json['roomId'] as String,
      gameId: json['gameId'] as String?,
      version: json['version'] as int?,
      payload: json['payload'],
      occurredAt: json['occurredAt'] == null
          ? null
          : DateTime.parse(json['occurredAt'] as String),
    );
  }
}

class TrickPlayModel {
  const TrickPlayModel({required this.seat, required this.cards});

  final String seat;
  final List<CardModel> cards;

  factory TrickPlayModel.fromJson(Map<String, dynamic> json) {
    return TrickPlayModel(
      seat: json['seat'] as String,
      cards: (json['cards'] as List<dynamic>)
          .map((item) => CardModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class PlayedTrickModel {
  const PlayedTrickModel({
    required this.index,
    required this.leader,
    required this.winner,
    required this.points,
    required this.plays,
  });

  final int index;
  final String leader;
  final String winner;
  final int points;
  final List<TrickPlayModel> plays;

  factory PlayedTrickModel.fromJson(Map<String, dynamic> json) {
    return PlayedTrickModel(
      index: json['index'] as int,
      leader: json['leader'] as String,
      winner: json['winner'] as String,
      points: json['points'] as int? ?? 0,
      plays: (json['plays'] as List<dynamic>)
          .map((item) => TrickPlayModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class GameStateModel {
  const GameStateModel({
    required this.id,
    required this.phase,
    required this.levelRank,
    required this.trumpSuit,
    required this.banker,
    required this.currentTurn,
    required this.attackerScore,
    required this.winningTeam,
    required this.levelDelta,
    required this.nextLevelRank,
    required this.completed,
    required this.handCounts,
    required this.southHand,
    required this.kitty,
    required this.currentTrick,
    this.currentTrickPlays = const [],
    this.playedTricks = const [],
    required this.declarationOptions,
    this.lastActionMessage,
  });

  final String id;
  final String phase;
  final String levelRank;
  final String? trumpSuit;
  final String? banker;
  final String currentTurn;
  final int attackerScore;
  final String? winningTeam;
  final int levelDelta;
  final String? nextLevelRank;
  final bool completed;
  final Map<String, int> handCounts;
  final List<CardModel> southHand;
  final List<CardModel> kitty;
  final Map<String, List<CardModel>> currentTrick;
  final List<TrickPlayModel> currentTrickPlays;
  final List<PlayedTrickModel> playedTricks;
  final List<String> declarationOptions;
  final String? lastActionMessage;

  factory GameStateModel.fromJson(Map<String, dynamic> json) {
    return GameStateModel(
      id: json['id'] as String,
      phase: json['phase'] as String,
      levelRank: json['levelRank'] as String,
      trumpSuit: json['trumpSuit'] as String?,
      banker: json['banker'] as String?,
      currentTurn: json['currentTurn'] as String,
      attackerScore: json['attackerScore'] as int,
      winningTeam: json['winningTeam'] as String?,
      levelDelta: json['levelDelta'] as int? ?? 0,
      nextLevelRank: json['nextLevelRank'] as String?,
      completed: json['completed'] as bool? ?? false,
      handCounts: (json['handCounts'] as Map<String, dynamic>).map(
        (key, value) => MapEntry(key, value as int),
      ),
      southHand: (json['southHand'] as List<dynamic>)
          .map((item) => CardModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      kitty: (json['kitty'] as List<dynamic>)
          .map((item) => CardModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      currentTrick: ((json['currentTrick'] as Map<String, dynamic>?) ?? {}).map(
        (seat, cards) => MapEntry(
          seat,
          (cards as List<dynamic>)
              .map((item) => CardModel.fromJson(item as Map<String, dynamic>))
              .toList(),
        ),
      ),
      currentTrickPlays: ((json['currentTrickPlays'] as List<dynamic>?) ?? [])
          .map((item) => TrickPlayModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      playedTricks: ((json['playedTricks'] as List<dynamic>?) ?? [])
          .map(
              (item) => PlayedTrickModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      declarationOptions:
          (json['declarationOptions'] as List<dynamic>).cast<String>(),
      lastActionMessage: json['lastActionMessage'] as String?,
    );
  }
}
