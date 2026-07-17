import 'package:flutter_test/flutter_test.dart';

import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/record_models.dart';

void main() {
  test('parses current trick by seat', () {
    final game = GameStateModel.fromJson({
      'id': 'game-1',
      'phase': 'PLAY',
      'levelRank': 'ACE',
      'trumpSuit': 'HEART',
      'banker': 'SOUTH',
      'currentTurn': 'WEST',
      'attackerScore': 0,
      'winningTeam': null,
      'levelDelta': 0,
      'nextLevelRank': null,
      'completed': false,
      'handCounts': {'SOUTH': 24, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
      'southHand': <Map<String, Object?>>[],
      'kitty': <Map<String, Object?>>[],
      'currentTrick': {
        'SOUTH': [
          {'suit': 'SPADE', 'rank': 'FIVE', 'deckIndex': 0},
        ],
      },
      'declarationOptions': <String>[],
    });

    expect(game.currentTrick['SOUTH'], hasLength(1));
    expect(game.currentTrick['SOUTH']!.single.rank, 'FIVE');
  });

  test('parses finished game result', () {
    final game = GameStateModel.fromJson({
      'id': 'game-2',
      'phase': 'FINISHED',
      'levelRank': 'ACE',
      'trumpSuit': 'HEART',
      'banker': 'SOUTH',
      'currentTurn': 'WEST',
      'attackerScore': 110,
      'winningTeam': 'EAST_WEST',
      'levelDelta': 1,
      'nextLevelRank': 'TWO',
      'completed': true,
      'handCounts': {'SOUTH': 0, 'WEST': 0, 'NORTH': 0, 'EAST': 0},
      'southHand': <Map<String, Object?>>[],
      'kitty': <Map<String, Object?>>[],
      'currentTrick': <String, List<Map<String, Object?>>>{},
      'declarationOptions': <String>[],
    });

    expect(game.winningTeam, 'EAST_WEST');
    expect(game.levelDelta, 1);
    expect(game.nextLevelRank, 'TWO');
    expect(game.completed, isTrue);
  });

  test('parses game record summary', () {
    final record = GameRecordModel.fromJson({
      'recordId': 'record-1',
      'gameId': 'game-1',
      'roomId': 'room-1',
      'startedAt': '2026-07-10T08:00:00Z',
      'finishedAt': '2026-07-10T08:30:00Z',
      'players': {'SOUTH': 'player-south'},
      'winningTeam': 'SOUTH_NORTH',
      'attackerScore': 70,
      'levelDelta': 1,
      'nextLevelRank': 'TWO',
      'completed': false,
    });

    expect(record.recordId, 'record-1');
    expect(record.players['SOUTH'], 'player-south');
    expect(record.finishedAt, DateTime.parse('2026-07-10T08:30:00Z'));
    expect(record.winningTeam, 'SOUTH_NORTH');
  });

  test('parses online player summary', () {
    final player = OnlinePlayerModel.fromJson({
      'playerId': 'player-1',
      'displayName': 'Alice',
      'guest': false,
      'lastSeenAt': '2026-07-10T08:00:00Z',
    });

    expect(player.playerId, 'player-1');
    expect(player.displayName, 'Alice');
    expect(player.guest, isFalse);
    expect(player.lastSeenAt, DateTime.parse('2026-07-10T08:00:00Z'));
  });

  test('parses ordered current and completed trick plays', () {
    final game = GameStateModel.fromJson({
      'id': 'game-history',
      'phase': 'PLAY',
      'levelRank': 'ACE',
      'trumpSuit': 'HEART',
      'banker': 'SOUTH',
      'currentTurn': 'SOUTH',
      'attackerScore': 0,
      'winningTeam': null,
      'levelDelta': 0,
      'nextLevelRank': null,
      'completed': false,
      'handCounts': {'SOUTH': 1, 'WEST': 1, 'NORTH': 1, 'EAST': 1},
      'southHand': <Map<String, Object?>>[],
      'kitty': <Map<String, Object?>>[],
      'currentTrick': <String, List<Map<String, Object?>>>{},
      'currentTrickPlays': [
        {
          'seat': 'SOUTH',
          'cards': [
            {'suit': 'SPADE', 'rank': 'KING', 'deckIndex': 0},
          ],
        },
        {
          'seat': 'WEST',
          'cards': [
            {'suit': 'SPADE', 'rank': 'QUEEN', 'deckIndex': 0},
          ],
        },
      ],
      'playedTricks': [
        {
          'index': 1,
          'leader': 'SOUTH',
          'winner': 'SOUTH',
          'points': 0,
          'plays': [
            {
              'seat': 'SOUTH',
              'cards': [
                {'suit': 'SPADE', 'rank': 'KING', 'deckIndex': 0},
              ],
            },
            {
              'seat': 'WEST',
              'cards': [
                {'suit': 'SPADE', 'rank': 'QUEEN', 'deckIndex': 0},
              ],
            },
            {
              'seat': 'NORTH',
              'cards': [
                {'suit': 'SPADE', 'rank': 'JACK', 'deckIndex': 0},
              ],
            },
            {
              'seat': 'EAST',
              'cards': [
                {'suit': 'SPADE', 'rank': 'TEN', 'deckIndex': 0},
              ],
            },
          ],
        },
      ],
      'declarationOptions': <String>[],
    });

    expect(game.currentTrickPlays.map((play) => play.seat), ['SOUTH', 'WEST']);
    expect(game.playedTricks.single.plays.map((play) => play.seat), [
      'SOUTH',
      'WEST',
      'NORTH',
      'EAST',
    ]);
    expect(game.playedTricks.single.winner, 'SOUTH');
  });
}
