import 'dart:convert';

import 'package:http/http.dart' as http;

import 'auth_models.dart';
import 'chat_models.dart';
import 'friend_models.dart';
import 'models.dart';
import 'record_models.dart';

class GameApiException implements Exception {
  GameApiException({required this.code, required this.message});

  final String code;
  final String message;

  @override
  String toString() => message;
}

abstract interface class GameApi {
  Future<GameStateModel> createGame();

  Future<GameStateModel> getGame(String gameId);

  Future<GameStateModel> declare(String gameId, String suit,
      {String? playerId});

  Future<GameStateModel> kitty(String gameId, List<CardModel> cards,
      {String? playerId});

  Future<GameStateModel> play(String gameId, List<CardModel> cards,
      {String? playerId});

  Future<GameStateModel> aiStep(String gameId);

  Future<GameStateModel> nextGame(String gameId);

  Future<PlayerProfileModel> createGuestPlayer();

  Future<AuthSessionModel> register({
    required String username,
    required String password,
    String? playerId,
  });

  Future<AuthSessionModel> login({
    required String username,
    required String password,
  });

  void setSessionToken(String? sessionToken);

  Future<void> logout();

  Future<List<ChatMessageModel>> fetchRoomMessages(String roomId);

  Future<ChatMessageModel> sendRoomMessage({
    required String roomId,
    required String playerId,
    required String content,
  });

  Future<List<GameRecordModel>> fetchPlayerRecords(String playerId);

  Future<List<FriendshipModel>> fetchFriends(String playerId);

  Future<FriendshipModel> sendFriendRequest({
    required String requesterPlayerId,
    required String addresseePlayerId,
  });

  Future<FriendshipModel> acceptFriendRequest({
    required String friendshipId,
    required String playerId,
  });

  Future<void> deleteFriendship({
    required String friendshipId,
    required String playerId,
  });

  Future<RoomInvitationModel> createRoomInvitation({
    required String roomId,
    required String fromPlayerId,
    required String toPlayerId,
  });

  Future<List<RoomInvitationModel>> fetchPendingInvitations(String playerId);

  Future<RoomInvitationModel> respondToInvitation({
    required String invitationId,
    required String playerId,
    required bool accepted,
  });

  Future<List<RoomStateModel>> fetchRooms();

  Future<RoomStateModel> createRoom(String playerId);

  Future<RoomStateModel> getRoom(String roomId);

  Future<RoomStateModel> joinSeat(String roomId, String seat, String playerId);

  Future<RoomStateModel> leaveSeat(String roomId, String seat, String playerId);

  Future<RoomStateModel> addBot(String roomId, String seat, String playerId);

  Future<RoomStateModel> removeBot(String roomId, String seat, String playerId);

  Future<GameStateModel> startGame(String roomId, String playerId);
}

class ApiClient implements GameApi {
  ApiClient(
      {this.baseUrl = 'http://localhost:8080',
      http.Client? httpClient,
      String? sessionToken})
      : httpClient = httpClient ?? http.Client(),
        _sessionToken = sessionToken;

  final String baseUrl;
  final http.Client httpClient;
  String? _sessionToken;

  @override
  Future<GameStateModel> createGame() async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games'),
      headers: _headers(),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> getGame(String gameId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/games/$gameId'),
      headers: _headers(),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> declare(String gameId, String suit,
      {String? playerId}) async {
    await _ensureSession();
    final body = <String, dynamic>{'suit': suit};
    if (playerId != null) body['playerId'] = playerId;
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games/$gameId/declare'),
      headers: _headers(json: true),
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> kitty(String gameId, List<CardModel> cards,
      {String? playerId}) async {
    await _ensureSession();
    final body = <String, dynamic>{
      'cards': cards.map((card) => card.toJson()).toList()
    };
    if (playerId != null) body['playerId'] = playerId;
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games/$gameId/kitty'),
      headers: _headers(json: true),
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> play(String gameId, List<CardModel> cards,
      {String? playerId}) async {
    await _ensureSession();
    final body = <String, dynamic>{
      'seat': 'SOUTH',
      'cards': cards.map((card) => card.toJson()).toList(),
    };
    if (playerId != null) body['playerId'] = playerId;
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games/$gameId/play'),
      headers: _headers(json: true),
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> aiStep(String gameId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games/$gameId/ai/step'),
      headers: _headers(),
    );
    return _decode(response);
  }

  @override
  Future<GameStateModel> nextGame(String gameId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/games/$gameId/next'),
      headers: _headers(),
    );
    return _decode(response);
  }

  @override
  Future<PlayerProfileModel> createGuestPlayer() async {
    final response =
        await httpClient.post(Uri.parse('$baseUrl/api/players/guest'));
    final profile = _decodePlayer(response);
    _sessionToken = profile.sessionToken;
    return profile;
  }

  @override
  Future<AuthSessionModel> register({
    required String username,
    required String password,
    String? playerId,
  }) async {
    final body = <String, dynamic>{
      'username': username,
      'password': password,
    };
    if (playerId != null) body['playerId'] = playerId;
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/auth/register'),
      headers: _headers(json: true),
      body: jsonEncode(body),
    );
    final session = _decodeAuth(response);
    _sessionToken = session.sessionToken;
    return session;
  }

  @override
  Future<AuthSessionModel> login({
    required String username,
    required String password,
  }) async {
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/auth/login'),
      headers: _headers(json: true),
      body: jsonEncode({
        'username': username,
        'password': password,
      }),
    );
    final session = _decodeAuth(response);
    _sessionToken = session.sessionToken;
    return session;
  }

