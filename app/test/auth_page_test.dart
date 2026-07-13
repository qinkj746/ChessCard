import 'package:chess_card_app/api_client.dart';
import 'package:chess_card_app/auth_controller.dart';
import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/auth_page.dart';
import 'package:chess_card_app/auth_session_storage.dart';
import 'package:chess_card_app/models.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('register mode blocks mismatched passwords without calling API',
      (tester) async {
    final api = FakeAuthApi();
    final controller = AuthController(
      api: api,
      storage: MemoryAuthSessionStorage(),
    );

    await _pumpAuthPage(tester, controller);
    await tester.tap(find.byKey(const Key('auth_mode_register')));
    await tester.enterText(find.byKey(const Key('auth_username')), 'alice');
    await tester.enterText(find.byKey(const Key('auth_password')), 'secret123');
    await tester.enterText(
        find.byKey(const Key('auth_confirm_password')), 'different');
    await tester.tap(find.byKey(const Key('auth_submit')));
    await tester.pump();

    expect(api.loginCalls, 0);
    expect(api.registerCalls, 0);
    expect(find.text('两次输入的密码不一致'), findsOneWidget);
  });

  testWidgets('login submits credentials and closes the page on success',
      (tester) async {
    final api = FakeAuthApi();
    final controller = AuthController(
      api: api,
      storage: MemoryAuthSessionStorage(),
    );
    bool? result;

    await _pumpLaunchPage(tester, controller, (value) => result = value);
    await tester.tap(find.byKey(const Key('open_auth')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('auth_username')), 'alice');
    await tester.enterText(find.byKey(const Key('auth_password')), 'secret123');
    await tester.tap(find.byKey(const Key('auth_submit')));
    await tester.pumpAndSettle();

    expect(result, isTrue);
    expect(api.loginCalls, 1);
    expect(controller.isSignedIn, isTrue);
    expect(controller.playerId, 'account-1');
  });

  testWidgets('authentication failure remains visible and re-enables submit',
      (tester) async {
    final api = FakeAuthApi()
      ..loginError = GameApiException(
        code: 'INVALID_CREDENTIALS',
        message: '用户名或密码错误',
      );
    final controller = AuthController(
      api: api,
      storage: MemoryAuthSessionStorage(),
    );

    await _pumpAuthPage(tester, controller);
    await tester.enterText(find.byKey(const Key('auth_username')), 'alice');
    await tester.enterText(find.byKey(const Key('auth_password')), 'secret123');
    await tester.tap(find.byKey(const Key('auth_submit')));
    await tester.pumpAndSettle();

    expect(find.text('用户名或密码错误'), findsOneWidget);
    expect(
      tester
          .widget<FilledButton>(find.byKey(const Key('auth_submit')))
          .onPressed,
      isNotNull,
    );
  });
}

Future<void> _pumpAuthPage(WidgetTester tester, AuthController controller) {
  return tester.pumpWidget(
    MaterialApp(home: AuthPage(controller: controller)),
  );
}

Future<void> _pumpLaunchPage(
  WidgetTester tester,
  AuthController controller,
  void Function(bool?) onResult,
) {
  return tester.pumpWidget(
    MaterialApp(
      home: Builder(
        builder: (context) => Scaffold(
          body: FilledButton(
            key: const Key('open_auth'),
            onPressed: () async {
              final result = await Navigator.of(context).push<bool>(
                MaterialPageRoute(
                    builder: (_) => AuthPage(controller: controller)),
              );
              onResult(result);
            },
            child: const Text('打开登录'),
          ),
        ),
      ),
    ),
  );
}

class MemoryAuthSessionStorage implements AuthSessionStorage {
  AuthSessionModel? session;

  @override
  Future<void> clear() async {
    session = null;
  }

  @override
  Future<AuthSessionModel?> read() async => session;

  @override
  Future<void> write(AuthSessionModel nextSession) async {
    session = nextSession;
  }
}

class FakeAuthApi extends ApiClient {
  FakeAuthApi() : super(baseUrl: 'http://unused.test');

  int loginCalls = 0;
  int registerCalls = 0;
  Object? loginError;

  final sessionResult = const AuthSessionModel(
    playerId: 'account-1',
    username: 'alice',
    displayName: 'Alice',
    sessionToken: 'token-1',
  );

  @override
  Future<AuthSessionModel> login({
    required String username,
    required String password,
  }) async {
    loginCalls += 1;
    final error = loginError;
    if (error != null) throw error;
    return sessionResult;
  }

  @override
  Future<AuthSessionModel> register({
    required String username,
    required String password,
    String? playerId,
  }) async {
    registerCalls += 1;
    return sessionResult;
  }

  @override
  Future<PlayerProfileModel> createGuestPlayer() async =>
      throw UnsupportedError('not used by auth page tests');
}
