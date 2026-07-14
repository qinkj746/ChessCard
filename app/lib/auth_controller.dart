import 'package:flutter/foundation.dart';

import 'api_client.dart';
import 'auth_models.dart';
import 'auth_session_storage.dart';
import 'models.dart';

class AuthController extends ChangeNotifier {
  AuthController({required this.api, required this.storage});

  final GameApi api;
  final AuthSessionStorage storage;

  AuthSessionModel? session;
  PlayerProfileModel? _guestProfile;
  bool initialized = false;
  bool busy = false;

  bool get isSignedIn => session != null;

  String? get playerId => session?.playerId ?? _guestProfile?.playerId;

  String? get displayName => session?.displayName ?? _guestProfile?.displayName;

  Future<void> initialize() async {
    if (initialized) return;

    try {
      session = await storage.read();
      api.setSessionToken(session?.sessionToken);
    } finally {
      initialized = true;
      notifyListeners();
    }
  }

  Future<String> ensurePlayerId() async {
    final account = session;
    if (account != null) return account.playerId;

    final guest = _guestProfile ?? await api.createGuestPlayer();
    _guestProfile = guest;
    return guest.playerId;
  }

  Future<void> login({
    required String username,
    required String password,
  }) async {
    await _runBusy(() async {
      final nextSession = await api.login(
        username: username,
        password: password,
      );
      await _saveSession(nextSession);
    });
  }

  Future<void> register({
    required String username,
    required String password,
  }) async {
    await _runBusy(() async {
      final nextSession = await api.register(
        username: username,
        password: password,
        playerId: _guestProfile?.playerId,
      );
      await _saveSession(nextSession);
    });
  }

  Future<void> logout() async {
    busy = true;
    notifyListeners();

    Object? remoteError;
    StackTrace? remoteStackTrace;
    try {
      await api.logout();
    } catch (error, stackTrace) {
      remoteError = error;
      remoteStackTrace = stackTrace;
    } finally {
      api.setSessionToken(null);
      session = null;
      _guestProfile = null;
      try {
        await storage.clear();
      } finally {
        busy = false;
        notifyListeners();
      }
    }

    if (remoteError != null) {
      Error.throwWithStackTrace(remoteError, remoteStackTrace!);
    }
  }

  Future<void> _runBusy(Future<void> Function() operation) async {
    busy = true;
    notifyListeners();
    try {
      await operation();
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<void> _saveSession(AuthSessionModel nextSession) async {
    api.setSessionToken(nextSession.sessionToken);
    await storage.write(nextSession);
    session = nextSession;
    _guestProfile = null;
  }
}
