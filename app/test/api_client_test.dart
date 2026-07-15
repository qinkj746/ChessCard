import 'dart:convert';

import 'package:chess_card_app/api_client.dart';
import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/chat_models.dart';
import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/record_models.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  test('createGame creates guest session and sends bearer token', () async {
    final requests = <http.Request>[];
    final client = ApiClient(
      baseUrl: 'http://example.test',
      httpClient: MockClient((request) async {
        requests.add(request);
        if (request.url.path == '/api/players/guest') {
          return http.Response(
            jsonEncode({
              'playerId': 'guest-1',
              'displayName': 'Guest-00001',
              'guest': true,
              'sessionToken': 'token-abc',
            }),
            200,
            headers: {'Content-Type': 'application/json'},
          );
        }
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.createGame();

    expect(requests.map((request) => request.url.path), [
      '/api/players/guest',
      '/api/games',
    ]);
    expect(requests.last.headers['Authorization'], 'Bearer token-abc');
  });

  test('play surfaces standard error response body', () async {
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient(
        (_) async => http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'code': 'INVALID_OPERATION',
              'message': 'invalid play',
              'requestId': 'abc',
            }),
          ),
          400,
        ),
      ),
    );

    expect(
      () => client.play('game-1',
          const [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)]),
      throwsA(
        isA<GameApiException>()
            .having((e) => e.code, 'code', 'INVALID_OPERATION')
            .having((e) => e.message, 'message', 'invalid play'),
      ),
    );
  });

  test('get game sends get to game endpoint', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final result = await client.getGame('game-1');

    expect(capturedRequest.method, 'GET');
    expect(
        capturedRequest.url.toString(), 'http://example.test/api/games/game-1');
    expect(result.id, 'game-1');
  });

  test('play sends south seat and selected cards', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.play(
        'game-1', const [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)]);

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/games/game-1/play');
    expect(jsonDecode(capturedRequest.body), {
      'seat': 'SOUTH',
      'cards': [
        {'suit': 'SPADE', 'rank': 'FIVE', 'deckIndex': 0},
      ],
    });
  });

  test('next game posts to next endpoint', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.nextGame('game-1');

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/games/game-1/next');
    expect(capturedRequest.body, isEmpty);
  });

  test('ai step posts to ai step endpoint', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.aiStep('game-1');

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/games/game-1/ai/step');
    expect(capturedRequest.body, isEmpty);
  });

  test('declare posts selected suit', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.declare('game-1', 'HEART');

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/games/game-1/declare');
    expect(jsonDecode(capturedRequest.body), {'suit': 'HEART'});
  });

  test('kitty posts selected cards', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.kitty(
        'game-1', const [CardModel(suit: 'HEART', rank: 'KING', deckIndex: 1)]);

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/games/game-1/kitty');
    expect(jsonDecode(capturedRequest.body), {
      'cards': [
        {'suit': 'HEART', 'rank': 'KING', 'deckIndex': 1},
      ],
    });
  });

  test('createGuestPlayer posts to players guest endpoint', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(
          jsonEncode({
            'playerId': 'abc-123',
            'displayName': 'Guest-1234',
            'guest': true,
            'sessionToken': 'token-xyz',
          }),
          200,
          headers: {'Content-Type': 'application/json'},
        );
      }),
    );

    final result = await client.createGuestPlayer();

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/players/guest');
    expect(result.playerId, 'abc-123');
    expect(result.displayName, 'Guest-1234');
    expect(result.guest, true);
    expect(result.sessionToken, 'token-xyz');
  });

  test('register posts credentials and stores returned session token',
      () async {
    final requests = <http.Request>[];
    final client = ApiClient(
      baseUrl: 'http://example.test',
      httpClient: MockClient((request) async {
        requests.add(request);
        if (request.url.path == '/api/auth/register') {
          return http.Response(
            _authSessionJson(sessionToken: 'registered-token'),
            200,
            headers: {'Content-Type': 'application/json'},
          );
        }
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final AuthSessionModel session = await client.register(
      username: 'alice',
      password: 'secret123',
      playerId: 'guest-1',
    );
    await client.getGame('game-1');

    expect(requests.first.method, 'POST');
    expect(
        requests.first.url.toString(), 'http://example.test/api/auth/register');
    expect(jsonDecode(requests.first.body), {
      'username': 'alice',
      'password': 'secret123',
      'playerId': 'guest-1',
    });
    expect(session.username, 'alice');
    expect(session.sessionToken, 'registered-token');
    expect(requests.last.headers['Authorization'], 'Bearer registered-token');
  });

  test('login posts credentials and stores returned session token', () async {
    final requests = <http.Request>[];
    final client = ApiClient(
      baseUrl: 'http://example.test',
      httpClient: MockClient((request) async {
        requests.add(request);
        if (request.url.path == '/api/auth/login') {
          return http.Response(
            _authSessionJson(sessionToken: 'login-token'),
            200,
            headers: {'Content-Type': 'application/json'},
          );
        }
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.login(username: 'alice', password: 'secret123');
    await client.getGame('game-1');

    expect(requests.first.method, 'POST');
    expect(requests.first.url.toString(), 'http://example.test/api/auth/login');
    expect(jsonDecode(requests.first.body), {
      'username': 'alice',
      'password': 'secret123',
    });
    expect(requests.last.headers['Authorization'], 'Bearer login-token');
  });

  test('fetchRoomMessages gets room chat messages with bearer token', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_chatMessageListJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final List<ChatMessageModel> messages =
        await client.fetchRoomMessages('room-1');

    expect(capturedRequest.method, 'GET');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/rooms/room-1/messages');
    expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
    expect(messages.single.content, 'hello');
  });

  test('sendRoomMessage posts chat payload and parses message', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_chatMessageJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final ChatMessageModel message = await client.sendRoomMessage(
      roomId: 'room-1',
      playerId: 'player-1',
      content: 'hello',
    );

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/rooms/room-1/messages');
    expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
    expect(jsonDecode(capturedRequest.body), {
      'playerId': 'player-1',
      'content': 'hello',
    });
    expect(message.content, 'hello');
  });

  test('fetchPlayerRecords gets player records with bearer token', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_recordListJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final List<GameRecordModel> records =
        await client.fetchPlayerRecords('player-south');

    expect(capturedRequest.method, 'GET');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/players/player-south/records');
    expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
    expect(records.single.gameId, 'game-1');
  });

  test('logout posts current token and clears session', () async {
    final requests = <http.Request>[];
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        requests.add(request);
        if (request.url.path == '/api/auth/logout') {
          return http.Response('', 200);
        }
        if (request.url.path == '/api/players/guest') {
          return http.Response(
            jsonEncode({
              'playerId': 'guest-2',
              'displayName': 'Guest-00002',
              'guest': true,
              'sessionToken': 'guest-token',
            }),
            200,
            headers: {'Content-Type': 'application/json'},
          );
        }
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    await client.logout();
    await client.getGame('game-1');

    expect(requests.first.method, 'POST');
    expect(
        requests.first.url.toString(), 'http://example.test/api/auth/logout');
    expect(jsonDecode(requests.first.body), {'sessionToken': 'token-abc'});
    expect(requests.map((request) => request.url.path), [
      '/api/auth/logout',
      '/api/players/guest',
      '/api/games/game-1',
    ]);
    expect(requests.last.headers['Authorization'], 'Bearer guest-token');
  });
  test('setSessionToken uses a restored token for later requests', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_chatMessageListJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    client.setSessionToken('restored-token');
    await client.fetchRoomMessages('room-1');

    expect(capturedRequest.headers['Authorization'], 'Bearer restored-token');
  });

  test('setSessionToken clears the token and resumes guest sessions', () async {
    final requests = <http.Request>[];
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'old-token',
      httpClient: MockClient((request) async {
        requests.add(request);
        if (request.url.path == '/api/players/guest') {
          return http.Response(
            jsonEncode({
              'playerId': 'guest-2',
              'displayName': 'Guest-00002',
              'guest': true,
              'sessionToken': 'guest-token',
            }),
            200,
            headers: {'Content-Type': 'application/json'},
          );
        }
        return http.Response(_gameJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    client.setSessionToken(null);
    await client.getGame('game-1');

    expect(requests.map((request) => request.url.path), [
      '/api/players/guest',
      '/api/games/game-1',
    ]);
    expect(requests.last.headers['Authorization'], 'Bearer guest-token');
  });

  test('addBot posts player id and parses bot seat', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_roomJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final room = await client.addBot('room-1', 'WEST', 'player-1');

    expect(capturedRequest.method, 'POST');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/rooms/room-1/seats/WEST/bot');
    expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
    expect(jsonDecode(capturedRequest.body), {'playerId': 'player-1'});
    expect(room.seats['SOUTH']!.isBot, isFalse);
    expect(room.seats['WEST']!.playerId, isNull);
    expect(room.seats['WEST']!.isBot, isTrue);
    expect(room.seats['WEST']!.displayName, '人机');
  });

  test('removeBot deletes with player id and parses room', () async {
    late http.Request capturedRequest;
    final client = ApiClient(
      baseUrl: 'http://example.test',
      sessionToken: 'token-abc',
      httpClient: MockClient((request) async {
        capturedRequest = request;
        return http.Response(_roomJson(), 200,
            headers: {'Content-Type': 'application/json'});
      }),
    );

    final room = await client.removeBot('room-1', 'WEST', 'player-1');

    expect(capturedRequest.method, 'DELETE');
    expect(capturedRequest.url.toString(),
        'http://example.test/api/rooms/room-1/seats/WEST/bot');
    expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
    expect(jsonDecode(capturedRequest.body), {'playerId': 'player-1'});
    expect(room.roomId, 'room-1');
    expect(room.seats['WEST']!.isBot, isTrue);
  });
}

String _gameJson() => jsonEncode({
      'id': 'game-1',
      'phase': 'PLAY',
      'levelRank': 'ACE',
      'trumpSuit': 'HEART',
      'banker': 'SOUTH',
      'currentTurn': 'WEST',
      'attackerScore': 0,
      'winningTeam': null,
      'levelDelta': 0,
      'nextLevelRank': null,
      'completed': false,
      'handCounts': {'SOUTH': 0, 'WEST': 1, 'NORTH': 1, 'EAST': 1},
      'southHand': [],
      'kitty': [],
      'currentTrick': {},
      'declarationOptions': [],
    });

String _authSessionJson({required String sessionToken}) => jsonEncode({
      'playerId': 'player-1',
      'username': 'alice',
      'displayName': 'alice',
      'sessionToken': sessionToken,
    });

String _roomJson() => jsonEncode({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'player-1',
      'gameId': null,
      'version': 2,
      'seats': {
        'SOUTH': {
          'playerId': 'player-1',
          'displayName': 'Alice',
          'isBot': false,
        },
        'WEST': {
          'playerId': null,
          'displayName': null,
          'isBot': true,
        },
      },
    });

String _recordListJson() => jsonEncode([
      {
        'recordId': 'record-1',
        'gameId': 'game-1',
        'roomId': 'room-1',
        'startedAt': '2026-07-10T08:00:00Z',
        'finishedAt': '2026-07-10T08:30:00Z',
        'players': {'SOUTH': 'player-south'},
        'winningTeam': 'SOUTH_NORTH',
        'attackerScore': 70,
        'levelDelta': 1,
        'nextLevelRank': 'TWO',
        'completed': false,
      }
    ]);

String _chatMessageJson() => jsonEncode({
      'messageId': 'message-1',
      'roomId': 'room-1',
      'senderPlayerId': 'player-1',
      'content': 'hello',
      'sentAt': '2026-07-10T08:00:00Z',
    });

String _chatMessageListJson() => jsonEncode([
      {
        'messageId': 'message-1',
        'roomId': 'room-1',
        'senderPlayerId': 'player-1',
        'content': 'hello',
        'sentAt': '2026-07-10T08:00:00Z',
      }
    ]);
