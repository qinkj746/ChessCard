import 'package:flutter/material.dart';

import 'app_error.dart';
import 'app_theme.dart';
import 'auth_controller.dart';

class AuthPage extends StatefulWidget {
  const AuthPage({super.key, required this.controller});

  final AuthController controller;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _formKey = GlobalKey<FormState>();
  final _username = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();

  bool _registerMode = false;
  bool _submitting = false;
  bool _showPassword = false;
  bool _showConfirmPassword = false;
  AppError? _error;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_registerMode && _password.text != _confirmPassword.text) {
      setState(() {
        _error = const AppError(
          code: 'PASSWORD_MISMATCH',
          message: '两次输入的密码不一致',
          retryable: false,
        );
      });
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      if (_registerMode) {
        await widget.controller.register(
          username: _username.text.trim(),
          password: _password.text,
        );
      } else {
        await widget.controller.login(
          username: _username.text.trim(),
          password: _password.text,
        );
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (error) {
      if (mounted) setState(() => _error = AppError.fromException(error));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _setMode(bool registerMode) {
    setState(() {
      _registerMode = registerMode;
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.night,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final compact = constraints.maxWidth < 760;
            final horizontalPadding = constraints.maxWidth < 520 ? 16.0 : 24.0;
            final availableHeight = constraints.maxHeight - 32;

            return SingleChildScrollView(
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                16,
                horizontalPadding,
                24,
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: availableHeight),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 900),
                    child: compact
                        ? Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              _AuthHeroPanel(registerMode: _registerMode),
                              const SizedBox(height: 16),
                              _AuthFormPanel(
                                formKey: _formKey,
                                username: _username,
                                password: _password,
                                confirmPassword: _confirmPassword,
                                registerMode: _registerMode,
                                submitting: _submitting,
                                showPassword: _showPassword,
                                showConfirmPassword: _showConfirmPassword,
                                error: _error,
                                onModeChanged: _setMode,
                                onSubmit: _submit,
                                onTogglePassword: () => setState(
                                  () => _showPassword = !_showPassword,
                                ),
                                onToggleConfirmPassword: () => setState(
                                  () => _showConfirmPassword =
                                      !_showConfirmPassword,
                                ),
                              ),
                            ],
                          )
                        : Row(
                            children: [
                              const Expanded(
                                child: _AuthHeroPanel(registerMode: false),
                              ),
                              const SizedBox(width: 18),
                              Expanded(
                                child: _AuthFormPanel(
                                  formKey: _formKey,
                                  username: _username,
                                  password: _password,
                                  confirmPassword: _confirmPassword,
                                  registerMode: _registerMode,
                                  submitting: _submitting,
                                  showPassword: _showPassword,
                                  showConfirmPassword: _showConfirmPassword,
                                  error: _error,
                                  onModeChanged: _setMode,
                                  onSubmit: _submit,
                                  onTogglePassword: () => setState(
                                    () => _showPassword = !_showPassword,
                                  ),
                                  onToggleConfirmPassword: () => setState(
                                    () => _showConfirmPassword =
                                        !_showConfirmPassword,
                                  ),
                                ),
                              ),
                            ],
                          ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _AuthHeroPanel extends StatelessWidget {
  const _AuthHeroPanel({required this.registerMode});

  final bool registerMode;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 360),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppTheme.feltSoft, AppTheme.feltDeep, AppTheme.night],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFF7A4C27), width: 4),
        boxShadow: const [
          BoxShadow(
            blurRadius: 28,
            offset: Offset(0, 18),
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
                  borderRadius: BorderRadius.circular(180),
                  border: Border.all(
                    color: AppTheme.goldSoft.withValues(alpha: 0.16),
                  ),
                ),
              ),
            ),
          ),
          Positioned(
            right: 4,
            bottom: 4,
            child: IgnorePointer(
              child: Opacity(
                opacity: 0.95,
                child: _AuthCardFan(
                  compact: MediaQuery.sizeOf(context).width < 420,
                ),
              ),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.10),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.16),
                      ),
                    ),
                    child: const Icon(Icons.style, color: AppTheme.goldSoft),
                  ),
                  const SizedBox(width: 10),
                  const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '升级',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      Text(
                        'ChessCard',
                        style: TextStyle(color: Color(0xFFD5E3DC)),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 112),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 310),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      registerMode ? '创建牌桌账号' : '进入今晚牌桌',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 34,
                        height: 1.08,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      '登录后保留好友、房间身份和牌局记录；也可以继续以访客身份游戏。',
                      style: TextStyle(
                        color: Color(0xFFD5E3DC),
                        fontSize: 15,
                        height: 1.45,
                      ),
                    ),
                    const SizedBox(height: 18),
                    const Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _AuthBenefitChip(
                          icon: Icons.groups_2,
                          label: '好友房间',
                        ),
                        _AuthBenefitChip(
                          icon: Icons.shield_outlined,
                          label: '身份保留',
                        ),
                        _AuthBenefitChip(
                          icon: Icons.auto_graph,
                          label: '记录同步',
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AuthBenefitChip extends StatelessWidget {
  const _AuthBenefitChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 34),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.14)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: AppTheme.goldSoft),
          const SizedBox(width: 6),
          Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 12,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _AuthFormPanel extends StatelessWidget {
  const _AuthFormPanel({
    required this.formKey,
    required this.username,
    required this.password,
    required this.confirmPassword,
    required this.registerMode,
    required this.submitting,
    required this.showPassword,
    required this.showConfirmPassword,
    required this.error,
    required this.onModeChanged,
    required this.onSubmit,
    required this.onTogglePassword,
    required this.onToggleConfirmPassword,
  });

  final GlobalKey<FormState> formKey;
  final TextEditingController username;
  final TextEditingController password;
  final TextEditingController confirmPassword;
  final bool registerMode;
  final bool submitting;
  final bool showPassword;
  final bool showConfirmPassword;
  final AppError? error;
  final ValueChanged<bool> onModeChanged;
  final VoidCallback onSubmit;
  final VoidCallback onTogglePassword;
  final VoidCallback onToggleConfirmPassword;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.nightSurface.withValues(alpha: 0.96),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.10)),
        boxShadow: const [
          BoxShadow(
            blurRadius: 24,
            offset: Offset(0, 16),
            color: Color(0x52000000),
          ),
        ],
      ),
      child: Form(
        key: formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: '返回',
                  onPressed: submitting
                      ? null
                      : () => Navigator.of(context).maybePop(),
                  icon: const Icon(Icons.arrow_back),
                  color: Colors.white,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        registerMode ? '创建牌桌账号' : '账号登录',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 3),
                      const Text(
                        '你的牌桌身份，从这里开始。',
                        style: TextStyle(color: AppTheme.muted, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            SegmentedButton<bool>(
              segments: const [
                ButtonSegment(
                  value: false,
                  icon: Icon(Icons.login),
                  label: Text('登录'),
                ),
                ButtonSegment(
                  value: true,
                  icon: Icon(Icons.person_add_alt_1),
                  label: Text('注册', key: Key('auth_mode_register')),
                ),
              ],
              selected: {registerMode},
              style: ButtonStyle(
                backgroundColor: WidgetStateProperty.resolveWith((states) {
                  if (states.contains(WidgetState.selected)) {
                    return AppTheme.gold;
                  }
                  return AppTheme.nightSurfaceAlt;
                }),
                foregroundColor: WidgetStateProperty.resolveWith((states) {
                  if (states.contains(WidgetState.selected)) {
                    return AppTheme.ink;
                  }
                  return Colors.white;
                }),
                side: WidgetStateProperty.all(
                  BorderSide(color: Colors.white.withValues(alpha: 0.12)),
                ),
              ),
              onSelectionChanged: submitting
                  ? null
                  : (selection) => onModeChanged(selection.single),
            ),
            const SizedBox(height: 18),
            TextFormField(
              key: const Key('auth_username'),
              controller: username,
              enabled: !submitting,
              autocorrect: false,
              textInputAction: TextInputAction.next,
              style: const TextStyle(color: Colors.white),
              decoration: _inputDecoration(
                label: '用户名',
                hint: '输入你的账号名',
                icon: Icons.account_circle_outlined,
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '请输入用户名';
                }
                return null;
              },
            ),
            const SizedBox(height: 14),
            TextFormField(
              key: const Key('auth_password'),
              controller: password,
              obscureText: !showPassword,
              enabled: !submitting,
              textInputAction:
                  registerMode ? TextInputAction.next : TextInputAction.done,
              style: const TextStyle(color: Colors.white),
              decoration: _inputDecoration(
                label: '密码',
                hint: '至少 6 位',
                icon: Icons.lock_outline,
                suffix: IconButton(
                  tooltip: showPassword ? '隐藏密码' : '显示密码',
                  onPressed: submitting ? null : onTogglePassword,
                  icon: Icon(
                    showPassword ? Icons.visibility_off : Icons.visibility,
                  ),
                ),
              ),
              validator: (value) {
                if (value == null || value.length < 6) {
                  return '密码至少需要 6 位';
                }
                return null;
              },
              onFieldSubmitted: (_) {
                if (!registerMode && !submitting) onSubmit();
              },
            ),
            if (registerMode) ...[
              const SizedBox(height: 14),
              TextFormField(
                key: const Key('auth_confirm_password'),
                controller: confirmPassword,
                obscureText: !showConfirmPassword,
                enabled: !submitting,
                textInputAction: TextInputAction.done,
                style: const TextStyle(color: Colors.white),
                decoration: _inputDecoration(
                  label: '确认密码',
                  hint: '再输入一次密码',
                  icon: Icons.verified_user_outlined,
                  suffix: IconButton(
                    tooltip: showConfirmPassword ? '隐藏确认密码' : '显示确认密码',
                    onPressed: submitting ? null : onToggleConfirmPassword,
                    icon: Icon(
                      showConfirmPassword
                          ? Icons.visibility_off
                          : Icons.visibility,
                    ),
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) return '请再次输入密码';
                  return null;
                },
                onFieldSubmitted: (_) {
                  if (!submitting) onSubmit();
                },
              ),
            ],
            if (error != null) ...[
              const SizedBox(height: 14),
              Semantics(
                liveRegion: true,
                container: true,
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: colorScheme.errorContainer.withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: colorScheme.error.withValues(alpha: 0.38),
                    ),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        Icons.error_outline,
                        color: colorScheme.error,
                        size: 18,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          error!.message,
                          style: TextStyle(
                            color: colorScheme.error,
                            height: 1.35,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
            const SizedBox(height: 18),
            FilledButton.icon(
              key: const Key('auth_submit'),
              onPressed: submitting ? null : onSubmit,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(50),
                backgroundColor: AppTheme.gold,
                foregroundColor: AppTheme.ink,
                disabledBackgroundColor: AppTheme.gold.withValues(alpha: 0.38),
                disabledForegroundColor: AppTheme.ink.withValues(alpha: 0.54),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              icon: submitting
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Icon(
                      registerMode ? Icons.person_add_alt_1 : Icons.event_seat),
              label: Text(registerMode ? '注册并登录' : '登录并入座'),
            ),
            const SizedBox(height: 12),
            const Row(
              children: [
                Icon(Icons.shield_outlined, color: AppTheme.goldSoft, size: 16),
                SizedBox(width: 7),
                Expanded(
                  child: Text(
                    '账号只用于保留你的游戏身份，不影响访客模式。',
                    style: TextStyle(color: AppTheme.muted, fontSize: 12),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  InputDecoration _inputDecoration({
    required String label,
    required String hint,
    required IconData icon,
    Widget? suffix,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      prefixIcon: Icon(icon),
      suffixIcon: suffix,
      filled: true,
      fillColor: AppTheme.nightSurfaceAlt,
      labelStyle: const TextStyle(color: AppTheme.goldSoft),
      hintStyle: const TextStyle(color: AppTheme.muted),
      prefixIconColor: AppTheme.goldSoft,
      suffixIconColor: AppTheme.muted,
      errorMaxLines: 2,
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.10)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: AppTheme.goldSoft, width: 1.4),
      ),
      disabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.06)),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: Colors.redAccent),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: Colors.redAccent, width: 1.4),
      ),
    );
  }
}

