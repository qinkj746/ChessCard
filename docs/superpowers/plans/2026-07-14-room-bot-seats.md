# Room Bot Seats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a room owner add or remove a bot on each empty seat and start only when all four seats are occupied by humans or bots.

**Architecture:** Add an explicit bot marker to `RoomSeat`, persist it in the existing JSON room snapshot, and expose owner-only bot seat mutations through REST. Bot seats remain room occupants but map to null `GameState.seatOwners`, preserving the existing AI execution path; Flutter receives `isBot`, renders per-seat controls, and disables start until the room is full.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Jackson, JUnit 5, AssertJ, Flutter/Dart, Material widgets, `package:http`, Flutter test.

---

## File Map

- `server/src/main/java/com/chesscard/shengji/domain/RoomSeat.java`: represent either a human or bot occupant and preserve legacy JSON compatibility.
- `server/src/main/java/com/chesscard/shengji/service/RoomService.java`: enforce owner-only bot mutations and four-seat start validation.
- `server/src/main/java/com/chesscard/shengji/service/GameService.java`: map bot room occupants to existing AI-controlled null game owners.
- `server/src/main/java/com/chesscard/shengji/api/RoomController.java`: publish bot add/remove routes.
- `server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java`: serialize human and bot seat information without resolving a bot player profile.
- `server/src/test/java/com/chesscard/shengji/persistence/RoomStateSerializationTest.java`: protect snapshot and legacy deserialization behavior.
- `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`: cover permissions, conflicts, removal, and full-room start rules.
- `server/src/test/java/com/chesscard/shengji/service/GameServiceCreateTest.java`: prove bot seats use the current AI owner signal.
- `server/src/test/java/com/chesscard/shengji/service/RoomEventPublishingTest.java`: keep room event expectations valid under the full-room rule.
- `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`: cover routes and DTO fields.
- `app/lib/models.dart`: parse nullable bot player IDs and expose bot display behavior.
- `app/lib/api_client.dart`: add bot seat methods to `GameApi` and `ApiClient`.
- `app/lib/room_page.dart`: render and execute per-seat bot controls and gate start.
- `app/test/room_models_test.dart`: cover bot and legacy human room payloads.
- `app/test/api_client_test.dart`: verify bot route methods, bodies, and response decoding.
- `app/test/widget_test.dart`: cover owner/non-owner controls, add/remove behavior, and full-room start state.

