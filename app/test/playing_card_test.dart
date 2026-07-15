import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/playing_card.dart';

void main() {
  Widget cardHost(CardModel card, {bool selected = false}) {
    return MaterialApp(
      home: Scaffold(
        body: PlayingCard(
          key: const Key('card'),
          card: card,
          width: 58,
          height: 110,
          selected: selected,
        ),
      ),
    );
  }

  testWidgets('renders standard rank and suit symbols', (tester) async {
    const cases = [
      (CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0), '5', '♠'),
      (CardModel(suit: 'HEART', rank: 'TEN', deckIndex: 0), '10', '♥'),
      (CardModel(suit: 'CLUB', rank: 'KING', deckIndex: 0), 'K', '♣'),
      (CardModel(suit: 'DIAMOND', rank: 'ACE', deckIndex: 0), 'A', '♦'),
    ];

    for (final (card, rank, suit) in cases) {
      await tester.pumpWidget(cardHost(card));
      expect(find.text(rank), findsOneWidget);
      expect(find.text(suit), findsNWidgets(2));
    }
  });

  testWidgets('uses red for hearts and black for spades', (tester) async {
    await tester.pumpWidget(cardHost(
      const CardModel(suit: 'HEART', rank: 'FIVE', deckIndex: 0),
    ));
    final heart = tester.widgetList<Text>(find.text('♥')).first;
    expect(heart.style?.color, const Color(0xFFC62828));

    await tester.pumpWidget(cardHost(
      const CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0),
    ));
    final spade = tester.widgetList<Text>(find.text('♠')).first;
    expect(spade.style?.color, const Color(0xFF171A18));
  });

  testWidgets('renders dedicated joker faces', (tester) async {
    final semantics = tester.ensureSemantics();
    try {
      await tester.pumpWidget(cardHost(
        const CardModel(suit: null, rank: 'SMALL_JOKER', deckIndex: 0),
      ));
      expect(find.text('小王'), findsNothing);
      expect(find.text('王'), findsNothing);
      expect(find.text('J'), findsOneWidget);
      expect(find.byKey(const Key('joker-art-small')), findsOneWidget);
      expect(find.bySemanticsLabel('小王'), findsOneWidget);

      await tester.pumpWidget(cardHost(
        const CardModel(suit: null, rank: 'BIG_JOKER', deckIndex: 0),
      ));
      expect(find.text('大王'), findsNothing);
      expect(find.text('王'), findsNothing);
      expect(find.text('J'), findsOneWidget);
      expect(find.byKey(const Key('joker-art-big')), findsOneWidget);
      expect(find.byKey(const Key('joker-flare-big')), findsOneWidget);
      expect(find.bySemanticsLabel('大王'), findsOneWidget);
    } finally {
      semantics.dispose();
    }
  });

  testWidgets('selected card uses gold border', (tester) async {
    await tester.pumpWidget(cardHost(
      const CardModel(suit: 'CLUB', rank: 'ACE', deckIndex: 0),
      selected: true,
    ));

    final container = tester.widget<AnimatedContainer>(find.descendant(
      of: find.byKey(const Key('card')),
      matching: find.byType(AnimatedContainer),
    ));
    final decoration = container.decoration! as BoxDecoration;
    final border = decoration.border! as Border;
    expect(border.top.color, const Color(0xFFE0A928));
    expect(border.top.width, 2);
  });
}
