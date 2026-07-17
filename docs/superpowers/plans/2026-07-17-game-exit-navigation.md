# Game Exit Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a left app-bar exit control that immediately leaves local games while requiring confirmation for active room games, including platform back navigation.

**Architecture:** `GamePage` keeps one exit coordinator that decides whether it can pop directly or must ask the active-room confirmation question. Its `AppBar.leading` and `PopScope` call that coordinator, so they cannot drift into different behavior. Room departure continues to use the existing `GameApi.leavePlayingRoom` request and error banner.

**Tech Stack:** Flutter 3.41, Dart 3.11, `flutter_test`, Material `PopScope`.

---

## File Structure

- Modify: `app/lib/game_page.dart` - add the common exit coordinator, leading app-bar arrow, platform-back interception, and remove the duplicate room return action.
- Modify: `app/test/widget_test.dart` - document local direct exit and active-room platform-back confirmation through widget tests.

### Task 1: Write Exit Navigation Regressions

**Files:**
- Modify: `app/test/widget_test.dart` near the existing room-exit tests

- [ ] **Step 1: Add a local-game direct-exit test**

```dart
testWidgets('local game back arrow exits without a room request',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await tester.pumpWidget(MaterialApp(
    home: Builder(
      builder: (context) => FilledButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => GamePage(initialGame: playGame, api: api),
          ),
        ),
        child: const Text('open local game'),
      ),
    ),
  ));

  await tester.tap(find.text('open local game'));
  await tester.pumpAndSettle();
  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();

  expect(find.text('open local game'), findsOneWidget);
  expect(find.text('游戏正在进行'), findsNothing);
  expect(api.leavePlayingRoomCalls, 0);
});
```

- [ ] **Step 2: Add an active-room platform-back test**

```dart
testWidgets('system back confirms before leaving an active room game',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await tester.pumpWidget(MaterialApp(
    home: Builder(
      builder: (context) => FilledButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => GamePage(
              initialGame: playGame,
              api: api,
              playerId: 'fake-player',
              roomId: 'room-1',
            ),
          ),
        ),
        child: const Text('open room game'),
      ),
    ),
  ));

  await tester.tap(find.text('open room game'));
  await tester.pumpAndSettle();
  await tester.pageBack();
  await tester.pumpAndSettle();

  expect(find.text('游戏正在进行'), findsOneWidget);
  expect(find.byType(GamePage), findsOneWidget);
  expect(api.leavePlayingRoomCalls, 0);
});
```

- [ ] **Step 3: Run the two tests and confirm they fail because the local arrow and platform-back guard are absent**

Run: `flutter test test/widget_test.dart --plain-name "local game back arrow exits without a room request"`

Expected: FAIL because `Icons.arrow_back` is not present in a local game.

Run: `flutter test test/widget_test.dart --plain-name "system back confirms before leaving an active room game"`

Expected: FAIL because the game page is popped instead of displaying `游戏正在进行`.

### Task 2: Implement The Shared Exit Coordinator

**Files:**
- Modify: `app/lib/game_page.dart` near `_isActiveRoomGame`, `build`, and `_buildActions`

- [ ] **Step 1: Add the common exit methods**

```dart
Future<void> _handleGameExit() async {
  final current = game;
  if (current == null || !widget.isRoomMode || !_isActiveRoomGame) {
    Navigator.of(context).pop(current?.id);
    return;
  }
  await _handleRoomExit();
}

void _handleBackNavigation(bool didPop, Object? result) {
  if (didPop || loading || !_isActiveRoomGame) return;
  _handleGameExit();
}
```

- [ ] **Step 2: Route the existing room confirmation through the common exit path**

Keep `_handleRoomExit` as the API-specific confirmation and departure implementation. It still directly pops finished room games, and `GamePage` calls `_handleGameExit` from UI navigation instead of calling `_handleRoomExit` directly.

- [ ] **Step 3: Add leading arrow and `PopScope`**

```dart
return PopScope<Object?>(
  canPop: !_isActiveRoomGame,
  onPopInvokedWithResult: _handleBackNavigation,
  child: Scaffold(
    appBar: AppBar(
      automaticallyImplyLeading: false,
      leading: IconButton(
        tooltip: '返回',
        onPressed: loading ? null : _handleGameExit,
        icon: const Icon(Icons.arrow_back),
      ),
      title: const Text('升级牌局'),
      actions: [
        if (current != null && !widget.isRoomMode)
          IconButton(
            tooltip: '重新开局',
            onPressed: loading ? null : () => _run(api.createGame),
            icon: const Icon(Icons.refresh),
          ),
      ],
    ),
    body: current == null ? _buildStart() : _buildTable(current),
  ),
);
```

- [ ] **Step 4: Remove the duplicate finished-room return action**

Replace the finished-game branch with only the local next-game action:

```dart
if (game.phase == 'FINISHED' && !game.completed && !widget.isRoomMode)
  FilledButton.icon(
    onPressed: loading
        ? null
        : () {
            Future<GameStateModel> action() => api.nextGame(game.id);
            _run(action, retryAction: () => _run(action));
          },
    icon: const Icon(Icons.skip_next),
    label: const Text('下一局'),
  ),
```

- [ ] **Step 5: Format and run the new regression tests**

Run: `dart format lib/game_page.dart test/widget_test.dart`

Run: `flutter test test/widget_test.dart --plain-name "local game back arrow exits without a room request"`

Expected: PASS.

Run: `flutter test test/widget_test.dart --plain-name "system back confirms before leaving an active room game"`

Expected: PASS.

### Task 3: Verify Existing Room Exit Behavior

**Files:**
- Verify: `app/test/widget_test.dart`

- [ ] **Step 1: Run all game-page exit tests**

Run: `flutter test test/widget_test.dart --plain-name "active room game confirms before exiting"`

Run: `flutter test test/widget_test.dart --plain-name "canceling active room game exit stays on game page"`

Run: `flutter test test/widget_test.dart --plain-name "confirming active room game exit calls API and returns"`

Run: `flutter test test/widget_test.dart --plain-name "active room game exit failure stays and shows error"`

Run: `flutter test test/widget_test.dart --plain-name "finished room game returns without in-progress confirmation"`

Expected: every command exits with code 0.

- [ ] **Step 2: Run full client validation**

Run: `flutter test`

Run: `flutter analyze`

Expected: both commands exit with code 0.

- [ ] **Step 3: Review the final diff before commit**

Run: `git diff --check -- app/lib/game_page.dart app/test/widget_test.dart`

Expected: no whitespace errors and only the intended exit-navigation changes plus pre-existing user modifications.
