import 'package:chess_card_app/models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses room event with version and payload', () {
    final event = RoomEventModel.fromJson({
      'type': 'GAME_UPDATED',
      'roomId': 'room-1',
      'gameId': 'game-1',
      'version': 7,
      'payload': {'source': 'test'},
      'occurredAt': '2026-07-09T10:00:00Z',
    });

    expect(event.type, 'GAME_UPDATED');
    expect(event.roomId, 'room-1');
    expect(event.gameId, 'game-1');
    expect(event.version, 7);
    expect(event.payload, {'source': 'test'});
  });

  test('parses room state version', () {
    final room = RoomStateModel.fromJson({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'player-1',
      'gameId': null,
      'version': 3,
      'seats': {
        'SOUTH': {'playerId': 'player-1'},
      },
    });

    expect(room.version, 3);
  });
}
