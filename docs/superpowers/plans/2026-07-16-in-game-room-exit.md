# In-Game Room Exit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe in-game room exit behavior: warn the exiting player, convert their seat to system control, keep rooms alive while humans remain, and delete rooms only when no humans remain.

**Architecture:** The backend owns room lifecycle and seat ownership changes. `RoomService` gets an explicit `leavePlayingRoom` path and delegates game seat-owner removal to `GameService`, while the app calls a new room exit endpoint from `GamePage` after confirmation. Existing waiting-room leave behavior is adjusted to transfer ownership instead of deleting rooms that still have humans.

**Tech Stack:** Spring Boot Java 17, JUnit 5/AssertJ, Flutter/Dart, `flutter_test`, `http`.

---

## Baseline Note

`scripts/verify-all.ps1` failed before implementation because Maven was running under Java 8 even though the project uses Java 17 records and switch expressions. Run backend commands with JDK 17 explicitly:

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## File Structure

- Modify `server/src/main/java/com/chesscard/shengji/service/GameService.java`: add a small method to clear a room-game seat owner when a player leaves.
- Modify `server/src/main/java/com/chesscard/shengji/service/RoomService.java`: implement waiting-room owner transfer and playing-room exit.
- Modify `server/src/main/java/com/chesscard/shengji/api/RoomController.java`: expose a new playing-room exit endpoint.
- Modify `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`: cover room lifecycle and ownership transfer rules.
- Modify `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`: cover the REST endpoint.
- Modify `app/lib/api_client.dart`: add `leavePlayingRoom`.
- Modify `app/test/api_client_test.dart`: cover request method, URL, body, and bearer token.
- Modify `app/lib/game_page.dart`: confirm before active room-game exit and call the API.
- Modify `app/test/widget_test.dart`: cover confirm, cancel, error, and finished-game navigation.

### Task 1: Backend Room-Service Exit Rules

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Add these tests to `RoomServiceTest`:

```java
@Test
void ownerLeavingWaitingRoomTransfersOwnershipWhenAnotherHumanRemains() {
    RoomState room = service.createRoom("player-1");
    service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);

    RoomState updated = service.leaveSeat(room.getRoomId(), "player-1", PlayerSeat.SOUTH);

    assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
    assertThat(updated.getSeats()).doesNotContainKey(PlayerSeat.SOUTH);
    assertThat(roomRepo.store).containsKey(room.getRoomId());
}

@Test
void leavingPlayingRoomConvertsSeatToBotAndKeepsRoomWhenHumanRemains() {
    RoomState room = service.createRoom("player-1");
    service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    GameState game = service.startGame(room.getRoomId(), "player-1");

    RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-2");

    assertThat(updated.getPhase()).isEqualTo(RoomPhase.PLAYING);
    assertThat(updated.getSeats().get(PlayerSeat.NORTH).isBot()).isTrue();
    assertThat(updated.getSeats().get(PlayerSeat.NORTH).getPlayerId()).isNull();
    assertThat(roomRepo.store).containsKey(room.getRoomId());
    assertThat(gameService.getGame(game.getId()).getSeatOwners().get(PlayerSeat.NORTH)).isNull();
}

@Test
void ownerLeavingPlayingRoomTransfersOwnershipWhenHumanRemains() {
    RoomState room = service.createRoom("player-1");
    service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    service.startGame(room.getRoomId(), "player-1");

    RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-1");

    assertThat(updated.getOwnerPlayerId()).isEqualTo("player-2");
    assertThat(updated.getSeats().get(PlayerSeat.SOUTH).isBot()).isTrue();
}

@Test
void leavingPlayingRoomDeletesRoomWhenNoHumanRemains() {
    RoomState room = service.createRoom("player-1");
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    GameState game = service.startGame(room.getRoomId(), "player-1");

    RoomState updated = service.leavePlayingRoom(room.getRoomId(), "player-1");

    assertThat(updated.getSeats().get(PlayerSeat.SOUTH).isBot()).isTrue();
    assertThat(roomRepo.store).doesNotContainKey(room.getRoomId());
    assertThat(gameService.getGame(game.getId()).getSeatOwners().get(PlayerSeat.SOUTH)).isNull();
}

@Test
void leavePlayingRoomRejectsPlayerWhoIsNotSeated() {
    RoomState room = service.createRoom("player-1");
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    service.startGame(room.getRoomId(), "player-1");

    assertThatThrownBy(() -> service.leavePlayingRoom(room.getRoomId(), "player-2"))
            .isInstanceOf(PermissionDeniedException.class)
            .hasMessageContaining("未入座");
}

@Test
void departedPlayerCanNoLongerActAsOldRoomGameSeat() {
    RoomState room = service.createRoom("player-1");
    service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    GameState game = service.startGame(room.getRoomId(), "player-1");

    service.leavePlayingRoom(room.getRoomId(), "player-2");

    assertThatThrownBy(() -> gameService.play(game.getId(), PlayerSeat.NORTH,
            List.of(gameService.getGame(game.getId()).getHands().get(PlayerSeat.NORTH).get(0)), "player-2"))
            .isInstanceOf(PermissionDeniedException.class);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd server
mvn -Dtest=RoomServiceTest test
```

