import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'models.dart';

abstract interface class RoomEventSource {
  Stream<RoomEventModel> get events;

  void connect();

  void reconnect();

  void disconnect();
}

abstract interface class RoomEventTransport {
  void connect({
    required String roomId,
    required String wsUrl,
    required void Function(String body) onMessage,
    required void Function(Object error) onError,
  });

  void disconnect();
}

class RoomConnection implements RoomEventSource {
  RoomConnection({
    required this.baseUrl,
    required this.roomId,
    RoomEventTransport? transport,
  }) : transport = transport ?? StompRoomEventTransport();

  final String baseUrl;
  final String roomId;
  final RoomEventTransport transport;
  final StreamController<RoomEventModel> _events =
      StreamController<RoomEventModel>.broadcast();
  int _lastVersion = 0;

  @override
  Stream<RoomEventModel> get events => _events.stream;

  int get lastVersion => _lastVersion;

  @override
  void connect() {
    transport.connect(
      roomId: roomId,
      wsUrl: _wsUrl(baseUrl),
      onMessage: _handleMessage,
      onError: _events.addError,
    );
  }

  @override
  void reconnect() {
    disconnect();
    connect();
  }

  @override
  void disconnect() {
    transport.disconnect();
  }

  void dispose() {
    disconnect();
    _events.close();
  }

  void _handleMessage(String body) {
    final decoded = jsonDecode(body) as Map<String, dynamic>;
    final event = RoomEventModel.fromJson(decoded);
    final version = event.version;
    if (version != null) {
      if (version <= _lastVersion) return;
      _lastVersion = version;
    }
    _events.add(event);
  }

  static String _wsUrl(String baseUrl) {
    final uri = Uri.parse(baseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
    return Uri(
      scheme: scheme,
      host: uri.host,
      port: uri.hasPort ? uri.port : null,
      path: '/ws',
    ).toString();
  }
}

class StompRoomEventTransport implements RoomEventTransport {
  StompClient? _client;

  @override
  void connect({
    required String roomId,
    required String wsUrl,
    required void Function(String body) onMessage,
    required void Function(Object error) onError,
  }) {
    late StompClient client;
    client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (StompFrame frame) {
          client.subscribe(
            destination: '/topic/rooms/$roomId',
            callback: (StompFrame frame) {
              final body = frame.body;
              if (body != null && body.isNotEmpty) {
                onMessage(body);
              }
            },
          );
        },
        onWebSocketError: (dynamic error) =>
            onError(error ?? 'WebSocket error'),
        onStompError: (frame) => onError(frame.body ?? frame.toString()),
      ),
    );
    _client = client;
    client.activate();
  }

  @override
  void disconnect() {
    _client?.deactivate();
    _client = null;
  }
}
