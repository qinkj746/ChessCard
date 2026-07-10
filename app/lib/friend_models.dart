class FriendshipModel {
  const FriendshipModel({
    required this.friendshipId,
    required this.requesterPlayerId,
    required this.addresseePlayerId,
    required this.status,
    this.createdAt,
    this.updatedAt,
  });

  final String friendshipId;
  final String requesterPlayerId;
  final String addresseePlayerId;
  final String status;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory FriendshipModel.fromJson(Map<String, dynamic> json) {
    return FriendshipModel(
      friendshipId: json['friendshipId'] as String,
      requesterPlayerId: json['requesterPlayerId'] as String,
      addresseePlayerId: json['addresseePlayerId'] as String,
      status: json['status'] as String,
      createdAt: _parseOptionalDate(json['createdAt']),
      updatedAt: _parseOptionalDate(json['updatedAt']),
    );
  }

  String otherPlayerId(String playerId) =>
      requesterPlayerId == playerId ? addresseePlayerId : requesterPlayerId;
}

class RoomInvitationModel {
  const RoomInvitationModel({
    required this.invitationId,
    required this.roomId,
    required this.fromPlayerId,
    required this.toPlayerId,
    required this.status,
    required this.createdAt,
    required this.expiresAt,
  });

  final String invitationId;
  final String roomId;
  final String fromPlayerId;
  final String toPlayerId;
  final String status;
  final DateTime createdAt;
  final DateTime expiresAt;

  factory RoomInvitationModel.fromJson(Map<String, dynamic> json) {
    return RoomInvitationModel(
      invitationId: json['invitationId'] as String,
      roomId: json['roomId'] as String,
      fromPlayerId: json['fromPlayerId'] as String,
      toPlayerId: json['toPlayerId'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      expiresAt: DateTime.parse(json['expiresAt'] as String),
    );
  }
}

DateTime? _parseOptionalDate(Object? value) {
  if (value is! String || value.isEmpty) return null;
  return DateTime.parse(value);
}