Expected: compilation fails because `leavePlayingRoom` and game seat-owner clearing do not exist, or tests fail because owner exit still deletes the waiting room.

- [ ] **Step 3: Add minimal game seat-owner clearing**

Add this method to `GameService` near `createGameForRoom`:

```java
public GameState clearRoomSeatOwner(String roomId, PlayerSeat seat, String playerId) {
    if (roomId == null || roomId.isBlank()) {
        throw new IllegalArgumentException("roomId 不能为空");
    }
    if (seat == null) {
        throw new IllegalArgumentException("seat 不能为空");
    }
    if (playerId == null || playerId.isBlank()) {
        throw new IllegalArgumentException("playerId 不能为空");
    }
    GameState game = repository.findByRoomId(roomId)
            .orElseThrow(() -> new GameNotFoundException("牌局不存在"));
    String owner = game.getSeatOwners().get(seat);
    if (!playerId.equals(owner)) {
        throw new PermissionDeniedException("该玩家未入座");
    }
    game.getSeatOwners().put(seat, null);
    return saveAndPublish(game);
}
```

If `GameRepository` does not yet expose `findByRoomId`, add it to `GameRepository`, `JpaGameRepository`, and the test fake repository:

```java
Optional<GameState> findByRoomId(String roomId);
```

The fake repository implementation:

```java
@Override
public Optional<GameState> findByRoomId(String roomId) {
    return store.values().stream()
            .filter(game -> roomId.equals(game.getRoomId()))
            .findFirst();
}
```

- [ ] **Step 4: Add minimal room lifecycle implementation**

In `RoomService`, replace the owner-only waiting-room deletion logic with helper-based lifecycle handling:

```java
public RoomState leavePlayingRoom(String roomId, String playerId) {
    if (playerId == null || playerId.isBlank()) {
        throw new IllegalArgumentException("playerId 不能为空");
    }
    RoomState room = getRoom(roomId);
    if (room.getPhase() != RoomPhase.PLAYING) {
        throw new IllegalArgumentException("房间不在游戏中，无法退出游戏");
    }
    PlayerSeat playerSeat = humanSeatFor(room, playerId);
    gameService.clearRoomSeatOwner(room.getRoomId(), playerSeat, playerId);
    room.getSeats().put(playerSeat, RoomSeat.bot(playerSeat, Instant.now()));
    room.touch();
    return saveOrDeleteAfterHumanExit(room);
}

private RoomState saveOrDeleteAfterHumanExit(RoomState room) {
    String nextOwner = firstHumanPlayerId(room);
    if (nextOwner == null) {
        repository.delete(room.getRoomId());
        eventPublisher.publish(RoomEvent.roomUpdated(room));
        return room;
    }
    room.setOwnerPlayerId(nextOwner);
    return saveAndPublish(room);
}

private PlayerSeat humanSeatFor(RoomState room, String playerId) {
    for (Map.Entry<PlayerSeat, RoomSeat> entry : room.getSeats().entrySet()) {
        RoomSeat seat = entry.getValue();
        if (!seat.isBot() && playerId.equals(seat.getPlayerId())) {
            return entry.getKey();
        }
    }
    throw new PermissionDeniedException("该玩家未入座");
}

private String firstHumanPlayerId(RoomState room) {
    return room.getSeats().values().stream()
            .filter(seat -> !seat.isBot())
            .map(RoomSeat::getPlayerId)
            .filter(id -> id != null && !id.isBlank())
            .findFirst()
            .orElse(null);
}
```

Update the end of `leaveSeat` to use the same cleanup rule:

```java
room.getSeats().remove(seat);
room.touch();
return saveOrDeleteAfterHumanExit(room);
```

Add `import java.util.Map;` to `RoomService`.