### Task 1: Persist an Explicit Bot Seat

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/domain/RoomSeat.java`
- Create: `server/src/test/java/com/chesscard/shengji/persistence/RoomStateSerializationTest.java`

- [ ] **Step 1: Write failing snapshot tests**

Create `RoomStateSerializationTest.java` with bot round-trip and legacy-seat coverage:

```java
package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomSeat;
import com.chesscard.shengji.domain.RoomState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RoomStateSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void roomSnapshotRoundTripPreservesHumanAndBotSeats() throws Exception {
        RoomState room = RoomState.create("owner-1");
        room.getSeats().put(PlayerSeat.WEST, RoomSeat.bot(PlayerSeat.WEST, Instant.parse("2026-07-14T10:00:00Z")));

        RoomState restored = mapper.readValue(mapper.writeValueAsString(room), RoomState.class);

        assertThat(restored.getSeats().get(PlayerSeat.SOUTH).isBot()).isFalse();
        assertThat(restored.getSeats().get(PlayerSeat.SOUTH).getPlayerId()).isEqualTo("owner-1");
        assertThat(restored.getSeats().get(PlayerSeat.WEST).isBot()).isTrue();
        assertThat(restored.getSeats().get(PlayerSeat.WEST).getPlayerId()).isNull();
    }

    @Test
    void legacySeatWithoutBotMarkerDeserializesAsHuman() throws Exception {
        String json = """
                {"seat":"NORTH","playerId":"player-2","joinedAt":"2026-07-14T10:00:00Z"}
                """;

        RoomSeat seat = mapper.readValue(json, RoomSeat.class);

        assertThat(seat.isBot()).isFalse();
        assertThat(seat.getPlayerId()).isEqualTo("player-2");
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomStateSerializationTest test
Pop-Location
```

Expected: compilation fails because `RoomSeat.bot(...)` and `RoomSeat.isBot()` do not exist.

- [ ] **Step 3: Add the minimal domain representation**

Add a `bot` field, accessors, and factory to `RoomSeat` while leaving the existing human constructor compatible:

```java
private boolean bot;

public RoomSeat(PlayerSeat seat, String playerId, Instant joinedAt) {
    this.seat = seat;
    this.playerId = playerId;
    this.joinedAt = joinedAt;
    this.bot = false;
}

public static RoomSeat bot(PlayerSeat seat, Instant joinedAt) {
    RoomSeat roomSeat = new RoomSeat();
    roomSeat.seat = seat;
    roomSeat.joinedAt = joinedAt;
    roomSeat.bot = true;
    return roomSeat;
}

public boolean isBot() {
    return bot;
}

public void setBot(boolean bot) {
    this.bot = bot;
}
```

The primitive defaults to `false` when old snapshots omit it. Do not generate a fake `playerId` for bots.

- [ ] **Step 4: Run the snapshot tests and verify GREEN**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomStateSerializationTest test
Pop-Location
```

Expected: 2 tests pass.

- [ ] **Step 5: Commit the domain increment**

```powershell
git add server/src/main/java/com/chesscard/shengji/domain/RoomSeat.java server/src/test/java/com/chesscard/shengji/persistence/RoomStateSerializationTest.java
git commit -m "feat: represent bot room seats"
```

### Task 2: Enforce Bot Management and Full-Room Start Rules

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Modify: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`
- Modify: `server/src/test/java/com/chesscard/shengji/service/RoomEventPublishingTest.java`

- [ ] **Step 1: Write failing room service tests**

Add imports for `PermissionDeniedException` and append focused tests to `RoomServiceTest`:

```java
@Test
void ownerAddsAndRemovesBotFromSelectedSeat() {
    RoomState room = service.createRoom("player-1");

    RoomState withBot = service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

    assertThat(withBot.getSeats().get(PlayerSeat.WEST).isBot()).isTrue();
    assertThat(withBot.getSeats().get(PlayerSeat.WEST).getPlayerId()).isNull();
    assertThat(withBot.getVersion()).isEqualTo(2);

    RoomState withoutBot = service.removeBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
    assertThat(withoutBot.getSeats()).doesNotContainKey(PlayerSeat.WEST);
    assertThat(withoutBot.getVersion()).isEqualTo(3);
}

@Test
void botManagementRequiresOwnerWaitingRoomAndCorrectOccupancy() {
    RoomState room = service.createRoom("player-1");

    assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-2", PlayerSeat.WEST))
            .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-1", PlayerSeat.SOUTH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("占用");
    assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", PlayerSeat.NORTH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("人机");
    assertThatThrownBy(() -> service.removeBot(room.getRoomId(), "player-1", PlayerSeat.SOUTH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("人机");

    room.setPhase(RoomPhase.PLAYING);
    assertThatThrownBy(() -> service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("等待");
}

@Test
void humanCannotJoinOrLeaveThroughBotSeat() {
    RoomState room = service.createRoom("player-1");
    service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);

    assertThatThrownBy(() -> service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.WEST))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("占用");
    assertThatThrownBy(() -> service.leaveSeat(room.getRoomId(), "player-2", PlayerSeat.WEST))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("自己");
}

@Test
void startGameRequiresAllFourSeats() {
    RoomState room = service.createRoom("player-1");

    assertThatThrownBy(() -> service.startGame(room.getRoomId(), "player-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("4");
}
```

Update `startGameCreatesGameAndMovesRoomToPlaying` so the room is full before starting:

```java
RoomState room = service.createRoom("player-1");
service.joinSeat(room.getRoomId(), "player-2", PlayerSeat.NORTH);
service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);

GameState game = service.startGame(room.getRoomId(), "player-1");

assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.SOUTH, "player-1");
assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.NORTH, "player-2");
assertThat(game.getSeatOwners().get(PlayerSeat.WEST)).isNull();
assertThat(game.getSeatOwners().get(PlayerSeat.EAST)).isNull();
assertThat(service.getRoom(room.getRoomId()).getVersion()).isEqualTo(5);
```

- [ ] **Step 2: Run the service tests and verify RED**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomServiceTest test
Pop-Location
```

Expected: compilation fails because `addBot` and `removeBot` are missing; after signatures exist, start validation tests still fail until the rules are implemented.

- [ ] **Step 3: Implement owner-only bot operations and start validation**

Add these operations and helper to `RoomService`:

```java
public RoomState addBot(String roomId, String playerId, PlayerSeat seat) {
    RoomState room = requireOwnerWaitingRoom(roomId, playerId);
    if (seat == null) {
        throw new IllegalArgumentException("seat 不能为空");
    }
    if (room.getSeats().containsKey(seat)) {
        throw new IllegalArgumentException("座位已被占用: " + seat.name());
    }
    room.getSeats().put(seat, RoomSeat.bot(seat, Instant.now()));
    room.touch();
    return saveAndPublish(room);
}

public RoomState removeBot(String roomId, String playerId, PlayerSeat seat) {
    RoomState room = requireOwnerWaitingRoom(roomId, playerId);
    if (seat == null) {
        throw new IllegalArgumentException("seat 不能为空");
    }
    RoomSeat existing = room.getSeats().get(seat);
    if (existing == null || !existing.isBot()) {
        throw new IllegalArgumentException("该座位不是人机");
    }
    room.getSeats().remove(seat);
    room.touch();
    return saveAndPublish(room);
}

private RoomState requireOwnerWaitingRoom(String roomId, String playerId) {
    if (playerId == null || playerId.isBlank()) {
        throw new IllegalArgumentException("playerId 不能为空");
    }
    RoomState room = getRoom(roomId);
    if (room.getPhase() != RoomPhase.WAITING) {
        throw new IllegalArgumentException("房间不在等待状态，无法管理人机");
    }
    if (!room.getOwnerPlayerId().equals(playerId)) {
        throw new PermissionDeniedException("只有房主才能管理人机");
    }
    return room;
}
```

Make existing human comparisons null-safe:

```java
if (room.getSeats().values().stream().anyMatch(s -> playerId.equals(s.getPlayerId()))) {
    throw new IllegalArgumentException("该玩家已入座其他座位");
}

if (!playerId.equals(existing.getPlayerId())) {
    throw new IllegalArgumentException("只能离开自己占用的座位");
}
```

In `startGame`, retain the owner check before adding the authoritative full-room guard:

```java
if (room.getSeats().size() != PlayerSeat.values().length) {
    throw new IllegalArgumentException("需要 4 个座位全部有人或人机才能开局");
}
```

- [ ] **Step 4: Update the room event test fixture**

In `startGamePublishesRoomAndGameUpdatedEvents`, fill the three empty seats before clearing recorded events and starting:

```java
RoomState room = service.createRoom("player-1");
service.addBot(room.getRoomId(), "player-1", PlayerSeat.WEST);
service.addBot(room.getRoomId(), "player-1", PlayerSeat.NORTH);
service.addBot(room.getRoomId(), "player-1", PlayerSeat.EAST);
publisher.events.clear();

GameState game = service.startGame(room.getRoomId(), "player-1");
```

Change the final room event version assertion to `5L`.

- [ ] **Step 5: Run service and event tests and verify GREEN**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomServiceTest,RoomEventPublishingTest test
Pop-Location
```

Expected: all tests in both classes pass.

- [ ] **Step 6: Commit the room rules**

```powershell
git add server/src/main/java/com/chesscard/shengji/service/RoomService.java server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java server/src/test/java/com/chesscard/shengji/service/RoomEventPublishingTest.java
git commit -m "feat: manage bot seats in rooms"
```

### Task 3: Map Bot Occupants to Existing AI Seats

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/test/java/com/chesscard/shengji/service/GameServiceCreateTest.java`

- [ ] **Step 1: Write the failing room-game mapping test**

Add `RoomSeat`, `Instant`, and `EnumMap` imports, then append:

```java
@Test
void createGameForRoomMapsBotsToAiOwners() {
    GameService service = new GameService(new FakeGameRepository(), new AiPlayer());
    Map<PlayerSeat, RoomSeat> seats = new EnumMap<>(PlayerSeat.class);
    seats.put(PlayerSeat.SOUTH, new RoomSeat(PlayerSeat.SOUTH, "player-1", Instant.now()));
    RoomSeat westBot = RoomSeat.bot(PlayerSeat.WEST, Instant.now());
    westBot.setPlayerId("stale-bot-id");
    seats.put(PlayerSeat.WEST, westBot);
    seats.put(PlayerSeat.NORTH, new RoomSeat(PlayerSeat.NORTH, "player-2", Instant.now()));
    seats.put(PlayerSeat.EAST, RoomSeat.bot(PlayerSeat.EAST, Instant.now()));

    GameState game = service.createGameForRoom("room-1", seats);

    assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.SOUTH, "player-1");
    assertThat(game.getSeatOwners()).containsEntry(PlayerSeat.NORTH, "player-2");
    assertThat(game.getSeatOwners().get(PlayerSeat.WEST)).isNull();
    assertThat(game.getSeatOwners().get(PlayerSeat.EAST)).isNull();
}
```

- [ ] **Step 2: Run the mapping test and verify RED**

Run:

```powershell
Push-Location server
mvn '-Dtest=GameServiceCreateTest#createGameForRoomMapsBotsToAiOwners' test
Pop-Location
```

Expected: FAIL because WEST is assigned `stale-bot-id`; the current implementation copies `playerId` without checking the explicit bot marker.

- [ ] **Step 3: Implement the explicit mapping**

Replace the seat-owner assignment inside `createGameForRoom` with:

```java
RoomSeat roomSeat = roomSeats.get(seat);
String playerId = roomSeat != null && !roomSeat.isBot()
        ? roomSeat.getPlayerId()
        : null;
game.getSeatOwners().put(seat, playerId);
```

- [ ] **Step 4: Run game creation and permission tests**

Run:

```powershell
Push-Location server
mvn -Dtest=GameServiceCreateTest,GameServicePermissionTest test
Pop-Location
```

Expected: all tests pass, proving human permissions and the null-owner AI signal remain intact.

- [ ] **Step 5: Commit the game mapping**

```powershell
git add server/src/main/java/com/chesscard/shengji/service/GameService.java server/src/test/java/com/chesscard/shengji/service/GameServiceCreateTest.java
git commit -m "feat: map room bots to AI seats"
```

### Task 4: Expose Bot Seat REST and DTO Contracts

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java`
- Modify: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`

- [ ] **Step 1: Write failing controller and DTO tests**

Append to `RoomControllerTest`:

```java
@Test
void ownerAddsAndRemovesBotThroughController() {
    RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

    RoomStateDto withBot = controller.addBot(
            created.roomId(), "west", new JoinSeatRequest("player-1"));

    assertThat(withBot.seats().get("WEST").isBot()).isTrue();
    assertThat(withBot.seats().get("WEST").playerId()).isNull();
    assertThat(withBot.seats().get("WEST").displayName()).isEqualTo("人机");

    RoomStateDto withoutBot = controller.removeBot(
            created.roomId(), "west", new JoinSeatRequest("player-1"));
    assertThat(withoutBot.seats()).doesNotContainKey("WEST");
}

@Test
void botEndpointRejectsNonOwner() {
    RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));

    assertThatThrownBy(() -> controller.addBot(
            created.roomId(), "west", new JoinSeatRequest("player-2")))
            .isInstanceOf(PermissionDeniedException.class);
}
```

Update existing human DTO assertions to include `assertThat(dto.seats().get("SOUTH").isBot()).isFalse();`.

- [ ] **Step 2: Run controller tests and verify RED**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomControllerTest test
Pop-Location
```

Expected: compilation fails because controller methods and `SeatInfo.isBot()` do not exist.

- [ ] **Step 3: Extend the room DTO without resolving bot profiles**

Change the nested record and mapping in `RoomStateDto`:

```java
public record SeatInfo(String playerId, String displayName, boolean isBot) {
}
```

```java
RoomSeat roomSeat = e.getValue();
if (roomSeat.isBot()) {
    return new SeatInfo(null, "人机", true);
}
String playerId = roomSeat.getPlayerId();
return new SeatInfo(playerId, displayNameResolver.apply(playerId), false);
```

Add `RoomSeat` to the DTO imports.

- [ ] **Step 4: Add bot routes to `RoomController`**

Reuse `JoinSeatRequest` and the existing `parseSeat` helper:

```java
@PostMapping("/{id}/seats/{seat}/bot")
public RoomStateDto addBot(
        @PathVariable String id,
        @PathVariable String seat,
        @RequestBody JoinSeatRequest request
) {
    requirePlayerRequest(request);
    return toDto(service.addBot(id, request.playerId(), parseSeat(seat)));
}

@DeleteMapping("/{id}/seats/{seat}/bot")
public RoomStateDto removeBot(
        @PathVariable String id,
        @PathVariable String seat,
        @RequestBody JoinSeatRequest request
) {
    requirePlayerRequest(request);
    return toDto(service.removeBot(id, request.playerId(), parseSeat(seat)));
}

private void requirePlayerRequest(JoinSeatRequest request) {
    if (request == null || request.playerId() == null || request.playerId().isBlank()) {
        throw new IllegalArgumentException("playerId 不能为空");
    }
}
```

Replace the repeated request validation in `joinSeat`, `leaveSeat`, and `start` with `requirePlayerRequest(request)` so all room actor requests share the same contract.

- [ ] **Step 5: Run controller tests and verify GREEN**

Run:

```powershell
Push-Location server
mvn -Dtest=RoomControllerTest test
Pop-Location
```

Expected: all controller tests pass.

- [ ] **Step 6: Commit the REST contract**

```powershell
git add server/src/main/java/com/chesscard/shengji/api/RoomController.java server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java
git commit -m "feat: expose room bot seat API"
```

### Task 5: Parse Bot Seats and Call Bot APIs in Flutter

**Files:**
- Modify: `app/lib/models.dart`
- Modify: `app/lib/api_client.dart`
- Modify: `app/test/room_models_test.dart`
- Modify: `app/test/api_client_test.dart`

- [ ] **Step 1: Write failing Flutter model tests**

Append to `room_models_test.dart`:

```dart
test('parses bot seat without a player id', () {
  final room = RoomStateModel.fromJson({
    'roomId': 'room-1',
    'phase': 'WAITING',
    'ownerPlayerId': 'player-1',
    'gameId': null,
    'version': 2,
    'seats': {
      'SOUTH': {
        'playerId': 'player-1',
        'displayName': 'Alice',
        'isBot': false,
      },
      'WEST': {
        'playerId': null,
        'displayName': '人机',
        'isBot': true,
      },
    },
  });

  expect(room.seats['WEST']!.isBot, isTrue);
  expect(room.seats['WEST']!.playerId, isNull);
  expect(room.seats['WEST']!.displayName, '人机');
  expect(room.seats['SOUTH']!.isBot, isFalse);
});

test('legacy human seat defaults isBot to false', () {
  final seat = SeatInfo.fromJson({
    'playerId': 'legacy-player',
    'displayName': 'Legacy',
  });

  expect(seat.isBot, isFalse);
  expect(seat.displayName, 'Legacy');
});
```

- [ ] **Step 2: Run model tests and verify RED**

Run:

```powershell
Push-Location app
flutter test test/room_models_test.dart
Pop-Location
```

Expected: FAIL because `SeatInfo` requires a string player ID and has no `isBot` field.

- [ ] **Step 3: Implement the Flutter seat model**

Replace `SeatInfo` with:

```dart
class SeatInfo {
  const SeatInfo({this.playerId, String? displayName, this.isBot = false})
      : _displayName = displayName;

  final String? playerId;
  final String? _displayName;
  final bool isBot;

  String get displayName {
    if (isBot) return '人机';
    final value = _displayName?.trim();
    return value == null || value.isEmpty ? (playerId ?? '') : value;
  }

  factory SeatInfo.fromJson(Map<String, dynamic> json) {
    return SeatInfo(
      playerId: json['playerId'] as String?,
      displayName: json['displayName'] as String?,
      isBot: json['isBot'] as bool? ?? false,
    );
  }
}
```

- [ ] **Step 4: Write failing API client tests**

Add a `_roomJson()` helper to `api_client_test.dart`:

```dart
String _roomJson() => jsonEncode({
      'roomId': 'room-1',
      'phase': 'WAITING',
      'ownerPlayerId': 'player-1',
      'gameId': null,
      'version': 2,
      'seats': {
        'SOUTH': {
          'playerId': 'player-1',
          'displayName': 'Alice',
          'isBot': false,
        },
        'WEST': {
          'playerId': null,
          'displayName': '人机',
          'isBot': true,
        },
      },
    });
```

Add route tests:

```dart
test('addBot posts owner to selected bot seat endpoint', () async {
  late http.Request capturedRequest;
  final client = ApiClient(
    baseUrl: 'http://example.test',
    sessionToken: 'token-abc',
    httpClient: MockClient((request) async {
      capturedRequest = request;
      return http.Response(_roomJson(), 200,
          headers: {'Content-Type': 'application/json'});
    }),
  );

  final room = await client.addBot('room-1', 'WEST', 'player-1');

  expect(capturedRequest.method, 'POST');
  expect(capturedRequest.url.path, '/api/rooms/room-1/seats/WEST/bot');
  expect(jsonDecode(capturedRequest.body), {'playerId': 'player-1'});
  expect(room.seats['WEST']!.isBot, isTrue);
});

test('removeBot deletes selected bot seat with owner body', () async {
  late http.Request capturedRequest;
  final client = ApiClient(
    baseUrl: 'http://example.test',
    sessionToken: 'token-abc',
    httpClient: MockClient((request) async {
      capturedRequest = request;
      return http.Response(_roomJson(), 200,
          headers: {'Content-Type': 'application/json'});
    }),
  );

  await client.removeBot('room-1', 'WEST', 'player-1');

  expect(capturedRequest.method, 'DELETE');
  expect(capturedRequest.url.path, '/api/rooms/room-1/seats/WEST/bot');
  expect(jsonDecode(capturedRequest.body), {'playerId': 'player-1'});
});
```

- [ ] **Step 5: Run API tests and verify RED**

Run:

```powershell
Push-Location app
flutter test test/api_client_test.dart
Pop-Location
```

Expected: compilation fails because `GameApi`, `ApiClient`, and test fakes do not implement `addBot` or `removeBot`.

- [ ] **Step 6: Add bot methods to `GameApi` and `ApiClient`**

Add interface methods:

```dart
Future<RoomStateModel> addBot(String roomId, String seat, String playerId);

Future<RoomStateModel> removeBot(String roomId, String seat, String playerId);
```

Add the implementations beside `joinSeat` and `leaveSeat`:

```dart
@override
Future<RoomStateModel> addBot(
    String roomId, String seat, String playerId) async {
  await _ensureSession();
  final response = await httpClient.post(
    Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat/bot'),
    headers: _headers(json: true),
    body: jsonEncode({'playerId': playerId}),
  );
  return _decodeRoom(response);
}

@override
Future<RoomStateModel> removeBot(
    String roomId, String seat, String playerId) async {
  await _ensureSession();
  final request = http.Request(
      'DELETE', Uri.parse('$baseUrl/api/rooms/$roomId/seats/$seat/bot'));
  request.headers.addAll(_headers(json: true));
  request.body = jsonEncode({'playerId': playerId});
  final response = await httpClient.send(request);
  return _decodeRoom(await http.Response.fromStream(response));
}
```

Keep `FakeApiClient` compilable and stateful when the interface expands. Add a mutable room seat map:

```dart
final Map<String, SeatInfo> roomSeats = {};
```

At the beginning of `createRoom`, initialize it from the configured fixture before returning the room:

```dart
roomSeats
  ..clear()
  ..addAll(createdRoomSeats ?? {'SOUTH': SeatInfo(playerId: playerId)});
```

Return `Map.unmodifiable(roomSeats)` as the response seats. Then add real fake implementations:

```dart
@override
Future<RoomStateModel> addBot(
    String roomId, String seat, String playerId) async {
  roomSeats[seat] = const SeatInfo(isBot: true, displayName: '人机');
  return RoomStateModel(
    roomId: roomId,
    phase: 'WAITING',
    ownerPlayerId: playerId,
    seats: Map.unmodifiable(roomSeats),
  );
}

@override
Future<RoomStateModel> removeBot(
    String roomId, String seat, String playerId) async {
  roomSeats.remove(seat);
  return RoomStateModel(
    roomId: roomId,
    phase: 'WAITING',
    ownerPlayerId: playerId,
    seats: Map.unmodifiable(roomSeats),
  );
}
```

- [ ] **Step 7: Run model and API tests and verify GREEN**

Run:

```powershell
Push-Location app
flutter test test/room_models_test.dart test/api_client_test.dart
Pop-Location
```

Expected: all model and API client tests pass.

- [ ] **Step 8: Commit the Flutter data contract**

```powershell
git add app/lib/models.dart app/lib/api_client.dart app/test/room_models_test.dart app/test/api_client_test.dart app/test/widget_test.dart
git commit -m "feat: add room bot client contract"
```

### Task 6: Add Per-Seat Bot Controls to the Room Page

**Files:**
- Modify: `app/lib/room_page.dart`
- Modify: `app/test/widget_test.dart`

- [ ] **Step 1: Make the room fake stateful and write failing widget tests**

Extend `FakeApiClient` with call tracking:

```dart
final List<String> addedBotSeats = [];
final List<String> removedBotSeats = [];
```

Add the tracking calls as the first statements in the existing fake methods:

```dart
addedBotSeats.add(seat);
removedBotSeats.add(seat);
```

Place `addedBotSeats.add(seat)` only in `addBot` and `removedBotSeats.add(seat)` only in `removeBot`.

Add widget tests:

```dart
testWidgets('owner can add and remove a bot on each empty seat',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await pumpRoomPage(tester, api: api);

  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();

  expect(find.text('添加人机'), findsNWidgets(3));
  await tester.tap(find.text('添加人机').first);
  await tester.pumpAndSettle();

  expect(api.addedBotSeats, ['WEST']);
  expect(find.text('人机'), findsOneWidget);
  expect(find.text('移除'), findsOneWidget);

  await tester.tap(find.text('移除'));
  await tester.pumpAndSettle();
  expect(api.removedBotSeats, ['WEST']);
  expect(find.text('添加人机'), findsNWidgets(3));
});

