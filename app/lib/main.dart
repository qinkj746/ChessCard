import 'dart:async';

import 'package:flutter/material.dart';

import 'api_client.dart';
import 'app_theme.dart';
import 'auth_controller.dart';
import 'auth_page.dart';
import 'auth_session_storage.dart';
import 'game_page.dart';
import 'models.dart';
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
      theme: AppTheme.theme(),
      home: _auth.initialized
          ? HomePage(auth: _auth)
          : const Scaffold(
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(),
                    SizedBox(height: 12),
                    Text('正在恢复登录状态...'),
                  ],
                ),
              ),
            ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.auth});

  final AuthController auth;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final AuthController auth = widget.auth;
  Timer? _presenceTimer;
  List<OnlinePlayerModel> _onlinePlayers = const [];
  bool _loadingPresence = true;
  String? _presenceError;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _refreshPresence());
    _presenceTimer = Timer.periodic(
      const Duration(seconds: 30),
      (_) => _refreshPresence(),
    );
  }

  @override
  void dispose() {
    _presenceTimer?.cancel();
    super.dispose();
  }

  Future<void> _refreshPresence() async {
    if (!mounted) return;
    try {
      final playerId = await auth.ensurePlayerId();
      await auth.api.sendPresenceHeartbeat(playerId);
      final onlinePlayers = await auth.api.fetchOnlinePlayers();
      if (!mounted) return;
      setState(() {
        _onlinePlayers = onlinePlayers;
        _presenceError = null;
        _loadingPresence = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _presenceError = error.toString();
        _loadingPresence = false;
      });
    }
  }

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
        backgroundColor: AppTheme.night,
        appBar: AppBar(
          title: const Text('升级'),
          backgroundColor: AppTheme.night,
          foregroundColor: Colors.white,
          surfaceTintColor: Colors.transparent,
        ),
        body: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final horizontalPadding =
                  constraints.maxWidth < 520 ? 16.0 : 24.0;
              return SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(
                  horizontalPadding,
                  16,
                  horizontalPadding,
                  24,
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 720),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _HomeAccountPanel(
                          auth: auth,
                          onLogin: auth.busy
                              ? null
                              : () => Navigator.of(context).push(
                                    MaterialPageRoute(
                                      builder: (_) =>
                                          AuthPage(controller: auth),
                                    ),
                                  ),
                          onLogout: auth.busy ? null : () => _logout(context),
                        ),
                        const SizedBox(height: 16),
                        const _HomeHeroTable(),
                        const SizedBox(height: 14),
                        LayoutBuilder(
                          builder: (context, modeConstraints) {
                            final narrow = modeConstraints.maxWidth < 560;
                            final cards = [
                              _HomeModeCard(
                                key: const Key('home_practice_card'),
                                icon: Icons.play_arrow,
                                title: '单人练习',
                                subtitle: '快速开局 · AI 陪打',
                                primary: true,
                                onTap: () => Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => GamePage(api: auth.api),
                                  ),
                                ),
                              ),
                              _HomeModeCard(
                                key: const Key('home_room_card'),
                                icon: Icons.meeting_room,
                                title: '朋友房间',
                                subtitle: '邀请好友 · 实时聊天',
                                onTap: () => Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => RoomPage(auth: auth),
                                  ),
                                ),
                              ),
                            ];
                            if (narrow) {
                              return Column(
                                children: [
                                  cards[0],
                                  const SizedBox(height: 10),
                                  cards[1],
                                ],
                              );
                            }
                            return Row(
                              children: [
                                Expanded(child: cards[0]),
                                const SizedBox(width: 10),
                                Expanded(child: cards[1]),
                              ],
                            );
                          },
                        ),
                        const SizedBox(height: 12),
                        _HomeStats(
                          onlinePlayers: _onlinePlayers,
                          loading: _loadingPresence,
                          error: _presenceError,
                          onRefresh: () => _refreshPresence(),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _HomeAccountPanel extends StatelessWidget {
  const _HomeAccountPanel({
    required this.auth,
    required this.onLogin,
    required this.onLogout,
  });

  final AuthController auth;
  final VoidCallback? onLogin;
  final VoidCallback? onLogout;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: AppTheme.nightSurface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.10)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            const Icon(Icons.account_circle, color: AppTheme.goldSoft),
            const SizedBox(width: 10),
            Expanded(
              child: auth.isSignedIn
                  ? Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          auth.session!.displayName,
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        const Text(
                          '已登录账号',
                          style: TextStyle(color: AppTheme.muted, fontSize: 12),
                        ),
                      ],
                    )
                  : const Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '以访客身份游戏',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          '登录后可保留好友和房间身份',
                          style: TextStyle(color: AppTheme.muted, fontSize: 12),
                        ),
                      ],
                    ),
            ),
            if (auth.isSignedIn)
              TextButton.icon(
                onPressed: onLogout,
                icon: const Icon(Icons.logout),
                label: const Text('退出登录'),
              )
            else
              FilledButton.icon(
                onPressed: onLogin,
                icon: const Icon(Icons.login),
                label: const Text('登录 / 注册'),
              ),
          ],
        ),
      ),
    );
  }
}

