import 'dart:convert';

import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/auth_session_storage.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const session = AuthSessionModel(
    playerId: 'player-1',
    username: 'alice',
    displayName: 'Alice',
    sessionToken: 'token-1',
  );

  test('writes and restores a valid account session', () async {
    final store = MemoryKeyValueStore();
    final storage = SecureAuthSessionStorage(store);

    await storage.write(session);
    final restored = await storage.read();

    expect(
        jsonDecode(store.values['chesscard.auth_session']!), session.toJson());
    expect(restored?.playerId, 'player-1');
    expect(restored?.username, 'alice');
    expect(restored?.displayName, 'Alice');
    expect(restored?.sessionToken, 'token-1');
  });

  test('clears malformed persisted JSON and returns no session', () async {
    final store = MemoryKeyValueStore()
      ..values['chesscard.auth_session'] = '{not json';
    final storage = SecureAuthSessionStorage(store);

    final restored = await storage.read();

    expect(restored, isNull);
    expect(store.values, isNot(contains('chesscard.auth_session')));
  });

  test('clears incomplete persisted sessions and returns no session', () async {
    final store = MemoryKeyValueStore()
      ..values['chesscard.auth_session'] = jsonEncode({
        'playerId': 'player-1',
        'username': 'alice',
      });
    final storage = SecureAuthSessionStorage(store);

    final restored = await storage.read();

    expect(restored, isNull);
    expect(store.values, isNot(contains('chesscard.auth_session')));
  });
}

class MemoryKeyValueStore implements SecureKeyValueStore {
  final Map<String, String> values = {};

  @override
  Future<void> delete(String key) async {
    values.remove(key);
  }

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String value) async {
    values[key] = value;
  }
}