class _AuthCardFan extends StatelessWidget {
  const _AuthCardFan({required this.compact});

  final bool compact;

  @override
  Widget build(BuildContext context) {
    final scale = compact ? 0.82 : 1.0;
    return SizedBox(
      width: 150 * scale,
      height: 126 * scale,
      child: Stack(
        alignment: Alignment.bottomRight,
        children: [
          _AuthMiniCard(
            label: 'K',
            black: true,
            angle: -0.22,
            left: 0,
            bottom: 4 * scale,
            scale: scale,
          ),
          _AuthMiniCard(
            label: 'A',
            angle: -0.05,
            left: 42 * scale,
            bottom: 18 * scale,
            scale: scale,
          ),
          _AuthMiniCard(
            label: '2',
            black: true,
            angle: 0.16,
            left: 84 * scale,
            bottom: 7 * scale,
            scale: scale,
          ),
        ],
      ),
    );
  }
}

class _AuthMiniCard extends StatelessWidget {
  const _AuthMiniCard({
    required this.label,
    required this.angle,
    required this.left,
    required this.bottom,
    required this.scale,
    this.black = false,
  });

  final String label;
  final double angle;
  final double left;
  final double bottom;
  final double scale;
  final bool black;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: left,
      bottom: bottom,
      child: Transform.rotate(
        angle: angle,
        child: Container(
          width: 58 * scale,
          height: 84 * scale,
          padding: EdgeInsets.all(8 * scale),
          decoration: BoxDecoration(
            color: AppTheme.paper,
            borderRadius: BorderRadius.circular(7),
            border: Border.all(color: const Color(0x261A1710)),
            boxShadow: const [
              BoxShadow(
                blurRadius: 16,
                offset: Offset(0, 10),
                color: Color(0x66000000),
              ),
            ],
          ),
          child: Text(
            label,
            style: TextStyle(
              color: black ? AppTheme.ink : const Color(0xFFB91C1C),
              fontWeight: FontWeight.w900,
              fontSize: 20 * scale,
            ),
          ),
        ),
      ),
    );
  }
}
