import 'package:flutter/material.dart';

class TableLayout extends StatelessWidget {
  const TableLayout({
    super.key,
    required this.status,
    this.banner,
    this.message,
    required this.board,
    this.playedHistory,
    required this.actions,
    required this.hand,
  });

  final Widget status;
  final Widget? banner;
  final Widget? message;
  final Widget board;
  final Widget? playedHistory;
  final Widget actions;
  final Widget hand;

  static const double _compactBreakpoint = 700;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < _compactBreakpoint) {
          return _buildCompact(constraints);
        }
        return _buildWide();
      },
    );
  }

  Widget _buildWide() {
    return Column(
      children: [
        status,
        if (banner != null) banner!,
        if (message != null) message!,
        Expanded(child: board),
        if (playedHistory != null) playedHistory!,
        actions,
        hand,
      ],
    );
  }

  Widget _buildCompact(BoxConstraints constraints) {
    final boardHeight = _compactBoardHeight(constraints);
    return SingleChildScrollView(
      child: ConstrainedBox(
        constraints: BoxConstraints(minHeight: constraints.maxHeight),
        child: Column(
          children: [
            status,
            if (banner != null) banner!,
            if (message != null) message!,
            SizedBox(height: boardHeight, child: board),
            if (playedHistory != null) playedHistory!,
            actions,
            hand,
          ],
        ),
      ),
    );
  }

  double _compactBoardHeight(BoxConstraints constraints) {
    final availableHeight =
        constraints.hasBoundedHeight ? constraints.maxHeight : 640.0;
    return (availableHeight * 0.42).clamp(220.0, 320.0).toDouble();
  }
}
