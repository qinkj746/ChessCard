import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/room_connection.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('connect subscribes to room topic and emits decoded events', () async {
    final transport = FakeRoomEventTransport();
    final connection = RoomConnection(
      baseUrl: 'http://example.test',
      roomId: 'room-1',
      transport: transport,
    );

    final eventFuture = connection.events.first;
    connection.connect();
    transport.emit(
        '''{"type":"GAME_UPDATED","roomId":"room-1","gameId":"game-1"}''');

    final event = await eventFuture;
    expect(transport.connectedRoomId, 'room-1');
    expect(transport.connectedUrl, 'ws://example.test/ws');
    expect(event.type, 'GAME_UPDATED');
    expect(event.gameId, 'game-1');
  });

  test('reconnect disconnects before connecting again', () {
    final transport = FakeRoomEventTransport();
    final connection = RoomConnection(
      baseUrl: 'https://example.test:8443',
      roomId: 'room-2',
      transport: transport,
    );

    connection.connect();
    connection.reconnect();

    expect(transport.disconnectCount, 1);
    expect(transport.connectCount, 2);
    expect(transport.connectedUrl, 'wss://example.test:8443/ws');
  });

  test('ignores stale versioned events', () async {
    final transport = FakeRoomEventTransport();
    final connection = RoomConnection(
      baseUrl: 'http://example.test',
      roomId: 'room-1',
      transport: transport,
    );

    final events = <RoomEventModel>[];
    final subscription = connection.events.listen(events.add);
    connection.connect();

    transport.emit('''{"type":"ROOM_UPDATED","roomId":"room-1","version":7}''');
    transport.emit('''{"type":"ROOM_UPDATED","roomId":"room-1","version":6}''');
    await Future<void>.delayed(Duration.zero);

    expect(events.map((event) => event.version), [7]);
    expect(connection.lastVersion, 7);
    await subscription.cancel();
  });
}

class FakeRoomEventTransport implements RoomEventTransport {
  String? connectedRoomId;
  String? connectedUrl;
  int connectCount = 0;
  int disconnectCount = 0;
  late void Function(String body) _onMessage;

  @override
  void connect({
    required String roomId,
    required String wsUrl,
    required void Function(String body) onMessage,
    required void Function(Object error) onError,
  }) {
    connectedRoomId = roomId;
    connectedUrl = wsUrl;
    connectCount++;
    _onMessage = onMessage;
  }

  void emit(String body) => _onMessage(body);

  @override
  void disconnect() {
    disconnectCount++;
  }
}
