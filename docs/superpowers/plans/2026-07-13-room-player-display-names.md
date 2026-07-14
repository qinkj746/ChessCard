# Room Player Display Names Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render current profile display names and every occupied room seat's display name while retaining player IDs for all room and game operations.

**Architecture:** Enrich `RoomStateDto.SeatInfo` at the controller boundary by resolving player profiles through `PlayerService`, with the stored player ID as a safe fallback. Parse the additional field in Flutter, preserve the complete active identity during room initialization, and render names while continuing to compare and submit IDs.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Dart, Flutter, flutter_test

---

### Task 1: Enrich Backend Room Seat DTOs

**Files:**
- Modify: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`

- [ ] **Step 1: Write failing controller tests for resolved and fallback names**

Add `PlayerProfile`, `PlayerRepository`, `PlayerService`, and `Instant` imports. Add a fake player repository and pass its service to the controller:

```java
private final FakePlayerRepository playerRepo = new FakePlayerRepository();
private final PlayerService playerService = new PlayerService(playerRepo);
private final RoomController controller = new RoomController(service, playerService);
```

Add these tests:

```java
@Test
void createReturnsSeatDisplayNameFromPlayerProfile() {
    playerRepo.save(new PlayerProfile(
            "player-1", "Alice", false, "token-1", Instant.now()));

    RoomStateDto dto = controller.create(new CreateRoomRequest("player-1"));

    assertThat(dto.seats().get("SOUTH").playerId()).isEqualTo("player-1");
    assertThat(dto.seats().get("SOUTH").displayName()).isEqualTo("Alice");
}

@Test
void createFallsBackToPlayerIdWhenProfileIsMissing() {
    RoomStateDto dto = controller.create(new CreateRoomRequest("stale-player"));

    assertThat(dto.seats().get("SOUTH").displayName())
            .isEqualTo("stale-player");
}
```

Add the repository fake:

```java
private static class FakePlayerRepository implements PlayerRepository {
    final Map<String, PlayerProfile> store = new HashMap<>();

    @Override
    public PlayerProfile save(PlayerProfile profile) {
        store.put(profile.getPlayerId(), profile);
        return profile;
    }

