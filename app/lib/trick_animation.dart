import 'package:flutter/material.dart';

class TrickAnimation extends StatelessWidget {
  const TrickAnimation({
    super.key,
    required this.message,
    required this.visible,
  });

  final String message;
  final bool visible;

  @override
  Widget build(BuildContext context) {
    return AnimatedSlide(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      offset: visible ? Offset.zero : const Offset(0, 0.18),
      child: AnimatedOpacity(
        duration: const Duration(milliseconds: 220),
        opacity: visible ? 1 : 0,
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: Colors.amber.shade200,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.amber.shade700, width: 1.5),
            boxShadow: const [
              BoxShadow(
                blurRadius: 10,
                offset: Offset(0, 3),
                color: Colors.black26,
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            child: Text(
              message,
              style: TextStyle(
                color: Colors.brown.shade900,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