- [ ] **Step 5: Run service tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd server
mvn -Dtest=RoomServiceTest test
```

Expected: `RoomServiceTest` passes.

- [ ] **Step 6: Commit Task 1**

```powershell
git add server/src/main/java/com/chesscard/shengji/service/GameService.java server/src/main/java/com/chesscard/shengji/service/GameRepository.java server/src/main/java/com/chesscard/shengji/persistence/JpaGameRepository.java server/src/main/java/com/chesscard/shengji/service/RoomService.java server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java
git commit -m "Add backend in-game room exit rules"
```

### Task 2: Backend REST Endpoint

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`

- [ ] **Step 1: Write failing controller test**

Add this test to `RoomControllerTest`:

```java
@Test
void leavePlayingRoomConvertsExitedPlayerToBot() throws Exception {
    RoomState room = roomService.createRoom("player-1");
    roomService.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
    roomService.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    roomService.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
    roomService.startGame(room.getRoomId(), "player-1");

    mockMvc.perform(delete("/api/rooms/{id}/players", room.getRoomId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"playerId\":\"player-2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
            .andExpect(jsonPath("$.phase").value("PLAYING"))
            .andExpect(jsonPath("$.seats.NORTH.bot").value(true))
            .andExpect(jsonPath("$.seats.NORTH.playerId").doesNotExist());
}
```

- [ ] **Step 2: Run controller test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd server
mvn -Dtest=RoomControllerTest#leavePlayingRoomConvertsExitedPlayerToBot test
```

Expected: FAIL with 405 or 404 because the endpoint does not exist.

- [ ] **Step 3: Add endpoint**

Add this method to `RoomController`:

```java
@DeleteMapping("/{id}/players")
public RoomStateDto leavePlayingRoom(@PathVariable String id, @RequestBody JoinSeatRequest request) {
    String playerId = requirePlayerRequest(request);
    return toDto(service.leavePlayingRoom(id, playerId));
}
```

- [ ] **Step 4: Run controller test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd server
mvn -Dtest=RoomControllerTest#leavePlayingRoomConvertsExitedPlayerToBot test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

```powershell
git add server/src/main/java/com/chesscard/shengji/api/RoomController.java server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java
git commit -m "Expose in-game room exit endpoint"
```

### Task 3: Flutter API Client

**Files:**
- Modify: `app/lib/api_client.dart`
- Test: `app/test/api_client_test.dart`

- [ ] **Step 1: Write failing API client test**

Add this test to `api_client_test.dart`:

```dart
test('leavePlayingRoom deletes player from room with bearer token', () async {
  late http.Request capturedRequest;
  final client = ApiClient(
    baseUrl: 'http://example.test',
    sessionToken: 'token-1',
    httpClient: MockClient((request) async {
      capturedRequest = request as http.Request;
      return http.Response(_roomJson(), 200,
          headers: {'content-type': 'application/json'});
    }),
  );

  final room = await client.leavePlayingRoom('room-1', 'player-1');

  expect(capturedRequest.method, 'DELETE');
  expect(capturedRequest.url.toString(),
      'http://example.test/api/rooms/room-1/players');
  expect(capturedRequest.headers['Authorization'], 'Bearer token-1');
  expect(jsonDecode(capturedRequest.body), {'playerId': 'player-1'});
  expect(room.roomId, 'room-1');
});
```

- [ ] **Step 2: Run API client test to verify it fails**

Run:

```powershell
cd app
flutter test test/api_client_test.dart --plain-name "leavePlayingRoom deletes player from room with bearer token"
```

Expected: compilation fails because `leavePlayingRoom` is not defined.

- [ ] **Step 3: Add API method**

Add to `GameApi`:

```dart
Future<RoomStateModel> leavePlayingRoom(String roomId, String playerId);
```

Add to `ApiClient` near `leaveSeat`:

```dart
@override
Future<RoomStateModel> leavePlayingRoom(String roomId, String playerId) async {
  await _ensureSession();
  final request = http.Request(
    'DELETE',
    Uri.parse('$baseUrl/api/rooms/$roomId/players'),
  );
  request.headers.addAll(_headers(json: true));
  request.body = jsonEncode({'playerId': playerId});
  final response = await httpClient.send(request);
  final body = await http.Response.fromStream(response);
  return _decodeRoom(body);
}
```

- [ ] **Step 4: Update fake APIs in tests**

Add this method to each `FakeApiClient implements GameApi` that fails compilation:

```dart
@override
Future<RoomStateModel> leavePlayingRoom(String roomId, String playerId) async {
  roomSeats.updateAll((seat, info) {
    if (info.playerId == playerId) {
      return const SeatInfo(isBot: true, displayName: '人机');
    }
    return info;
  });
  roomVersion += 1;
  return RoomStateModel(
    roomId: roomId,
    phase: 'PLAYING',
    ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
    version: roomVersion,
    seats: Map.unmodifiable(roomSeats),
  );
}
```

- [ ] **Step 5: Run API client test to verify it passes**

Run:

```powershell
cd app
flutter test test/api_client_test.dart --plain-name "leavePlayingRoom deletes player from room with bearer token"
```

Expected: PASS.

- [ ] **Step 6: Commit Task 3**

```powershell
git add app/lib/api_client.dart app/test/api_client_test.dart app/test/widget_test.dart
git commit -m "Add client API for in-game room exit"
```

### Task 4: Flutter GamePage Confirmation Flow

**Files:**
- Modify: `app/lib/game_page.dart`
- Test: `app/test/widget_test.dart`

- [ ] **Step 1: Write failing widget tests**

Add these tests to `widget_test.dart`:

```dart
testWidgets('active room game confirms before exiting',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await tester.pumpWidget(MaterialApp(
    home: GamePage(
      initialGame: playGame,
      api: api,
      playerId: 'fake-player',
      roomId: 'room-1',
    ),
  ));

  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();

  expect(find.text('游戏正在进行'), findsOneWidget);
  expect(find.text('继续游戏'), findsOneWidget);
  expect(find.text('退出'), findsOneWidget);
});

