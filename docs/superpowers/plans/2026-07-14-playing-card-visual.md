# 经典扑克牌面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将手牌和牌桌出牌从中文组合文本替换为统一、可复用的经典扑克牌面组件。

**Architecture:** 新建 `PlayingCard` 展示组件，集中处理点数、花色、王、颜色、尺寸和选中视觉。`GamePage` 保留现有交互与布局，只在手牌和当前墩渲染位置传入不同尺寸。

**Tech Stack:** Flutter、Dart、Material、`flutter_test`

## Global Constraints

- 不引入图片、字体或第三方牌库依赖。
- 手牌和牌桌出牌必须复用同一个组件。
- 叫主按钮和状态栏继续显示中文花色。
- 不改变选牌、出牌、牌局状态或网络逻辑。
- 未经用户明确要求不创建 Git commit。

---

### Task 1: 经典扑克牌面组件

**Files:**
- Create: `app/lib/playing_card.dart`
- Create: `app/test/playing_card_test.dart`

**Interfaces:**
- Consumes: `CardModel` from `app/lib/models.dart`
- Produces: `PlayingCard({Key? key, required CardModel card, required double width, required double height, bool selected = false})`

- [ ] **Step 1: 写四种花色与点数的失败测试**

在 `app/test/playing_card_test.dart` 创建测试壳，并验证四个标准花色符号及点数：

```dart
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
}
```

- [ ] **Step 2: 运行测试并确认组件尚不存在**

Run: `cd app; flutter test test/playing_card_test.dart`

Expected: FAIL，提示无法解析 `package:chess_card_app/playing_card.dart` 或 `PlayingCard`。

- [ ] **Step 3: 增加红黑颜色、大小王和选中状态测试**

继续在同一测试文件增加：

```dart
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
  await tester.pumpWidget(cardHost(
    const CardModel(suit: null, rank: 'SMALL_JOKER', deckIndex: 0),
  ));
  expect(find.text('小王'), findsOneWidget);
  expect(find.text('王'), findsOneWidget);

  await tester.pumpWidget(cardHost(
    const CardModel(suit: null, rank: 'BIG_JOKER', deckIndex: 0),
  ));
  expect(find.text('大王'), findsOneWidget);
  expect(find.text('王'), findsOneWidget);
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
```

- [ ] **Step 4: 实现最小可复用组件**

在 `app/lib/playing_card.dart` 实现：

```dart
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

  bool get _isJoker => card.rank == 'SMALL_JOKER' || card.rank == 'BIG_JOKER';
  Color get _ink => card.suit == 'HEART' || card.suit == 'DIAMOND' || card.rank == 'BIG_JOKER'
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
                Text(_rank, style: TextStyle(color: _ink, fontSize: cornerRankSize, fontWeight: FontWeight.w800, height: 0.95)),
                if (!_isJoker)
                  Text(_suit, style: TextStyle(color: _ink, fontSize: cornerSuitSize, height: 1)),
              ],
            ),
          ),
          Center(
            child: Padding(
              padding: EdgeInsets.only(top: height * 0.1),
              child: Text(
                _isJoker ? '王' : _suit,
                style: TextStyle(color: _ink, fontSize: centerSize, fontWeight: FontWeight.w800, height: 1),
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
```

- [ ] **Step 5: 运行组件测试**

Run: `cd app; flutter test test/playing_card_test.dart`

Expected: PASS，全部牌面映射、颜色、王和选中状态测试通过。

---

### Task 2: 接入手牌与牌桌出牌

**Files:**
- Modify: `app/lib/game_page.dart:1-10`
- Modify: `app/lib/game_page.dart:404-444`
- Modify: `app/lib/game_page.dart:636-686`
- Modify: `app/test/widget_test.dart:1-20`
- Modify: `app/test/widget_test.dart:240-270`

**Interfaces:**
- Consumes: `PlayingCard` from Task 1
- Produces: `GamePage` renders `PlayingCard` for hand and trick cards while preserving `GestureDetector` selection

- [ ] **Step 1: 写 GamePage 统一组件的失败测试**

在 `app/test/widget_test.dart` 导入 `playing_card.dart`：

```dart
import 'package:chess_card_app/playing_card.dart';
```

将当前墩测试增强为：

```dart
testWidgets('current trick and hand use playing cards', (tester) async {
  await tester.pumpWidget(
    const MaterialApp(home: GamePage(initialGame: trickGame)),
  );

  expect(find.byType(PlayingCard), findsWidgets);
  expect(find.text('K'), findsOneWidget);
  expect(find.text('♠'), findsNWidgets(2));
  expect(find.textContaining('黑桃'), findsNothing);
});
```

保留现有窄屏测试，并增加 `expect(find.byType(PlayingCard), findsWidgets);`。

- [ ] **Step 2: 运行 GamePage 测试并确认尚未接入**

Run: `cd app; flutter test test/widget_test.dart --plain-name "current trick and hand use playing cards"`

Expected: FAIL，`PlayingCard` 数量为 0。

- [ ] **Step 3: 在 GamePage 导入组件**

在 `app/lib/game_page.dart` 的本地导入区增加：

```dart
import 'playing_card.dart';
```

- [ ] **Step 4: 替换牌桌中央文本牌面**

将 `_trickSeat` 中现有 `Container` 和 `Text('${_suit(...)}\n${_rank(...)}')` 替换为：

```dart
PlayingCard(
  card: card,
  width: 42,
  height: 56,
)
```

保留外层 `Wrap` 的 `spacing: 4`，确保拖拉机等多张组合继续紧凑排列。

- [ ] **Step 5: 替换手牌文本牌面并保留点击行为**

将 `_buildHand` 内 `GestureDetector` 的子组件替换为：

```dart
PlayingCard(
  card: card,
  width: 58,
  height: 110,
  selected: isSelected,
)
```

删除旧的 `AnimatedContainer` 牌面实现，但保留 `GestureDetector.onTap`、`selected` 集合和横向列表尺寸。`PlayingCard` 自己负责 120ms 上移动画和选中描边。

- [ ] **Step 6: 运行相关 Widget 测试**

Run: `cd app; flutter test test/widget_test.dart`

Expected: PASS，现有选牌、出牌按钮、当前墩和窄屏测试全部通过。

---

### Task 3: 格式化与完整验证

**Files:**
- Verify: `app/lib/playing_card.dart`
- Verify: `app/lib/game_page.dart`
- Verify: `app/test/playing_card_test.dart`
- Verify: `app/test/widget_test.dart`

**Interfaces:**
- Consumes: 完成的牌面组件与 GamePage 接入
- Produces: 格式化且通过静态分析和全部 Flutter 测试的客户端改动

- [ ] **Step 1: 格式化修改文件**

Run: `cd app; dart format lib/playing_card.dart lib/game_page.dart test/playing_card_test.dart test/widget_test.dart`

Expected: 命令成功，四个文件符合 Dart 格式。

- [ ] **Step 2: 运行全部客户端测试和静态分析**

Run: `powershell -ExecutionPolicy Bypass -File scripts/test-app.ps1`

Expected: `flutter test` 全部 PASS，`flutter analyze` 报告 `No issues found!`。

- [ ] **Step 3: 检查差异范围**

Run: `git -c safe.directory=D:/Project/AI/ChessCard diff --check`

Expected: 无尾随空格或空白错误；差异只包含设计文档、计划文档、牌面组件、GamePage 接入和对应测试。
