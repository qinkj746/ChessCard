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
    String requiredString(String key) {
      final value = json[key];
      if (value is! String || value.trim().isEmpty) {
        throw FormatException('Missing or invalid $key');
      }
      return value;
    }

    return AuthSessionModel(
      playerId: requiredString('playerId'),
      username: requiredString('username'),
      displayName: requiredString('displayName'),
      sessionToken: requiredString('sessionToken'),
    );
  }

  Map<String, dynamic> toJson() => {
        'playerId': playerId,
        'username': username,
        'displayName': displayName,
        'sessionToken': sessionToken,
      };
}
