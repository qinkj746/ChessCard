class AuthSessionModel {
  const AuthSessionModel({
    required this.playerId,
    required this.username,
    required this.displayName,
    required this.sessionToken,
  });

  final String playerId;
  final String username;
  final String displayName;
  final String sessionToken;

  factory AuthSessionModel.fromJson(Map<String, dynamic> json) {
    return AuthSessionModel(
      playerId: json['playerId'] as String,
      username: json['username'] as String,
      displayName: json['displayName'] as String,
      sessionToken: json['sessionToken'] as String,
    );
  }
}
