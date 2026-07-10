class GameRecordModel {
  const GameRecordModel({
    required this.recordId,
    required this.gameId,
    required this.roomId,
    required this.startedAt,
    required this.finishedAt,
    required this.players,
    required this.winningTeam,
    required this.attackerScore,
    required this.levelDelta,
    required this.nextLevelRank,
    required this.completed,
  });

  final String recordId;
  final String gameId;
  final String? roomId;
  final DateTime startedAt;
  final DateTime finishedAt;
  final Map<String, String> players;
  final String winningTeam;
  final int attackerScore;
  final int levelDelta;
  final String nextLevelRank;
  final bool completed;

  factory GameRecordModel.fromJson(Map<String, dynamic> json) {
    return GameRecordModel(
      recordId: json['recordId'] as String,
      gameId: json['gameId'] as String,
      roomId: json['roomId'] as String?,
      startedAt: DateTime.parse(json['startedAt'] as String),
      finishedAt: DateTime.parse(json['finishedAt'] as String),
      players: (json['players'] as Map<String, dynamic>).map(
        (seat, playerId) => MapEntry(seat, playerId as String),
      ),
      winningTeam: json['winningTeam'] as String,
      attackerScore: json['attackerScore'] as int,
      levelDelta: json['levelDelta'] as int,
      nextLevelRank: json['nextLevelRank'] as String,
      completed: json['completed'] as bool,
    );
  }
}