testWidgets('start stays disabled until all four seats are occupied',
    (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  await pumpRoomPage(tester, api: api);

  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();

  FilledButton startButton = tester.widget(
    find.widgetWithIcon(FilledButton, Icons.play_arrow),
  );
  expect(startButton.onPressed, isNull);

  for (var index = 0; index < 3; index++) {
    await tester.tap(find.text('添加人机').first);
    await tester.pumpAndSettle();
  }

  startButton = tester.widget(
    find.widgetWithIcon(FilledButton, Icons.play_arrow),
  );
  expect(startButton.onPressed, isNotNull);
});
```

Add an optional owner override to the fake constructor and test that bot controls are absent while an unseated player still sees `入座`:

```dart
testWidgets('non-owner cannot manage bot seats',
    (WidgetTester tester) async {
  final api = FakeApiClient(
    playGame,
    createdRoomOwnerPlayerId: 'other-owner',
    createdRoomSeats: const {
      'SOUTH': SeatInfo(playerId: 'other-owner', displayName: 'Owner'),
      'WEST': SeatInfo(isBot: true, displayName: '人机'),
    },
  );
  await pumpRoomPage(tester, api: api);

  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();

  expect(find.text('添加人机'), findsNothing);
  expect(find.text('移除'), findsNothing);
  expect(find.text('入座'), findsNWidgets(2));
});
```

Add `this.createdRoomOwnerPlayerId` to the `FakeApiClient` named constructor parameters and add the field:

```dart
final String? createdRoomOwnerPlayerId;
```

In `createRoom`, `addBot`, and `removeBot`, set the returned owner with:

```dart
ownerPlayerId: createdRoomOwnerPlayerId ?? playerId,
```

- [ ] **Step 2: Run the widget tests and verify RED**

Run:

```powershell
Push-Location app
flutter test test/widget_test.dart --plain-name "owner can add and remove a bot on each empty seat"
Pop-Location
```

Expected: FAIL because the room page does not render or call bot controls.

- [ ] **Step 3: Add bot mutation handlers to `RoomPage`**

Add handlers matching existing join/leave loading and error behavior:

```dart
Future<void> _addBot(String seat) async {
  if (playerId == null || room == null) return;
  setState(() {
    loading = true;
    error = null;
  });
  try {
    final nextRoom = await api.addBot(room!.roomId, seat, playerId!);
    if (mounted) setState(() => room = nextRoom);
  } catch (e) {
    if (mounted) setState(() => error = AppError.fromException(e));
  } finally {
    if (mounted) setState(() => loading = false);
  }
}