    @Override
    public Optional<PlayerProfile> find(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

- [ ] **Step 2: Run the backend test and verify RED**

Run:

```powershell
mvn -f server/pom.xml -Dtest=RoomControllerTest test
```

Expected: compilation fails because `RoomController` has no two-argument constructor and `SeatInfo` has no `displayName()` accessor.

- [ ] **Step 3: Add display names to the backend DTO and controller mapping**

Change `RoomStateDto.SeatInfo` and its mapper to accept a resolver:

```java
public record SeatInfo(String playerId, String displayName) {
}

public static RoomStateDto from(
        RoomState room,
        Function<String, String> displayNameResolver
) {
    Map<String, SeatInfo> seatMap = room.getSeats().entrySet().stream()
            .collect(Collectors.toMap(
                    entry -> entry.getKey().name(),
                    entry -> {
                        String playerId = entry.getValue().getPlayerId();
                        return new SeatInfo(
                                playerId,
                                displayNameResolver.apply(playerId)
                        );
                    }
            ));
    return new RoomStateDto(
            room.getRoomId(),
            room.getPhase().name(),
            room.getOwnerPlayerId(),
            room.getGameId(),
            room.getVersion(),
            seatMap
    );
}
```

Import `java.util.function.Function`. Update `RoomController` to receive `PlayerService`, route create/get/join/leave results through `toDto`, and use the ID fallback:

```java
private final RoomService service;
private final PlayerService playerService;

public RoomController(RoomService service, PlayerService playerService) {
    this.service = service;
    this.playerService = playerService;
}

private RoomStateDto toDto(RoomState room) {
    return RoomStateDto.from(room, this::displayNameFor);
}

private String displayNameFor(String playerId) {
    try {
        String displayName = playerService.getPlayer(playerId).getDisplayName();
        return displayName == null || displayName.isBlank()
                ? playerId
                : displayName;
    } catch (IllegalArgumentException ignored) {
        return playerId;
    }
}
```

Replace each of the four existing `RoomStateDto.from(...)` calls with `toDto(...)`. Leave `start()` unchanged because it returns `GameStateDto`.

- [ ] **Step 4: Run the backend test and verify GREEN**

Run:

```powershell
mvn -f server/pom.xml -Dtest=RoomControllerTest test
```

Expected: all `RoomControllerTest` tests pass.

### Task 2: Parse Room Seat Display Names in Flutter

**Files:**
- Modify: `app/test/room_models_test.dart`
- Modify: `app/lib/models.dart`

- [ ] **Step 1: Write failing model tests for supplied and missing names**

Add two tests:

```dart
test('parses room seat display name', () {
  final room = RoomStateModel.fromJson({
    'roomId': 'room-1',
    'phase': 'WAITING',
    'ownerPlayerId': 'player-1',
    'gameId': null,
    'version': 1,
    'seats': {
      'SOUTH': {'playerId': 'player-1', 'displayName': 'Alice'},
    },
  });

  expect(room.seats['SOUTH']?.displayName, 'Alice');
  expect(room.seats['SOUTH']?.playerId, 'player-1');
});

test('room seat falls back to player id when display name is missing', () {
  final room = RoomStateModel.fromJson({
    'roomId': 'room-1',
    'phase': 'WAITING',
    'ownerPlayerId': 'legacy-player',
    'gameId': null,
    'version': 1,
    'seats': {
      'SOUTH': {'playerId': 'legacy-player'},
    },
  });

  expect(room.seats['SOUTH']?.displayName, 'legacy-player');
});
```

- [ ] **Step 2: Run the model test and verify RED**

Run:

```powershell
flutter test test/room_models_test.dart
```

from `app/`.

Expected: compilation fails because `SeatInfo` has no `displayName` getter.

- [ ] **Step 3: Implement compatible SeatInfo parsing**

Replace `SeatInfo` with:

```dart
class SeatInfo {
  const SeatInfo({required this.playerId, String? displayName})
      : _displayName = displayName;

  final String playerId;
  final String? _displayName;

  String get displayName {
    final value = _displayName?.trim();
    return value == null || value.isEmpty ? playerId : value;
  }

  factory SeatInfo.fromJson(Map<String, dynamic> json) {
    return SeatInfo(
      playerId: json['playerId'] as String,
      displayName: json['displayName'] as String?,
    );
  }
}
```

The optional constructor parameter keeps existing tests and in-memory callers source compatible.

- [ ] **Step 4: Run the model test and verify GREEN**

Run:

```powershell
flutter test test/room_models_test.dart
```

from `app/`.

Expected: all room model tests pass.

### Task 3: Render Active and Seated Player Names

**Files:**
- Modify: `app/test/widget_test.dart`
- Modify: `app/lib/auth_controller.dart`
- Modify: `app/lib/room_page.dart`

- [ ] **Step 1: Write failing widget tests for pre-room and multi-seat names**

Extend `FakeApiClient` with configurable room seats:

```dart
Map<String, SeatInfo>? createdRoomSeats;
```

Use it from `createRoom`:

```dart
seats: createdRoomSeats ?? {'SOUTH': SeatInfo(playerId: playerId)},
```

Replace the existing restored-account room test with assertions before and after creation:

```dart
testWidgets('room page renders account and seated player display names',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame)
    ..createdRoomSeats = const {
      'SOUTH': SeatInfo(playerId: 'account-1', displayName: 'Alice'),
      'NORTH': SeatInfo(playerId: 'account-2', displayName: 'Bob'),
    };
  final controller = AuthController(
    api: api,
    storage: MemoryAuthSessionStorage(const AuthSessionModel(
      playerId: 'account-1',
      username: 'alice',
      displayName: 'Alice',
      sessionToken: 'token-1',
    )),
  );
  await controller.initialize();

  await tester.pumpWidget(MaterialApp(home: RoomPage(auth: controller)));
  await tester.pumpAndSettle();

  expect(find.text('玩家: Alice'), findsOneWidget);
  expect(find.textContaining('account-1'), findsNothing);

  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();

  expect(find.text('Alice'), findsOneWidget);
  expect(find.text('Bob'), findsOneWidget);
  expect(find.textContaining('account-1'), findsNothing);
  expect(find.textContaining('account-2'), findsNothing);
  expect(api.createdRoomPlayerIds, ['account-1']);
  expect(api.guestCreateCalls, 0);
});
```

- [ ] **Step 2: Run the widget test and verify RED**

Run:

```powershell
flutter test test/widget_test.dart --plain-name "room page renders account and seated player display names"
```

from `app/`.

Expected: the pre-room assertion fails because the page renders `玩家: account-1`.

- [ ] **Step 3: Preserve and expose the active display name**

Add this getter to `AuthController`:

```dart
String? get displayName =>
    session?.displayName ?? _guestProfile?.displayName;
```

Add room state:

```dart
String? playerDisplayName;
```

Replace `_initPlayer` identity extraction with:

```dart
final String id;
final String displayName;
if (widget.auth != null) {
  id = await widget.auth!.ensurePlayerId();
  displayName = widget.auth!.displayName ?? id;
} else {
  final profile = await api.createGuestPlayer();
  id = profile.playerId;
  displayName = profile.displayName;
}
if (mounted) {
  setState(() {
    playerId = id;
    playerDisplayName = displayName;
  });
}
```

Render `playerDisplayName ?? playerId` in both current-player labels. Render `seatInfo.displayName` in `_seatCard`:

```dart
Text('玩家: ${playerDisplayName ?? playerId}', ...)
```

```dart
occupied ? seatInfo.displayName : '空位'
```

- [ ] **Step 4: Run the focused widget test and verify GREEN**

Run:

```powershell
flutter test test/widget_test.dart --plain-name "room page renders account and seated player display names"
```

from `app/`.

Expected: the focused widget test passes.

### Task 4: Format and Verify the Complete Change

**Files:**
- Verify all files modified in Tasks 1-3.

- [ ] **Step 1: Format changed source and test files**

Run from the repository root:

```powershell
dart format app/lib/models.dart app/lib/auth_controller.dart app/lib/room_page.dart app/test/room_models_test.dart app/test/widget_test.dart
```

Expected: formatter completes successfully.

- [ ] **Step 2: Run focused backend and Flutter tests**

Run:

```powershell
mvn -f server/pom.xml -Dtest=RoomControllerTest test
flutter test app/test/room_models_test.dart
flutter test app/test/widget_test.dart
```

Expected: every command exits with code 0 and reports no failed tests.

- [ ] **Step 3: Run static analysis**

Run from `app/`:

```powershell
flutter analyze
```

Expected: `No issues found!`.

- [ ] **Step 4: Review the final diff**

Run:

```powershell
git diff --check
git diff -- server/src/main/java/com/chesscard/shengji/api/RoomController.java server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java app/lib/models.dart app/lib/auth_controller.dart app/lib/room_page.dart app/test/room_models_test.dart app/test/widget_test.dart
```

Expected: `git diff --check` prints nothing; the diff contains only display-name transport, rendering, and regression tests. The pre-existing `server/src/main/resources/application-dev.yml` change remains untouched.
