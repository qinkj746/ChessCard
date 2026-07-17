import 'package:flutter/material.dart';

class AppTheme {
  const AppTheme._();

  static const night = Color(0xFF0F172A);
  static const nightSurface = Color(0xFF132033);
  static const nightSurfaceAlt = Color(0xFF18273B);
  static const felt = Color(0xFF0F6B45);
  static const feltDeep = Color(0xFF073C2D);
  static const feltSoft = Color(0xFF16825A);
  static const gold = Color(0xFFD99A2B);
  static const goldSoft = Color(0xFFF3D48A);
  static const paper = Color(0xFFFFFAF0);
  static const ink = Color(0xFF172033);
  static const muted = Color(0xFF93A4B8);

  static ThemeData theme() {
    return ThemeData(useMaterial3: true, colorSchemeSeed: Colors.green);
  }
}
