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
        _ => card.rank,
      };

  String get _semanticLabel => switch (card.rank) {
        'SMALL_JOKER' => '小王',
        'BIG_JOKER' => '大王',
        _ => '$_rank $_suit',
      };

  @override
  Widget build(BuildContext context) {
    final cornerRankSize = width * 0.34;
    final cornerSuitSize = width * 0.28;
    final centerSize = width * 0.58;

    return Semantics(
      label: _semanticLabel,
      excludeSemantics: true,
      child: AnimatedContainer(
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
            if (_isJoker)
              Positioned.fill(
                child: _JokerFace(
                  width: width,
                  height: height,
                  ink: _ink,
                  isBig: card.rank == 'BIG_JOKER',
                ),
              )
            else ...[
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
                    _suit,
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
            ],
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
      ),
    );
  }
}

class _JokerFace extends StatelessWidget {
  const _JokerFace({
    required this.width,
    required this.height,
    required this.ink,
    required this.isBig,
  });

  final double width;
  final double height;
  final Color ink;
  final bool isBig;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned(
          top: height * 0.07,
          left: width * 0.12,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'J',
                textScaler: TextScaler.noScaling,
                style: TextStyle(
                  color: ink,
                  fontSize: width * 0.32,
                  fontWeight: FontWeight.w900,
                  height: 0.9,
                ),
              ),
              Icon(
                isBig ? Icons.star_rounded : Icons.star_outline_rounded,
                color: ink,
                size: width * 0.2,
              ),
            ],
          ),
        ),
        Align(
          alignment: const Alignment(0.25, 0.25),
          child: SizedBox(
            width: width * 0.6,
            height: height * 0.48,
            child: CustomPaint(
              key: Key(isBig ? 'joker-art-big' : 'joker-art-small'),
              painter: _JokerArtworkPainter(ink: ink, isBig: isBig),
            ),
          ),
        ),
        if (isBig)
          Positioned(
            key: const Key('joker-flare-big'),
            top: height * 0.1,
            right: width * 0.1,
            child: Icon(
              Icons.auto_awesome_rounded,
              color: ink,
              size: width * 0.2,
            ),
          ),
      ],
    );
  }
}

class _JokerArtworkPainter extends CustomPainter {
  const _JokerArtworkPainter({required this.ink, required this.isBig});

  final Color ink;
  final bool isBig;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = Paint()
      ..color = ink
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.width * 0.055
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    final fill = Paint()
      ..color = ink.withValues(alpha: isBig ? 0.16 : 0.09)
      ..style = PaintingStyle.fill;

    final crown = Path()
      ..moveTo(size.width * 0.17, size.height * 0.7)
      ..lineTo(size.width * 0.12, size.height * 0.32)
      ..lineTo(size.width * 0.36, size.height * 0.5)
      ..lineTo(size.width * 0.5, size.height * 0.16)
      ..lineTo(size.width * 0.64, size.height * 0.5)
      ..lineTo(size.width * 0.88, size.height * 0.32)
      ..lineTo(size.width * 0.83, size.height * 0.7)
      ..quadraticBezierTo(
        size.width * 0.5,
        size.height * 0.82,
        size.width * 0.17,
        size.height * 0.7,
      )
      ..close();

    canvas.drawPath(crown, fill);
    canvas.drawPath(crown, stroke);

    final tips = [
      Offset(size.width * 0.12, size.height * 0.3),
      Offset(size.width * 0.5, size.height * 0.14),
      Offset(size.width * 0.88, size.height * 0.3),
    ];
    final tipPaint = Paint()
      ..color = ink
      ..style = isBig ? PaintingStyle.fill : PaintingStyle.stroke
      ..strokeWidth = size.width * 0.045;
    for (final tip in tips) {
      canvas.drawCircle(tip, size.width * 0.055, tipPaint);
    }

    final jewel = Path()
      ..moveTo(size.width * 0.5, size.height * 0.53)
      ..lineTo(size.width * 0.58, size.height * 0.63)
      ..lineTo(size.width * 0.5, size.height * 0.73)
      ..lineTo(size.width * 0.42, size.height * 0.63)
      ..close();
    canvas.drawPath(jewel, isBig ? tipPaint : stroke);
  }

  @override
  bool shouldRepaint(covariant _JokerArtworkPainter oldDelegate) {
    return oldDelegate.ink != ink || oldDelegate.isBig != isBig;
  }
}
