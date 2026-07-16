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

  static const _silverEdge = Color(0xFFD7DBDE);
  static const _silverCore = Color(0xFFF2F4F6);
  static const _goldEdge = Color(0xFFF3D879);
  static const _goldCore = Color(0xFFFFF6D6);
  static const _silverAccent = Color(0xFF9AA0A5);

  @override
  Widget build(BuildContext context) {
    final accent = isBig ? PlayingCard.gold : _silverAccent;
    final badgeInner = isBig ? _goldCore : _silverCore;
    final badgeOuter = isBig ? _goldEdge : _silverEdge;

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
        Center(
          child: Container(
            width: width * 0.62,
            height: width * 0.62,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: RadialGradient(
                colors: [badgeInner, badgeOuter],
                stops: const [0.4, 1.0],
              ),
              boxShadow: isBig
                  ? [
                      BoxShadow(
                        color: PlayingCard.gold.withValues(alpha: 0.35),
                        blurRadius: 8,
                      ),
                    ]
                  : null,
            ),
            child: CustomPaint(
              key: Key(isBig ? 'joker-art-big' : 'joker-art-small'),
              painter: _JokerArtworkPainter(ink: ink, accent: accent),
            ),
          ),
        ),
        if (isBig)
          Positioned(
            key: const Key('joker-flare-big'),
            top: height * 0.08,
            right: width * 0.08,
            child: Icon(
              Icons.auto_awesome_rounded,
              color: PlayingCard.gold,
              size: width * 0.2,
            ),
          ),
        Positioned(
          right: width * 0.08,
          bottom: height * 0.05,
          child: Container(
            padding: EdgeInsets.symmetric(
              horizontal: width * 0.06,
              vertical: width * 0.02,
            ),
            decoration: BoxDecoration(
              color: ink,
              borderRadius: BorderRadius.circular(width * 0.05),
            ),
            child: Text(
              isBig ? 'BIG' : 'SM',
              textScaler: TextScaler.noScaling,
              style: TextStyle(
                color: Colors.white,
                fontSize: width * 0.11,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.6,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _JokerArtworkPainter extends CustomPainter {
  const _JokerArtworkPainter({required this.ink, required this.accent});

  final Color ink;
  final Color accent;

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final stroke = Paint()
      ..color = ink
      ..style = PaintingStyle.stroke
      ..strokeWidth = w * 0.035
      ..strokeJoin = StrokeJoin.round
      ..strokeCap = StrokeCap.round;
    final crownFill = Paint()
      ..color = accent
      ..style = PaintingStyle.fill;

    // Symmetric 5-point crown with a base band.
    final crown = Path()
      ..moveTo(w * 0.15, h * 0.72)
      ..lineTo(w * 0.15, h * 0.5)
      ..lineTo(w * 0.28, h * 0.62)
      ..lineTo(w * 0.35, h * 0.34)
      ..lineTo(w * 0.5, h * 0.55)
      ..lineTo(w * 0.65, h * 0.34)
      ..lineTo(w * 0.72, h * 0.62)
      ..lineTo(w * 0.85, h * 0.5)
      ..lineTo(w * 0.85, h * 0.72)
      ..close();
    canvas.drawPath(crown, crownFill);
    canvas.drawPath(crown, stroke);

    // Separator between the band and the peaks.
    canvas.drawLine(
      Offset(w * 0.15, h * 0.62),
      Offset(w * 0.85, h * 0.62),
      stroke,
    );

    // Pearl tips on each peak.
    final pearl = Paint()..color = ink;
    for (final tip in [
      Offset(w * 0.35, h * 0.34),
      Offset(w * 0.5, h * 0.24),
      Offset(w * 0.65, h * 0.34),
    ]) {
      canvas.drawCircle(tip, w * 0.05, pearl);
    }

    // Diamond jewel centered on the band.
    final jewel = Path()
      ..moveTo(w * 0.5, h * 0.62)
      ..lineTo(w * 0.56, h * 0.67)
      ..lineTo(w * 0.5, h * 0.72)
      ..lineTo(w * 0.44, h * 0.67)
      ..close();
    canvas.drawPath(jewel, pearl);
  }

  @override
  bool shouldRepaint(covariant _JokerArtworkPainter oldDelegate) {
    return oldDelegate.ink != ink || oldDelegate.accent != accent;
  }
}
