import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'auth_models.dart';

abstract interface class SecureKeyValueStore {
  Future<String?> read(String key);

  Future<void> write(String key, String value);

  Future<void> delete(String key);
}

abstract interface class AuthSessionStorage {
  Future<AuthSessionModel?> read();

  Future<void> write(AuthSessionModel session);

  Future<void> clear();
}

class FlutterSecureKeyValueStore implements SecureKeyValueStore {
  FlutterSecureKeyValueStore([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  @override
  Future<void> delete(String key) => _storage.delete(key: key);

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);
}

class SecureAuthSessionStorage implements AuthSessionStorage {
  SecureAuthSessionStorage(this._store);

  static const _sessionKey = 'chesscard.auth_session';

  final SecureKeyValueStore _store;

  @override
  Future<void> clear() => _store.delete(_sessionKey);

  @override
  Future<AuthSessionModel?> read() async {
    final encoded = await _store.read(_sessionKey);
    if (encoded == null || encoded.isEmpty) return null;

    try {
      final decoded = jsonDecode(encoded);
      if (decoded is! Map<String, dynamic>) {
        throw const FormatException('Session must be a JSON object');
      }
      return AuthSessionModel.fromJson(decoded);
    } on FormatException {
      await clear();
      return null;
    } on TypeError {
      await clear();
      return null;
    }
  }

  @override
  Future<void> write(AuthSessionModel session) =>
      _store.write(_sessionKey, jsonEncode(session.toJson()));
}
