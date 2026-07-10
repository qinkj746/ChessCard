import 'package:chess_card_app/game_page.dart';
import 'package:chess_card_app/models.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows completed trick history in play order', (tester) async {
    const game = GameStateModel(
      id: 'game-history',
      phase: 'PLAY',
      levelRank: 'ACE',
      trumpSuit: 'HEART',
      banker: 'SOUTH',
      currentTurn: 'SOUTH',
      attackerScore: 0,
      winningTeam: null,
      levelDelta: 0,
      nextLevelRank: null,
      completed: false,
      handCounts: {'SOUTH': 1, 'WEST': 1, 'NORTH': 1, 'EAST': 1},
      southHand: [CardModel(suit: 'CLUB', rank: 'THREE', deckIndex: 0)],
      kitty: [],
      currentTrick: {},
      playedTricks: [
        PlayedTrickModel(
          index: 1,
          leader: 'SOUTH',
          winner: 'SOUTH',
          points: 0,
          plays: [
            TrickPlayModel(
                seat: 'SOUTH',
                cards: [CardModel(suit: 'SPADE', rank: 'KING', deckIndex: 0)]),
            TrickPlayModel(
                seat: 'WEST',
                cards: [CardModel(suit: 'SPADE', rank: 'QUEEN', deckIndex: 0)]),
            TrickPlayModel(
                seat: 'NORTH',
                cards: [CardModel(suit: 'SPADE', rank: 'JACK', deckIndex: 0)]),
            TrickPlayModel(
                seat: 'EAST',
                cards: [CardModel(suit: 'SPADE', rank: 'TEN', deckIndex: 0)]),
          ],
        ),
      ],
      declarationOptions: [],
    );

    await tester
        .pumpWidget(const MaterialApp(home: GamePage(initialGame: game)));

    final texts = tester
        .widgetList<Text>(find.byType(Text))
        .map((text) => text.data ?? '')
        .toList();
    final south = texts.indexOf('\u5357: \u9ed1\u6843 K');
    final west = texts.indexOf('\u897f: \u9ed1\u6843 Q');
    final north = texts.indexOf('\u5317: \u9ed1\u6843 J');
    final east = texts.indexOf('\u4e1c: \u9ed1\u6843 10');

    expect(texts, contains('\u7b2c 1 \u58a9'));
    expect(south, greaterThanOrEqualTo(0));
    expect(west, greaterThan(south));
    expect(north, greaterThan(west));
    expect(east, greaterThan(north));
  });
}