Future<void> _removeBot(String seat) async {
  if (playerId == null || room == null) return;
  setState(() {
    loading = true;
    error = null;
  });
  try {
    final nextRoom = await api.removeBot(room!.roomId, seat, playerId!);
    if (mounted) setState(() => room = nextRoom);
  } catch (e) {
    if (mounted) setState(() => error = AppError.fromException(e));
  } finally {
    if (mounted) setState(() => loading = false);
  }
}
```

- [ ] **Step 4: Render the correct seat controls**

In `_seatCard`, derive explicit state:

```dart
final seatInfo = currentRoom.seats[seat];
final occupied = seatInfo != null;
final isBot = seatInfo?.isBot ?? false;
final isMe = occupied && !isBot && seatInfo.playerId == playerId;
final isOwner = currentRoom.ownerPlayerId == playerId;
final isSeated = currentRoom.seats.values.any(
  (item) => !item.isBot && item.playerId == playerId,
);
```

Use `Icons.smart_toy` for bots, `Icons.person` for humans, and `Icons.person_outline` for empty seats. Render `seatInfo.displayName` for occupied seats. In the action area:

```dart
if (!occupied && isOwner && isSeated)
  OutlinedButton.icon(
    onPressed: loading ? null : () => _addBot(seat),
    icon: const Icon(Icons.smart_toy, size: 16),
    label: const Text('添加人机', style: TextStyle(fontSize: 12)),
  )