  @override
  void setSessionToken(String? sessionToken) {
    _sessionToken = sessionToken;
  }

  @override
  Future<void> logout() async {
    final token = _sessionToken;
    if (token == null || token.isEmpty) return;
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/auth/logout'),
      headers: _headers(json: true),
      body: jsonEncode({'sessionToken': token}),
    );
    _throwIfError(response);
    _sessionToken = null;
  }

  @override
  Future<List<ChatMessageModel>> fetchRoomMessages(String roomId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/rooms/$roomId/messages'),
      headers: _headers(),
    );
    return _decodeChatMessages(response);
  }

  @override
  Future<ChatMessageModel> sendRoomMessage({
    required String roomId,
    required String playerId,
    required String content,
  }) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms/$roomId/messages'),
      headers: _headers(json: true),
      body: jsonEncode({
        'playerId': playerId,
        'content': content,
      }),
    );
    return _decodeChatMessage(response);
  }

  @override
  Future<List<FriendshipModel>> fetchFriends(String playerId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/players/$playerId/friends'),
      headers: _headers(),
    );
    return _decodeFriendships(response);
  }

  @override
  Future<FriendshipModel> sendFriendRequest({
    required String requesterPlayerId,
    required String addresseePlayerId,
  }) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/friends/requests'),
      headers: _headers(json: true),
      body: jsonEncode({
        'requesterPlayerId': requesterPlayerId,
        'addresseePlayerId': addresseePlayerId,
      }),
    );
    return _decodeFriendship(response);
  }

  @override
  Future<FriendshipModel> acceptFriendRequest({
    required String friendshipId,
    required String playerId,
  }) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/friends/requests/$friendshipId/accept'),
      headers: _headers(json: true),
      body: jsonEncode({'playerId': playerId}),
    );
    return _decodeFriendship(response);
  }

  @override
  Future<void> deleteFriendship({
    required String friendshipId,
    required String playerId,
  }) async {
    await _ensureSession();
    final request = http.Request(
      'DELETE',
      Uri.parse('$baseUrl/api/friends/$friendshipId'),
    );
    request.headers.addAll(_headers(json: true));
    request.body = jsonEncode({'playerId': playerId});
    final response = await httpClient.send(request);
    final body = await http.Response.fromStream(response);
    _throwIfError(body);
  }

  @override
  Future<RoomInvitationModel> createRoomInvitation({
    required String roomId,
    required String fromPlayerId,
    required String toPlayerId,
  }) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms/$roomId/invitations'),
      headers: _headers(json: true),
      body: jsonEncode({
        'fromPlayerId': fromPlayerId,
        'toPlayerId': toPlayerId,
      }),
    );
    return _decodeRoomInvitation(response);
  }

  @override
  Future<List<RoomInvitationModel>> fetchPendingInvitations(
      String playerId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/players/$playerId/invitations'),
      headers: _headers(),
    );
    return _decodeRoomInvitations(response);
  }

  @override
  Future<RoomInvitationModel> respondToInvitation({
    required String invitationId,
    required String playerId,
    required bool accepted,
  }) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/invitations/$invitationId/respond'),
      headers: _headers(json: true),
      body: jsonEncode({
        'playerId': playerId,
        'accepted': accepted,
      }),
    );
    return _decodeRoomInvitation(response);
  }

  @override
  Future<List<GameRecordModel>> fetchPlayerRecords(String playerId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/players/$playerId/records'),
      headers: _headers(),
    );
    return _decodeRecords(response);
  }

  @override
  Future<List<RoomStateModel>> fetchRooms() async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/rooms'),
      headers: _headers(),
    );
    return _decodeRooms(response);
  }

  @override
  Future<RoomStateModel> createRoom(String playerId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms'),
      headers: _headers(json: true),
      body: jsonEncode({'playerId': playerId}),
    );
    return _decodeRoom(response);
  }

  @override
  Future<RoomStateModel> getRoom(String roomId) async {
    await _ensureSession();
    final response = await httpClient.get(
      Uri.parse('$baseUrl/api/rooms/$roomId'),
      headers: _headers(),
    );
    return _decodeRoom(response);
  }

  @override
  Future<RoomStateModel> joinSeat(
      String roomId, String seat, String playerId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat'),
      headers: _headers(json: true),
      body: jsonEncode({'playerId': playerId}),
    );
    return _decodeRoom(response);
  }

  @override
  Future<RoomStateModel> leaveSeat(
      String roomId, String seat, String playerId) async {
    await _ensureSession();
    final request = http.Request(
        'DELETE', Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat'));
    request.headers.addAll(_headers(json: true));
    request.body = jsonEncode({'playerId': playerId});
    final response = await httpClient.send(request);
    final body = await http.Response.fromStream(response);
    return _decodeRoom(body);
  }

  @override
  Future<RoomStateModel> addBot(
      String roomId, String seat, String playerId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat/bot'),
      headers: _headers(json: true),
      body: jsonEncode({'playerId': playerId}),
    );
    return _decodeRoom(response);
  }

  @override
  Future<RoomStateModel> removeBot(
      String roomId, String seat, String playerId) async {
    await _ensureSession();
    final request = http.Request(
        'DELETE', Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat/bot'));
    request.headers.addAll(_headers(json: true));
    request.body = jsonEncode({'playerId': playerId});
    final response = await httpClient.send(request);
    final body = await http.Response.fromStream(response);
    return _decodeRoom(body);
  }

  @override
  Future<GameStateModel> startGame(String roomId, String playerId) async {
    await _ensureSession();
    final response = await httpClient.post(
      Uri.parse('$baseUrl/api/rooms/$roomId/start'),
      headers: _headers(json: true),
      body: jsonEncode({'playerId': playerId}),
    );
    return _decode(response);
  }

  Future<void> _ensureSession() async {
    if (_sessionToken != null && _sessionToken!.isNotEmpty) return;
    await createGuestPlayer();
  }

  Map<String, String> _headers({bool json = false}) {
    final headers = <String, String>{};
    if (json) headers['Content-Type'] = 'application/json';
    final token = _sessionToken;
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  GameStateModel _decode(http.Response response) {
    _throwIfError(response);
    return GameStateModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  PlayerProfileModel _decodePlayer(http.Response response) {
    _throwIfError(response);
    return PlayerProfileModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  AuthSessionModel _decodeAuth(http.Response response) {
    _throwIfError(response);
    return AuthSessionModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  ChatMessageModel _decodeChatMessage(http.Response response) {
    _throwIfError(response);
    return ChatMessageModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  List<ChatMessageModel> _decodeChatMessages(http.Response response) {
    _throwIfError(response);
    return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
        .map((item) => ChatMessageModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  FriendshipModel _decodeFriendship(http.Response response) {
    _throwIfError(response);
    return FriendshipModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  List<FriendshipModel> _decodeFriendships(http.Response response) {
    _throwIfError(response);
    return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
        .map((item) => FriendshipModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  RoomInvitationModel _decodeRoomInvitation(http.Response response) {
    _throwIfError(response);
    return RoomInvitationModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  List<RoomInvitationModel> _decodeRoomInvitations(http.Response response) {
    _throwIfError(response);
    return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
        .map((item) =>
            RoomInvitationModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  List<GameRecordModel> _decodeRecords(http.Response response) {
    _throwIfError(response);
    return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
        .map((item) => GameRecordModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  RoomStateModel _decodeRoom(http.Response response) {
    _throwIfError(response);
    return RoomStateModel.fromJson(
      jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
    );
  }

  List<RoomStateModel> _decodeRooms(http.Response response) {
    _throwIfError(response);
    return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
        .map((item) => RoomStateModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  void _throwIfError(http.Response response) {
    if (response.statusCode < 400) return;
    final body = utf8.decode(response.bodyBytes);
    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      if (json.containsKey('code') && json.containsKey('message')) {
        throw GameApiException(
          code: json['code'] as String,
          message: json['message'] as String,
        );
      }
    } catch (e) {
      if (e is GameApiException) rethrow;
    }
    throw Exception(body);
  }
}