class _HomeHeroTable extends StatelessWidget {
  const _HomeHeroTable();

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 236),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppTheme.feltSoft, AppTheme.feltDeep],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFF7A4C27), width: 5),
        boxShadow: const [
          BoxShadow(
            blurRadius: 24,
            offset: Offset(0, 14),
            color: Color(0x66000000),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(120),
                  border: Border.all(
                    color: AppTheme.goldSoft.withValues(alpha: 0.16),
                  ),
                ),
              ),
            ),
          ),
          const Align(
            alignment: Alignment.topLeft,
            child: SizedBox(
              width: 250,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '今日牌局',
                    style: TextStyle(
                      color: AppTheme.goldSoft,
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  SizedBox(height: 6),
                  Text(
                    '开一桌，打到 K',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 28,
                      height: 1.08,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  SizedBox(height: 8),
                  Text(
                    '单人练习或和朋友约局，从这里开始。',
                    style: TextStyle(color: Color(0xFFD5E3DC)),
                  ),
                ],
              ),
            ),
          ),
          const Positioned(
            right: 10,
            bottom: 6,
            child: _CardFan(),
          ),
        ],
      ),
    );
  }
}

class _CardFan extends StatelessWidget {
  const _CardFan();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: 136,
      height: 112,
      child: Stack(
        alignment: Alignment.bottomRight,
        children: [
          _MiniHomeCard(label: 'K', black: true, angle: -0.22, left: 0),
          _MiniHomeCard(label: 'A', angle: -0.06, left: 38, lift: 10),
          _MiniHomeCard(
              label: '2', black: true, angle: 0.14, left: 76, lift: 4),
        ],
      ),
    );
  }
}

class _MiniHomeCard extends StatelessWidget {
  const _MiniHomeCard({
    required this.label,
    this.black = false,
    required this.angle,
    required this.left,
    this.lift = 0,
  });

  final String label;
  final bool black;
  final double angle;
  final double left;
  final double lift;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: left,
      bottom: lift,
      child: Transform.rotate(
        angle: angle,
        child: Container(
          width: 52,
          height: 76,
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: AppTheme.paper,
            borderRadius: BorderRadius.circular(7),
            border: Border.all(color: const Color(0x261A1710)),
            boxShadow: const [
              BoxShadow(
                blurRadius: 14,
                offset: Offset(0, 8),
                color: Color(0x66000000),
              ),
            ],
          ),
          child: Text(
            label,
            style: TextStyle(
              color: black ? AppTheme.ink : const Color(0xFFB91C1C),
              fontWeight: FontWeight.w900,
              fontSize: 18,
            ),
          ),
        ),
      ),
    );
  }
}

class _HomeModeCard extends StatelessWidget {
  const _HomeModeCard({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.primary = false,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    final foreground = primary ? AppTheme.ink : Colors.white;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Ink(
          height: 124,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            gradient: primary
                ? const LinearGradient(
                    colors: [AppTheme.gold, AppTheme.goldSoft],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  )
                : null,
            color: primary ? null : AppTheme.nightSurfaceAlt,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: primary
                  ? Colors.white.withValues(alpha: 0.26)
                  : Colors.white.withValues(alpha: 0.11),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Icon(icon, color: foreground),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      color: foreground,
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      color: primary ? const Color(0xBF211506) : AppTheme.muted,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeStats extends StatelessWidget {
  const _HomeStats({
    required this.onlinePlayers,
    required this.loading,
    required this.error,
    required this.onRefresh,
  });

  final List<OnlinePlayerModel> onlinePlayers;
  final bool loading;
  final String? error;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('home_online_players'),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTheme.nightSurface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.09)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: _HomeStat(
                  value: loading ? '...' : onlinePlayers.length.toString(),
                  label: '在线用户',
                ),
              ),
              IconButton(
                tooltip: '刷新',
                onPressed: loading ? null : onRefresh,
                icon: const Icon(Icons.refresh, color: AppTheme.goldSoft),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (error != null)
            const Text(
              '在线状态暂不可用',
              style: TextStyle(color: AppTheme.muted, fontSize: 12),
            )
          else if (loading)
            const Text(
              '正在同步在线状态...',
              style: TextStyle(color: AppTheme.muted, fontSize: 12),
            )
          else if (onlinePlayers.isEmpty)
            const Text(
              '暂无在线用户',
              style: TextStyle(color: AppTheme.muted, fontSize: 12),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final player in onlinePlayers)
                  _OnlinePlayerChip(
                    key: Key('home_online_player_${player.playerId}'),
                    player: player,
                  ),
              ],
            ),
        ],
      ),
    );
  }
}

class _HomeStat extends StatelessWidget {
  const _HomeStat({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTheme.nightSurface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.09)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.w900,
            ),
          ),
          Text(
            label,
            style: const TextStyle(color: AppTheme.muted, fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _OnlinePlayerChip extends StatelessWidget {
  const _OnlinePlayerChip({super.key, required this.player});

  final OnlinePlayerModel player;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(maxWidth: 180, minHeight: 34),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: AppTheme.nightSurfaceAlt,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.10)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            player.guest ? Icons.person_outline : Icons.account_circle,
            size: 16,
            color: AppTheme.goldSoft,
          ),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              player.displayName,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
