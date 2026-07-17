import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:chess_card_app/game_page.dart';
import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/playing_card.dart';

void main() {
  testWidgets('kitty phase fits all banker cards on a wide table',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(1920, 930));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final game = GameStateModel(
      id: 'game-kitty-full-hand',
      phase: 'KITTY',
      levelRank: 'ACE',
      trumpSuit: 'HEART',
      banker: 'SOUTH',
      currentTurn: 'SOUTH',
      attackerScore: 0,
      winningTeam: null,
      levelDelta: 0,
      nextLevelRank: null,
      completed: false,
      handCounts: const {'SOUTH': 33, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
      southHand: sampleCards(33),
      kitty: const [],
      currentTrick: const {},
      declarationOptions: const [],
    );

    await tester.pumpWidget(MaterialApp(home: GamePage(initialGame: game)));
    await tester.pump();

    expect(find.byType(PlayingCard), findsNWidgets(33));
    final lastCard = find.byWidgetPredicate(
      (widget) => widget is PlayingCard && widget.card.deckIndex == 32,
    );
    expect(lastCard, findsOneWidget);
    expect(tester.getBottomRight(lastCard).dx, lessThanOrEqualTo(1920));
  });
}

List<CardModel> sampleCards(int count) {
  const suits = ['SPADE', 'HEART', 'CLUB', 'DIAMOND'];
  const ranks = [
    'THREE',
    'FOUR',
    'FIVE',
    'SIX',
    'SEVEN',
    'EIGHT',
    'NINE',
    'TEN',
    'JACK',
    'QUEEN',
    'KING',
    'ACE',
    'TWO',
  ];
  return List.generate(
    count,
    (index) => CardModel(
      suit: suits[index % suits.length],
      rank: ranks[index % ranks.length],
      deckIndex: index,
    ),
  );
}