testWidgets('canceling active room game exit stays on game page',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await tester.pumpWidget(MaterialApp(
    home: GamePage(
      initialGame: playGame,
      api: api,
      playerId: 'fake-player',
      roomId: 'room-1',
    ),
  ));

  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();
  await tester.tap(find.text('继续游戏'));
  await tester.pumpAndSettle();

  expect(find.text('游戏正在进行'), findsNothing);
  expect(find.byType(GamePage), findsOneWidget);
  expect(api.leavePlayingRoomCalls, 0);
});

testWidgets('confirming active room game exit calls API and returns',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  Object? poppedResult;
  await tester.pumpWidget(MaterialApp(
    home: Builder(
      builder: (context) => FilledButton(
        onPressed: () async {
          poppedResult = await Navigator.of(context).push<String>(
            MaterialPageRoute(
              builder: (_) => GamePage(
                initialGame: playGame,
                api: api,
                playerId: 'fake-player',
                roomId: 'room-1',
              ),
            ),
          );
        },
        child: const Text('open'),
      ),
    ),
  ));
  await tester.tap(find.text('open'));
  await tester.pumpAndSettle();

  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();
  await tester.tap(find.text('退出'));
  await tester.pumpAndSettle();

  expect(api.leavePlayingRoomCalls, 1);
  expect(api.leftPlayingRoomPlayerIds, ['fake-player']);
  expect(poppedResult, playGame.id);
});

testWidgets('active room game exit failure stays and shows error',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame, leavePlayingRoomError: 'exit failed');
  await tester.pumpWidget(MaterialApp(
    home: GamePage(
      initialGame: playGame,
      api: api,
      playerId: 'fake-player',
      roomId: 'room-1',
    ),
  ));

  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();
  await tester.tap(find.text('退出'));
  await tester.pumpAndSettle();

  expect(find.text('exit failed'), findsOneWidget);
  expect(find.byType(GamePage), findsOneWidget);
});

testWidgets('finished room game returns without in-progress confirmation',
    (WidgetTester tester) async {
  final api = FakeApiClient(finishedGame);
  await tester.pumpWidget(MaterialApp(
    home: GamePage(
      initialGame: finishedGame,
      api: api,
      playerId: 'fake-player',
      roomId: 'room-1',
    ),
  ));

  await tester.tap(find.byIcon(Icons.arrow_back));
  await tester.pumpAndSettle();

  expect(find.text('游戏正在进行'), findsNothing);
  expect(api.leavePlayingRoomCalls, 0);
});
```

Extend the widget-test `FakeApiClient` with:

```dart
final String? leavePlayingRoomError;
int leavePlayingRoomCalls = 0;
final List<String> leftPlayingRoomPlayerIds = [];
```

Add `this.leavePlayingRoomError,` to the constructor parameter list.

Implement:

```dart
@override
Future<RoomStateModel> leavePlayingRoom(String roomId, String playerId) async {
  leavePlayingRoomCalls += 1;
  leftPlayingRoomPlayerIds.add(playerId);
  if (leavePlayingRoomError != null) {
    throw Exception(leavePlayingRoomError);
  }
  return RoomStateModel(
    roomId: roomId,
    phase: 'PLAYING',
    ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
    version: roomVersion + 1,
    seats: const {
      'SOUTH': SeatInfo(isBot: true, displayName: '人机'),
      'WEST': SeatInfo(isBot: true, displayName: '人机'),
      'NORTH': SeatInfo(isBot: true, displayName: '人机'),
      'EAST': SeatInfo(isBot: true, displayName: '人机'),
    },
  );
}
```

- [ ] **Step 2: Run widget tests to verify they fail**

Run:

```powershell
cd app
flutter test test/widget_test.dart --plain-name "active room game confirms before exiting"
```

Expected: FAIL because tapping the back icon exits immediately without a dialog.

- [ ] **Step 3: Add confirmation and exit API call**

In `_GamePageState`, add:

```dart
bool get _isActiveRoomGame =>
    widget.isRoomMode && game != null && game!.phase != 'FINISHED';

