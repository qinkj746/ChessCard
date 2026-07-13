import 'package:flutter/material.dart';

import 'api_client.dart';
import 'app_error.dart';
import 'auth_controller.dart';
import 'auth_page.dart';
import 'auth_session_storage.dart';
import 'game_page.dart';
import 'room_page.dart';

void main() {
  runApp(const ChessCardApp());
}

class ChessCardApp extends StatefulWidget {
  const ChessCardApp({super.key, this.api, this.storage});

  final GameApi? api;
  final AuthSessionStorage? storage;

  @override
  State<ChessCardApp> createState() => _ChessCardAppState();
}

class _ChessCardAppState extends State<ChessCardApp> {
  late final AuthController _auth;

  @override
  void initState() {
    super.initState();
    _auth = AuthController(
      api: widget.api ?? ApiClient(),
      storage: widget.storage ??
          SecureAuthSessionStorage(FlutterSecureKeyValueStore()),
    );
    _auth.addListener(_refresh);
    _auth.initialize();
  }

  void _refresh() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _auth.removeListener(_refresh);
    _auth.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.green),
      home: _auth.initialized
          ? HomePage(auth: _auth)
          : const Scaffold(
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(),
                    SizedBox(height: 12),
                    Text('正在恢复登录状态'),
                  ],
                ),
              ),
            ),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.auth});

  final AuthController auth;

  Future<void> _logout(BuildContext context) async {
    try {
      await auth.logout();
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('已退出本地账号；远端登出失败：$error')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: auth,
      builder: (context, _) => Scaffold(
        appBar: AppBar(title: const Text('升级')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: auth.isSignedIn
                      ? Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.account_circle),
                            const SizedBox(width: 12),
                            Text(auth.session!.displayName),
                            const SizedBox(width: 12),
                            TextButton.icon(
                              onPressed:
                                  auth.busy ? null : () => _logout(context),
                              icon: const Icon(Icons.logout),
                              label: const Text('退出登录'),
                            ),
                          ],
                        )
                      : Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Text('以访客身份游戏'),
                            const SizedBox(height: 8),
                            FilledButton.icon(
                              onPressed: auth.busy
                                  ? null
                                  : () => Navigator.of(context).push(
                                        MaterialPageRoute(
                                          builder: (_) =>
                                              AuthPage(controller: auth),
                                        ),
                                      ),
                              icon: const Icon(Icons.login),
                              label: const Text('登录 / 注册'),
                            ),
                          ],
                        ),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => GamePage(api: auth.api)),
                ),
                icon: const Icon(Icons.play_arrow),
                label: const Text('单人游戏'),
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => RoomPage(auth: auth)),
                ),
                icon: const Icon(Icons.meeting_room),
                label: const Text('进入房间'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
