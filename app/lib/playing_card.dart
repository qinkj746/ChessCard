import 'package:flutter/material.dart';

import 'models.dart';

class PlayingCard extends StatelessWidget {
  const PlayingCard({
    super.key,
    required this.card,
    required this.width,
    required this.height,
    this.selected = false,
  });

  static const red = Color(0xFFC62828);
  static const black = Color(0xFF171A18);
  static const gold = Color(0xFFE0A928);

  final CardModel card;
  final double width;
  final double height;
  final bool selected;

  bool get _isJoker =>
      card.rank == 'SMALL_JOKER' || card.rank == 'BIG_JOKER';

  Color get _ink => card.suit == 'HEART' ||
          card.suit == 'DIAMOND' ||
          card.rank == 'BIG_JOKER'
      ? red
      : black;

  String get _suit => switch (card.suit) {
        'SPADE' => '♠',
        'HEART' => '♥',
        'CLUB' => '♣',
        'DIAMOND' => '♦',
        _ => '',
      };

  String get _rank => switch (card.rank) {
        'THREE' => '3',
        'FOUR' => '4',
        'FIVE' => '5',
        'SIX' => '6',
        'SEVEN' => '7',
        'EIGHT' => '8',
        'NINE' => '9',
        'TEN' => '10',
        'JACK' => 'J',
        'QUEEN' => 'Q',
        'KING' => 'K',
        'ACE' => 'A',
        'TWO' => '2',
        'SMALL_JOKER' => '小王',
        'BIG_JOKER' => '大王',
        _ => card.rank,
      };

  @override
  Widget build(BuildContext context) {
    final cornerRankSize = width * (_isJoker ? 0.22 : 0.34);
    final cornerSuitSize = width * 0.28;
    final centerSize = width * (_isJoker ? 0.46 : 0.58);

    return AnimatedContainer(
      duration: const Duration(milliseconds: 120),
      transform: Matrix4.translationValues(0, selected ? -12 : 0, 0),
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: const Color(0xFFFFFEFA),
        borderRadius: BorderRadius.circular(width * 0.1),
        border: Border.all(
          color: selected ? gold : const Color(0x42000000),
          width: selected ? 2 : 1,
        ),
        boxShadow: [
          BoxShadow(
            blurRadius: selected ? 8 : 4,
            offset: const Offset(0, 2),
            color: const Color(0x42000000),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            top: height * 0.07,
            left: width * 0.12,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  _rank,
                  textScaler: TextScaler.noScaling,
                  style: TextStyle(
                    color: _ink,
                    fontSize: cornerRankSize,
                    fontWeight: FontWeight.w800,
                    height: 0.95,
                  ),
                ),
                if (!_isJoker)
                  Text(
                    _suit,
                    textScaler: TextScaler.noScaling,
                    style: TextStyle(
                      color: _ink,
                      fontSize: cornerSuitSize,
                      height: 1,
                    ),
                  ),
              ],
            ),
          ),
          Center(
            child: Padding(
              padding: EdgeInsets.only(top: height * 0.1),
              child: Text(
                _isJoker ? '王' : _suit,
                textScaler: TextScaler.noScaling,
                style: TextStyle(
                  color: _ink,
                  fontSize: centerSize,
                  fontWeight: FontWeight.w800,
                  height: 1,
                ),
              ),
            ),
          ),
          if (selected)
            Positioned.fill(
              child: IgnorePointer(
                child: Padding(
                  padding: const EdgeInsets.all(4),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      border: Border.all(color: const Color(0x99E0A928)),
                      borderRadius: BorderRadius.circular(width * 0.06),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