Future<void> _handleRoomExit() async {
  final current = game;
  if (current == null) return;
  if (!_isActiveRoomGame) {
    Navigator.of(context).pop(current.id);
    return;
  }
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('游戏正在进行'),
      content: const Text('现在退出后，你的座位将由系统托管，其他玩家可以继续游戏。确定要退出吗？'),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('继续游戏'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('退出'),
        ),
      ],
    ),
  );
  if (confirmed != true || !mounted) return;
  setState(() {
    loading = true;
    error = null;
  });
  try {
    await api.leavePlayingRoom(widget.roomId!, widget.playerId!);
    if (mounted) Navigator.of(context).pop(current.id);
  } catch (e) {
    if (mounted) setState(() => error = AppError.fromException(e));
  } finally {
    if (mounted) setState(() => loading = false);
  }
}
```

Update the AppBar action:

```dart
onPressed: loading
    ? null
    : widget.isRoomMode
        ? _handleRoomExit
        : () => _run(api.createGame),
```

Update the finished-game room button:

```dart
onPressed: loading ? null : _handleRoomExit,
```

- [ ] **Step 4: Run widget tests to verify they pass**

Run:

```powershell
cd app
flutter test test/widget_test.dart --plain-name "active room game confirms before exiting"
flutter test test/widget_test.dart --plain-name "canceling active room game exit stays on game page"
flutter test test/widget_test.dart --plain-name "confirming active room game exit calls API and returns"
flutter test test/widget_test.dart --plain-name "active room game exit failure stays and shows error"
flutter test test/widget_test.dart --plain-name "finished room game returns without in-progress confirmation"
```

Expected: all listed tests pass.

- [ ] **Step 5: Commit Task 4**

```powershell
git add app/lib/game_page.dart app/test/widget_test.dart
git commit -m "Confirm active room game exits"
```

### Task 5: Final Verification

**Files:**
- Verify all changed files.

- [ ] **Step 1: Run backend focused tests**

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd server
mvn -Dtest=RoomServiceTest,RoomControllerTest test
```

Expected: PASS.

- [ ] **Step 2: Run Flutter focused tests**

```powershell
cd app
flutter test test/api_client_test.dart test/widget_test.dart
```

Expected: PASS.

- [ ] **Step 3: Run full verification with JDK 17**

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\scripts\verify-all.ps1
```

Expected: backend and app verification pass. If it still prints `Using existing JAVA_HOME: D:\Java\jdk`, stop and rerun from a shell where `JAVA_HOME` is set to `D:\Java\jdk17`.

- [ ] **Step 4: Inspect final diff**

```powershell
git status --short
git diff --stat HEAD
```

Expected: only files listed in this plan are modified.

- [ ] **Step 5: Commit final fixes if any**

```powershell
git add server app docs/superpowers/plans/2026-07-16-in-game-room-exit.md
git commit -m "Complete in-game room exit flow"
```

Skip this commit if all implementation tasks were already committed and only the plan file is uncommitted; in that case commit only the plan:

```powershell
git add docs/superpowers/plans/2026-07-16-in-game-room-exit.md
git commit -m "Add in-game room exit implementation plan"
```

## Self-Review

Spec coverage:

- Confirmation prompt: Task 4.
- Seat becomes system-controlled: Task 1 and Task 2.
- Room remains while humans remain: Task 1.
- Room deletion when no humans remain: Task 1.
- Owner transfer: Task 1.
- Departed player cannot keep acting: Task 1.
- API client and UI integration: Task 3 and Task 4.

Marker scan: no incomplete-work markers or open-ended implementation instructions remain.

Type consistency: `leavePlayingRoom(String roomId, String playerId)` is used consistently in `RoomService`, `RoomController`, `GameApi`, `ApiClient`, and Flutter fakes.
