# Room Chat Message Deduplication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure a room chat message is rendered once when its WebSocket event arrives before the matching REST send response.

**Architecture:** Keep the REST command and WebSocket event paths unchanged, but route both results through one idempotent list-update helper in `RoomPage`. The helper uses the server-generated `messageId` as the identity key, preserving REST-only feedback when WebSocket is unavailable.

**Tech Stack:** Flutter, Dart, `flutter_test`, existing `GameApi` and `RoomEventSource` test fakes

---

## File Structure

- Modify `app/test/widget_test.dart`: add a deterministic delayed-send fake and a Widget regression test that reproduces WebSocket-before-REST delivery.
- Modify `app/lib/room_page.dart`: centralize chat-list insertion and use it from both asynchronous delivery paths.

### Task 1: Reproduce and fix duplicate room chat messages

**Files:**
- Modify: `app/test/widget_test.dart:350-396`
- Modify: `app/test/widget_test.dart:920-1050`
- Modify: `app/lib/room_page.dart:184-204`
- Modify: `app/lib/room_page.dart:277-299`

- [ ] **Step 1: Add controllable send completion to the test fake**

Add this field beside `sentChatContents` in `FakeApiClient`:

```dart
Completer<ChatMessageModel>? sendRoomMessageCompleter;
```

Update `sendRoomMessage` so a test can hold the REST response while still recording the outgoing content:

```dart
@override
Future<ChatMessageModel> sendRoomMessage({
  required String roomId,
  required String playerId,
  required String content,
}) async {
  sentChatContents.add(content);
  final pendingResponse = sendRoomMessageCompleter;
  if (pendingResponse != null) {
    final message = await pendingResponse.future;
    chatMessages.add(message);
    return message;
  }
  final message = ChatMessageModel(
    messageId: 'message-${sentChatContents.length}',
    roomId: roomId,
    senderPlayerId: playerId,
    content: content,
    sentAt: DateTime.parse('2026-07-10T08:00:00Z'),
  );
  chatMessages.add(message);
  return message;
}
```

- [ ] **Step 2: Write the failing WebSocket-before-REST regression test**

Add this test after `room page sends chat message and clears input`:

```dart
testWidgets('room page deduplicates chat event before send response',
    (WidgetTester tester) async {
  final events = FakeRoomEventSource();
  final api = FakeApiClient(playGame);
  final sendCompleter = Completer<ChatMessageModel>();
  api.sendRoomMessageCompleter = sendCompleter;
  await tester.pumpWidget(MaterialApp(
    home: RoomPage(
      api: api,
      roomConnectionFactory: (_) => events,
    ),
  ));
  await tester.pumpAndSettle();

  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField), 'hello once');
  await tester.tap(find.byIcon(Icons.send));
  await tester.pump();

  events.emit(const RoomEventModel(
    type: 'CHAT_MESSAGE',
    roomId: 'room-1',
    payload: {
      'messageId': 'message-1',
      'roomId': 'room-1',
      'senderPlayerId': 'fake-player',
      'content': 'hello once',
      'sentAt': '2026-07-10T08:00:00Z',
    },
  ));
  await tester.pump();
  sendCompleter.complete(ChatMessageModel(
    messageId: 'message-1',
    roomId: 'room-1',
    senderPlayerId: 'fake-player',
    content: 'hello once',
    sentAt: DateTime.parse('2026-07-10T08:00:00Z'),
  ));
  await tester.pumpAndSettle();

  expect(api.sentChatContents, ['hello once']);
  expect(find.text('hello once'), findsOneWidget);
  final field = tester.widget<TextField>(find.byType(TextField));
  expect(field.controller?.text, isEmpty);
});
```

- [ ] **Step 3: Run the new test and verify the expected failure**

Run from `app`:

```powershell
flutter test test/widget_test.dart --plain-name "room page deduplicates chat event before send response"
```

Expected: FAIL because `find.text('hello once')` finds two widgets after the WebSocket event and REST response both append `message-1`.

- [ ] **Step 4: Add the minimal idempotent insertion helper**

Add this method near `_handleRoomEvent` in `_RoomPageState`:

```dart
void _addChatMessageIfAbsent(ChatMessageModel message) {
  if (_chatMessages.every(
    (item) => item.messageId != message.messageId,
  )) {
    _chatMessages.add(message);
  }
}
```

Replace the WebSocket branch's inline conditional with:

```dart
if (mounted) {
  setState(() => _addChatMessageIfAbsent(message));
}
```

Replace the REST success branch's unconditional append with:

```dart
setState(() {
  _addChatMessageIfAbsent(message);
  _chatController.clear();
});
```

- [ ] **Step 5: Run the regression and existing chat tests**

Run from `app`:

```powershell
flutter test test/widget_test.dart --plain-name "room page deduplicates chat event before send response"
flutter test test/widget_test.dart --plain-name "room page sends chat message and clears input"
flutter test test/widget_test.dart --plain-name "room page appends incoming chat message event"
```

Expected: all three commands report `All tests passed!`.

- [ ] **Step 6: Run Flutter verification**

Run from `app`:

```powershell
flutter test
flutter analyze
```

Expected: the full test suite passes and static analysis reports `No issues found!`.

- [ ] **Step 7: Review and commit the implementation**

Run from the repository root:

```powershell
git diff --check
git diff -- app/test/widget_test.dart app/lib/room_page.dart
git add -- app/test/widget_test.dart app/lib/room_page.dart
git commit -m "fix: deduplicate room chat messages"
```

Expected: the diff contains only the regression test, its deterministic fake support, and the shared insertion helper; the commit completes successfully.
