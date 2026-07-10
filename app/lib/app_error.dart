import 'dart:io';

import 'package:http/http.dart' as http;

import 'api_client.dart';

class AppError {
  const AppError({
    required this.code,
    required this.message,
    required this.retryable,
  });

  final String code;
  final String message;
  final bool retryable;

  factory AppError.fromException(Object e) {
    if (e is GameApiException) {
      return AppError(code: e.code, message: e.message, retryable: false);
    }
    if (e is SocketException ||
        e is HttpException ||
        e is http.ClientException) {
      return const AppError(
        code: 'NETWORK_ERROR',
        message: 'Network connection failed. Please check your connection.',
        retryable: true,
      );
    }
    final msg = e.toString();
    return AppError(
      code: 'UNKNOWN',
      message: msg.startsWith('Exception: ') ? msg.substring(11) : msg,
      retryable: false,
    );
  }

  @override
  String toString() => message;
}
