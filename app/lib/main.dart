import 'package:flutter/material.dart';

import 'game_page.dart';
import 'room_page.dart';

void main() {
  runApp(const ChessCardApp());
}

class ChessCardApp extends StatelessWidget {
  const ChessCardApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.green),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('升级')),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            FilledButton.icon(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const GamePage()),
              ),
              icon: const Icon(Icons.play_arrow),
              label: const Text('单人游戏'),
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const RoomPage()),
              ),
              icon: const Icon(Icons.meeting_room),
              label: const Text('进入房间'),
            ),
          ],
        ),
      ),
    );
  }
}