else if (!occupied && !isSeated)
  Row(
    mainAxisAlignment: MainAxisAlignment.center,
    children: [
      OutlinedButton(
        onPressed: loading ? null : () => _joinSeat(seat),
        child: const Text('入座', style: TextStyle(fontSize: 12)),
      ),
      if (isOwner) ...[
        const SizedBox(width: 4),
        IconButton(
          onPressed: loading ? null : () => _addBot(seat),
          icon: const Icon(Icons.smart_toy, size: 18),
          tooltip: '添加人机',
        ),
      ],
    ],
  )
else if (isBot && isOwner)
  OutlinedButton.icon(
    onPressed: loading ? null : () => _removeBot(seat),
    icon: const Icon(Icons.close, size: 16),
    label: const Text('移除', style: TextStyle(fontSize: 12)),
  )
else if (isMe)
  OutlinedButton(
    onPressed: loading ? null : () => _leaveSeat(seat),
    child: const Text('离开', style: TextStyle(fontSize: 12)),
  )
```

Keep the existing fixed `height: 28` action wrapper and compact padding so button changes do not resize the grid. Use an amber-tinted bot card distinct from human blue/green and empty grey states.

- [ ] **Step 5: Gate start on the client and update navigation fixture**

Change the owner start button callback to:

```dart
onPressed: loading || seatCount < 4 ? null : _startGame,
```

Update `start game navigates to game page` to construct a fake with a full room:

```dart
final api = FakeApiClient(
  playGame,
  createdRoomSeats: const {
    'SOUTH': SeatInfo(playerId: 'fake-player'),
    'WEST': SeatInfo(isBot: true),
    'NORTH': SeatInfo(isBot: true),
    'EAST': SeatInfo(isBot: true),
  },
);
await pumpRoomPage(tester, api: api);
```

- [ ] **Step 6: Run all room widget tests and verify GREEN**

Run:

```powershell
Push-Location app
flutter test test/widget_test.dart
Pop-Location
```

Expected: all widget tests pass with no overflow or pending timer errors.

- [ ] **Step 7: Format, analyze, and commit the room UI**

Run:

```powershell
Push-Location app
dart format lib/room_page.dart test/widget_test.dart
flutter analyze
Pop-Location
```

Expected: formatting completes and analysis reports no issues.

Commit:

```powershell
git add app/lib/room_page.dart app/test/widget_test.dart
git commit -m "feat: manage bots from room seats"
```

### Task 7: Verify the Complete Cross-Module Feature

**Files:**
- Verify only; fix failures in the files owned by Tasks 1-6 and commit each fix with its corresponding layer.

- [ ] **Step 1: Run focused server coverage**

```powershell
Push-Location server
mvn -Dtest=RoomStateSerializationTest,RoomServiceTest,RoomEventPublishingTest,GameServiceCreateTest,GameServicePermissionTest,RoomControllerTest test
Pop-Location
```

Expected: all selected server tests pass.

- [ ] **Step 2: Run focused Flutter coverage**

```powershell
Push-Location app
flutter test test/room_models_test.dart test/api_client_test.dart test/widget_test.dart
Pop-Location
```

Expected: all selected Flutter tests pass.

- [ ] **Step 3: Run repository-wide verification**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-all.ps1
```

Expected: backend tests, Flutter tests, and Flutter analysis all pass.

- [ ] **Step 4: Inspect the final diff and history**

```powershell
git status --short
git diff --check HEAD~6..HEAD
git log -7 --oneline
```

Expected: the worktree is clean, `git diff --check` prints nothing, and history contains the design/plan plus focused implementation commits.
