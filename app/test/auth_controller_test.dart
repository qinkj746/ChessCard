import 'package:chess_card_app/api_client.dart';
import 'package:chess_card_app/auth_controller.dart';
import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/auth_session_storage.dart';
import 'package:chess_card_app/models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const account = AuthSessionModel(
    playerId: 'account-1',
    username: 'alice',
    displayName: 'Alice',
    sessionToken: 'token-1',
  );

  test('initialize restores the saved account and seeds the API token',
      () async {
    final api = FakeGameApi();
    final storage = MemoryAuthSessionStorage(account);
    final controller = AuthController(api: api, storage: storage);

    await controller.initialize();

    expect(controller.initialized, isTrue);
    expect(controller.isSignedIn, isTrue);
    expect(controller.playerId, 'account-1');
    expect(api.setSessionTokenCalls, ['token-1']);
  });

  test('register binds the cached guest player id and persists the account',
      () async {
    final api = FakeGameApi();
    final storage = MemoryAuthSessionStorage();
    final controller = AuthController(api: api, storage: storage);

    final guestId = await controller.ensurePlayerId();
    await controller.register(username: 'alice', password: 'secret123');

    expect(guestId, 'guest-1');
    expect(api.registerPlayerIds, ['guest-1']);
    expect(controller.session, api.registerResult);
    expect(storage.savedSession, api.registerResult);
    expect(controller.playerId, 'account-1');
  });

  test('login replaces guest state and persists the returned account',
      () async {
    final api = FakeGameApi();
    final storage = MemoryAuthSessionStorage();
    final controller = AuthController(api: api, storage: storage);

    await controller.ensurePlayerId();
    await controller.login(username: 'alice', password: 'secret123');

    expect(storage.savedSession, api.loginResult);
    expect(api.setSessionTokenCalls.last, 'token-2');
    expect(await controller.ensurePlayerId(), 'account-2');
    expect(api.guestCreateCalls, 1);
  });

  test('ensurePlayerId returns an account id or creates one guest only once',
      () async {
    final signedIn = AuthController(
      api: FakeGameApi(),
      storage: MemoryAuthSessionStorage(account),
    );
    await signedIn.initialize();
    expect(await signedIn.ensurePlayerId(), 'account-1');

    final api = FakeGameApi();
    final guest = AuthController(api: api, storage: MemoryAuthSessionStorage());
    expect(await guest.ensurePlayerId(), 'guest-1');
    expect(await guest.ensurePlayerId(), 'guest-1');
    expect(api.guestCreateCalls, 1);
  });

  test('logout clears local state even when remote logout throws', () async {
    final api = FakeGameApi()..logoutError = StateError('offline');
    final storage = MemoryAuthSessionStorage(account);
    final controller = AuthController(api: api, storage: storage);
    await controller.initialize();

    await expectLater(controller.logout(), throwsA(isA<StateError>()));

    expect(controller.session, isNull);
    expect(controller.playerId, isNull);
    expect(storage.savedSession, isNull);
    expect(api.setSessionTokenCalls.last, isNull);
    expect(controller.busy, isFalse);
  });
}

class MemoryAuthSessionStorage implements AuthSessionStorage {
  MemoryAuthSessionStorage([this.savedSession]);

  AuthSessionModel? savedSession;

  @override
  Future<void> clear() async {
    savedSession = null;
  }

  @override
  Future<AuthSessionModel?> read() async => savedSession;

  @override
  Future<void> write(AuthSessionModel session) async {
    savedSession = session;
  }
}

class FakeGameApi extends ApiClient {
  FakeGameApi() : super(baseUrl: 'http://unused.test');

  final List<String?> setSessionTokenCalls = [];
  final List<String?> registerPlayerIds = [];
  int guestCreateCalls = 0;
  Object? logoutError;

  final registerResult = const AuthSessionModel(
    playerId: 'account-1',
    username: 'alice',
    displayName: 'Alice',
    sessionToken: 'token-1',
  );
  final loginResult = const AuthSessionModel(
    playerId: 'account-2',
    username: 'bob',
    displayName: 'Bob',
    sessionToken: 'token-2',
  );

  @override
  Future<PlayerProfileModel> createGuestPlayer() async {
    guestCreateCalls += 1;
    return const PlayerProfileModel(
      playerId: 'guest-1',
      displayName: 'Guest-00001',
      guest: true,
      sessionToken: 'guest-token',
    );
  }

  @override
  Future<AuthSessionModel> login({
    required String username,
    required String password,
  }) async =>
      loginResult;

  @override
  Future<AuthSessionModel> register({
    required String username,
    required String password,
    String? playerId,
  }) async {
    registerPlayerIds.add(playerId);
    return registerResult;
  }

  @override
  Future<void> logout() async {
    final error = logoutError;
    if (error != null) throw error;
  }

  @override
  void setSessionToken(String? sessionToken) {
    setSessionTokenCalls.add(sessionToken);
  }
}
