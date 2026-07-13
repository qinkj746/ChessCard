import 'package:flutter/material.dart';

import 'app_error.dart';
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

  @override
  Widget build(BuildContext context) {
    final passwordFields = [
      TextFormField(
        key: const Key('auth_password'),
        controller: _password,
        obscureText: true,
        enabled: !_submitting,
        decoration: const InputDecoration(labelText: '密码'),
        validator: (value) {
          if (value == null || value.length < 6) return '密码至少需要 6 位';
          return null;
        },
      ),
    ];
    if (_registerMode) {
      passwordFields.add(
        TextFormField(
          key: const Key('auth_confirm_password'),
          controller: _confirmPassword,
          obscureText: true,
          enabled: !_submitting,
          decoration: const InputDecoration(labelText: '确认密码'),
          validator: (value) {
            if (value == null || value.isEmpty) return '请再次输入密码';
            return null;
          },
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('登录 / 注册')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SegmentedButton<bool>(
                      segments: const [
                        ButtonSegment(
                          value: false,
                          label: Text('登录'),
                        ),
                        ButtonSegment(
                          value: true,
                          label: Text('注册', key: Key('auth_mode_register')),
                        ),
                      ],
                      selected: {_registerMode},
                      onSelectionChanged: _submitting
                          ? null
                          : (selection) {
                              setState(() {
                                _registerMode = selection.single;
                                _error = null;
                              });
                            },
                    ),
                    const SizedBox(height: 24),
                    TextFormField(
                      key: const Key('auth_username'),
                      controller: _username,
                      enabled: !_submitting,
                      autocorrect: false,
                      decoration: const InputDecoration(labelText: '用户名'),
                      validator: (value) {
                        if (value == null || value.trim().isEmpty) {
                          return '请输入用户名';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    ...passwordFields.expand(
                      (field) => [field, const SizedBox(height: 16)],
                    ),
                    if (_error != null)
                      Semantics(
                        liveRegion: true,
                        container: true,
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 16),
                          child: Text(
                            _error!.message,
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                            ),
                          ),
                        ),
                      ),
                    FilledButton(
                      key: const Key('auth_submit'),
                      onPressed: _submitting ? null : _submit,
                      child: _submitting
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(_registerMode ? '注册并登录' : '登录'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
