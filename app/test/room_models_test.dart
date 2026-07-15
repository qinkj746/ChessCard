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

  test('parses room seat display name', () {
    final room = RoomStateModel.fromJson({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'player-1',
      'gameId': null,
      'version': 3,
      'seats': {
        'SOUTH': {
          'playerId': 'player-1',
          'displayName': 'Alice',
          'isBot': false,
        },
      },
    });

    expect(room.seats['SOUTH']!.displayName, 'Alice');
    expect(room.seats['SOUTH']!.playerId, 'player-1');
    expect(room.seats['SOUTH']!.isBot, isFalse);
  });

  test('parses bot room seat with fixed display name', () {
    final room = RoomStateModel.fromJson({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'player-1',
      'gameId': null,
      'version': 3,
      'seats': {
        'WEST': {
          'playerId': null,
          'displayName': 'Ignored bot name',
          'isBot': true,
        },
      },
    });

    expect(room.seats['WEST']!.playerId, isNull);
    expect(room.seats['WEST']!.isBot, isTrue);
    expect(room.seats['WEST']!.displayName, '人机');
  });

  test('room seat falls back to player id when display name is missing', () {
    final room = RoomStateModel.fromJson({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'legacy-player',
      'gameId': null,
      'version': 3,
      'seats': {
        'SOUTH': {'playerId': 'legacy-player'},
      },
    });

    expect(room.seats['SOUTH']!.displayName, 'legacy-player');
    expect(room.seats['SOUTH']!.isBot, isFalse);
  });
}
