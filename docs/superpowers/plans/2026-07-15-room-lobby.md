# Room Lobby Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the App room entry open to a lobby that lists joinable waiting rooms, lets players join one, and still lets them create a new room.

**Architecture:** Add a `GET /api/rooms` backend path that returns `RoomStateDto` entries for rooms with `phase == WAITING` and fewer than four occupied seats. Add `fetchRooms()` to the Flutter API client. Update `RoomPage` so its initial state is a lobby and its existing room detail view is reused after create or join.

**Tech Stack:** Spring Boot, Java, JUnit 5, AssertJ, MockMvc, Flutter, Dart, flutter_test, http MockClient.

---

## File Structure

- Modify `server/src/main/java/com/chesscard/shengji/service/RoomRepository.java`: add `findJoinableWaitingRooms()`.
- Modify `server/src/main/java/com/chesscard/shengji/service/RoomService.java`: add `listJoinableRooms()`.
- Modify `server/src/main/java/com/chesscard/shengji/persistence/RoomStateJpaRepository.java`: add query helper for waiting rooms.
- Modify `server/src/main/java/com/chesscard/shengji/persistence/JpaRoomRepository.java`: map persisted waiting room snapshots to domain objects and filter full rooms.
- Modify `server/src/main/java/com/chesscard/shengji/api/RoomController.java`: add `GET /api/rooms`.
- Modify `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`: cover joinable filtering.
- Modify `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`: cover HTTP/list DTO behavior.
- Create `server/src/test/java/com/chesscard/shengji/persistence/JpaRoomRepositoryTest.java`: cover persisted listing behavior.
- Modify `app/lib/api_client.dart`: add `fetchRooms()` to `GameApi` and `ApiClient`.
- Modify `app/lib/room_page.dart`: add lobby state, refresh, create, join flow.
- Modify `app/test/api_client_test.dart`: cover `GET /api/rooms`.
- Modify `app/test/widget_test.dart`: cover lobby UI, empty lobby create, and join.

## Task 1: Backend Service Joinable Room List

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomRepository.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

Add imports:

```java
import java.util.List;
```

Add tests near the other `getRoom` tests:

```java
@Test
void listJoinableRoomsIncludesWaitingRoomsWithOpenSeats() {
    RoomState room = service.createRoom("player-1");

    List<RoomState> rooms = service.listJoinableRooms();

    assertThat(rooms).extracting(RoomState::getRoomId).contains(room.getRoomId());
}

@Test
void listJoinableRoomsExcludesFullAndPlayingRooms() {
    RoomState open = service.createRoom("open-owner");
    RoomState full = service.createRoom("full-owner");
    service.joinSeat(full.getRoomId(), "full-2", PlayerSeat.WEST);
    service.joinSeat(full.getRoomId(), "full-3", PlayerSeat.NORTH);
    service.joinSeat(full.getRoomId(), "full-4", PlayerSeat.EAST);
    RoomState playing = service.createRoom("playing-owner");
    service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.WEST);
    service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.NORTH);
    service.addBot(playing.getRoomId(), "playing-owner", PlayerSeat.EAST);
    service.startGame(playing.getRoomId(), "playing-owner");

    List<RoomState> rooms = service.listJoinableRooms();

    assertThat(rooms).extracting(RoomState::getRoomId)
            .contains(open.getRoomId())
            .doesNotContain(full.getRoomId(), playing.getRoomId());
}
```

Update the fake repository:

```java
@Override
public List<RoomState> findJoinableWaitingRooms() {
    return store.values().stream()
            .filter(room -> room.getPhase() == RoomPhase.WAITING)
            .filter(room -> room.getSeats().size() < 4)
            .toList();
}
```

- [ ] **Step 2: Run service tests to verify RED**

Run: `cd server; mvn -Dtest=RoomServiceTest test`

Expected: compile failure because `RoomService.listJoinableRooms()` and `RoomRepository.findJoinableWaitingRooms()` do not exist.

- [ ] **Step 3: Add minimal service/repository API**

In `RoomRepository.java`:

```java
import java.util.List;

default List<RoomState> findJoinableWaitingRooms() {
    return List.of();
}
```

In `RoomService.java`:

```java
import java.util.List;

public List<RoomState> listJoinableRooms() {
    return repository.findJoinableWaitingRooms();
}
```

- [ ] **Step 4: Run service tests to verify GREEN**

Run: `cd server; mvn -Dtest=RoomServiceTest test`

Expected: PASS.

## Task 2: Backend Persistence Joinable Query

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/persistence/RoomStateJpaRepository.java`
- Modify: `server/src/main/java/com/chesscard/shengji/persistence/JpaRoomRepository.java`
- Test: `server/src/test/java/com/chesscard/shengji/persistence/JpaRoomRepositoryTest.java`

- [ ] **Step 1: Write the failing persistence test**

Create `JpaRoomRepositoryTest.java`:

```java
package com.chesscard.shengji.persistence;

import com.chesscard.shengji.domain.PlayerSeat;
import com.chesscard.shengji.domain.RoomPhase;
import com.chesscard.shengji.domain.RoomState;
import com.chesscard.shengji.service.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaRoomRepositoryTest.Config.class)
class JpaRoomRepositoryTest {
    @Autowired
    private RoomRepository repository;

    @Test
    void findJoinableWaitingRoomsReturnsOnlyWaitingRoomsWithOpenSeats() {
        RoomState open = repository.save(RoomState.create("open-owner"));
        RoomState full = repository.save(RoomState.create("full-owner"));
        full.getSeats().put(PlayerSeat.WEST, new com.chesscard.shengji.domain.RoomSeat(PlayerSeat.WEST, "full-2", java.time.Instant.now()));
        full.getSeats().put(PlayerSeat.NORTH, new com.chesscard.shengji.domain.RoomSeat(PlayerSeat.NORTH, "full-3", java.time.Instant.now()));
        full.getSeats().put(PlayerSeat.EAST, new com.chesscard.shengji.domain.RoomSeat(PlayerSeat.EAST, "full-4", java.time.Instant.now()));
        repository.save(full);
        RoomState playing = RoomState.create("playing-owner");
        playing.setPhase(RoomPhase.PLAYING);
        repository.save(playing);

        List<RoomState> rooms = repository.findJoinableWaitingRooms();

        assertThat(rooms).extracting(RoomState::getRoomId)
                .contains(open.getRoomId())
                .doesNotContain(full.getRoomId(), playing.getRoomId());
    }

    static class Config {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @org.springframework.context.annotation.Bean
        RoomRepository roomRepository(RoomStateJpaRepository jpaRepository, ObjectMapper objectMapper) {
            return new JpaRoomRepository(jpaRepository, objectMapper);
        }
    }
}
```

- [ ] **Step 2: Run persistence test to verify RED**

Run: `cd server; mvn -Dtest=JpaRoomRepositoryTest test`

Expected: test failure because `JpaRoomRepository.findJoinableWaitingRooms()` currently returns the interface default empty list.

- [ ] **Step 3: Implement persisted query**

In `RoomStateJpaRepository.java`:

```java
import java.util.List;

List<RoomStateEntity> findByPhaseOrderByUpdatedAtDesc(String phase);
```

In `JpaRoomRepository.java`:

```java
import com.chesscard.shengji.domain.RoomPhase;
import java.util.List;

@Override
public List<RoomState> findJoinableWaitingRooms() {
    return jpaRepository.findByPhaseOrderByUpdatedAtDesc(RoomPhase.WAITING.name()).stream()
            .map(entity -> fromJson(entity.getSnapshotJson()))
            .filter(room -> room.getSeats().size() < 4)
            .toList();
}
```

- [ ] **Step 4: Run persistence test to verify GREEN**

Run: `cd server; mvn -Dtest=JpaRoomRepositoryTest test`

Expected: PASS.

## Task 3: Backend Controller List Endpoint

**Files:**
- Modify: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Add imports:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

Add test:

```java
@Test
void listReturnsJoinableRoomDtosOverHttp() throws Exception {
    playerRepo.save(new PlayerProfile("player-1", "Alice", false, "token-1", Instant.now()));
    RoomStateDto created = controller.create(new CreateRoomRequest("player-1"));
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].roomId").value(created.roomId()))
            .andExpect(jsonPath("$[0].phase").value("WAITING"))
            .andExpect(jsonPath("$[0].seats.SOUTH.displayName").value("Alice"));
}
```

Update `FakeRoomRepository` with the same `findJoinableWaitingRooms()` implementation from Task 1.

- [ ] **Step 2: Run controller test to verify RED**

Run: `cd server; mvn -Dtest=RoomControllerTest#listReturnsJoinableRoomDtosOverHttp test`

Expected: HTTP 404 or compile failure because `RoomController.list()` does not exist.

- [ ] **Step 3: Add controller endpoint**

In `RoomController.java` add import:

```java
import java.util.List;
```

Add method:

```java
@GetMapping
public List<RoomStateDto> list() {
    return service.listJoinableRooms().stream().map(this::toDto).toList();
}
```

- [ ] **Step 4: Run controller test to verify GREEN**

Run: `cd server; mvn -Dtest=RoomControllerTest#listReturnsJoinableRoomDtosOverHttp test`

Expected: PASS.

## Task 4: Flutter API Client Room List

**Files:**
- Modify: `app/lib/api_client.dart`
- Test: `app/test/api_client_test.dart`
- Modify test fake in `app/test/widget_test.dart` later when compiling full app tests.

- [ ] **Step 1: Write failing API client test**

Add test near other room API tests:

```dart
test('fetchRooms gets joinable rooms with bearer token', () async {
  late http.Request capturedRequest;
  final client = ApiClient(
    baseUrl: 'http://example.test',
    sessionToken: 'token-abc',
    httpClient: MockClient((request) async {
      capturedRequest = request;
      return http.Response('[${_roomJson()}]', 200,
          headers: {'Content-Type': 'application/json'});
    }),
  );

  final rooms = await client.fetchRooms();

  expect(capturedRequest.method, 'GET');
  expect(capturedRequest.url.toString(), 'http://example.test/api/rooms');
  expect(capturedRequest.headers['Authorization'], 'Bearer token-abc');
  expect(rooms.single.roomId, 'room-1');
});
```

- [ ] **Step 2: Run API client test to verify RED**

Run: `cd app; flutter test test/api_client_test.dart --plain-name "fetchRooms gets joinable rooms with bearer token"`

Expected: compile failure because `fetchRooms()` is not defined.

- [ ] **Step 3: Add API method and decoder**

In `GameApi`:

```dart
Future<List<RoomStateModel>> fetchRooms();
```

In `ApiClient`:

```dart
@override
Future<List<RoomStateModel>> fetchRooms() async {
  await _ensureSession();
  final response = await httpClient.get(
    Uri.parse('$baseUrl/api/rooms'),
    headers: _headers(),
  );
  return _decodeRooms(response);
}

List<RoomStateModel> _decodeRooms(http.Response response) {
  _throwIfError(response);
  return (jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>)
      .map((item) => RoomStateModel.fromJson(item as Map<String, dynamic>))
      .toList();
}
```

- [ ] **Step 4: Run API client test to verify GREEN**

Run: `cd app; flutter test test/api_client_test.dart --plain-name "fetchRooms gets joinable rooms with bearer token"`

Expected: PASS.

## Task 5: Flutter Room Lobby UI and Join Flow

**Files:**
- Modify: `app/lib/room_page.dart`
- Modify: `app/test/widget_test.dart`

- [ ] **Step 1: Update fake API for compilation**

In `FakeApiClient`, add:

```dart
final List<RoomStateModel> lobbyRooms = [];
int fetchRoomsCalls = 0;
final List<String> joinedSeats = [];

@override
Future<List<RoomStateModel>> fetchRooms() async {
  fetchRoomsCalls += 1;
  return List.unmodifiable(lobbyRooms);
}
```

- [ ] **Step 2: Write failing lobby display test**

Add test:

```dart
testWidgets('room page shows lobby after guest init', (WidgetTester tester) async {
  final api = FakeApiClient(playGame);
  api.lobbyRooms.add(const RoomStateModel(
    roomId: 'room-open',
    phase: 'WAITING',
    ownerPlayerId: 'owner-player',
    version: 1,
    seats: {'SOUTH': SeatInfo(playerId: 'owner-player', displayName: 'Owner')},
  ));

  await pumpRoomPage(tester, api: api);

  expect(api.fetchRoomsCalls, 1);
  expect(find.textContaining('room-open'), findsOneWidget);
  expect(find.byIcon(Icons.refresh), findsOneWidget);
  expect(find.byIcon(Icons.add), findsOneWidget);
});
```

- [ ] **Step 3: Run widget test to verify RED**

Run: `cd app; flutter test test/widget_test.dart --plain-name "room page shows lobby after guest init"`

Expected: FAIL because current `RoomPage` shows only the create action and does not fetch rooms.

- [ ] **Step 4: Add minimal lobby state and UI**

In `_RoomPageState`, add:

```dart
final List<RoomStateModel> lobbyRooms = [];
```

After player identity is set in `_initPlayer()`, call:

```dart
await _loadLobbyRooms();
```

Add:

```dart
Future<void> _loadLobbyRooms() async {
  try {
    final rooms = await api.fetchRooms();
    if (mounted) {
      setState(() {
        lobbyRooms
          ..clear()
          ..addAll(rooms);
      });
    }
  } catch (e) {
    if (mounted) setState(() => error = AppError.fromException(e));
  }
}
```

Replace the `room == null` body branch with a `_buildLobby()` method that renders player label, refresh icon, create button, and a list of rooms.

- [ ] **Step 5: Run lobby display test to verify GREEN**

Run: `cd app; flutter test test/widget_test.dart --plain-name "room page shows lobby after guest init"`

Expected: PASS.

- [ ] **Step 6: Write failing join flow test**

Add test:

```dart
testWidgets('joining lobby room chooses first empty seat and opens room detail', (WidgetTester tester) async {
  await tester.binding.setSurfaceSize(const Size(800, 1000));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final api = FakeApiClient(playGame);
  api.lobbyRooms.add(const RoomStateModel(
    roomId: 'room-open',
    phase: 'WAITING',
    ownerPlayerId: 'owner-player',
    version: 1,
    seats: {
      'SOUTH': SeatInfo(playerId: 'owner-player', displayName: 'Owner'),
      'WEST': SeatInfo(playerId: 'west-player', displayName: 'West'),
    },
  ));

  await pumpRoomPage(tester, api: api);
  await tester.tap(find.text('Join').first);
  await tester.pumpAndSettle();

  expect(api.joinedSeats.last, 'NORTH');
  expect(find.textContaining('room-open'), findsOneWidget);
  expect(find.byType(GridView), findsOneWidget);
});
```

Update fake `joinSeat()` so the first line records the chosen seat:

```dart
joinedSeats.add(seat);
```

- [ ] **Step 7: Run join flow test to verify RED**

Run: `cd app; flutter test test/widget_test.dart --plain-name "joining lobby room chooses first empty seat and opens room detail"`

Expected: FAIL because no join action exists in the lobby.

- [ ] **Step 8: Implement join action**

Add helper:

```dart
String? _firstEmptySeat(RoomStateModel candidate) {
  for (final seat in const ['SOUTH', 'WEST', 'NORTH', 'EAST']) {
    if (!candidate.seats.containsKey(seat)) return seat;
  }
  return null;
}
```

Add:

```dart
Future<void> _joinRoomFromLobby(RoomStateModel candidate) async {
  final currentPlayerId = playerId;
  final seat = _firstEmptySeat(candidate);
  if (currentPlayerId == null || seat == null) return;
  setState(() {
    loading = true;
    error = null;
  });
  try {
    final nextRoom = await api.joinSeat(candidate.roomId, seat, currentPlayerId);
    if (!mounted) return;
    final applied = _applyRoomSnapshot(nextRoom);
    if (applied) {
      _attachRoomEvents(nextRoom.roomId);
      await _loadRoomCompanionData(nextRoom.roomId);
    }
  } catch (e) {
    if (mounted) setState(() => error = AppError.fromException(e));
    await _loadLobbyRooms();
  } finally {
    if (mounted) setState(() => loading = false);
  }
}
```

Wire each lobby row join button to `_joinRoomFromLobby(room)`.

- [ ] **Step 9: Run join flow test to verify GREEN**

Run: `cd app; flutter test test/widget_test.dart --plain-name "joining lobby room chooses first empty seat and opens room detail"`

Expected: PASS.

## Task 6: Full Verification and Cleanup

**Files:**
- Review all touched files.

- [ ] **Step 1: Run targeted backend tests**

Run: `cd server; mvn -Dtest=RoomServiceTest,RoomControllerTest,JpaRoomRepositoryTest test`

Expected: PASS.

- [ ] **Step 2: Run targeted Flutter tests**

Run: `cd app; flutter test test/api_client_test.dart test/widget_test.dart`

Expected: PASS.

- [ ] **Step 3: Run analyzers or broader checks if targeted tests pass**

Run: `cd app; flutter analyze`

Expected: PASS.

- [ ] **Step 4: Check git diff**

Run: `git diff --stat`

Expected: only room lobby related files changed, plus pre-existing user changes still untouched unless they are in overlapping files.

- [ ] **Step 5: Commit implementation only after verification**

Run:

```bash
git add server/src/main/java/com/chesscard/shengji/service/RoomRepository.java `
  server/src/main/java/com/chesscard/shengji/service/RoomService.java `
  server/src/main/java/com/chesscard/shengji/persistence/RoomStateJpaRepository.java `
  server/src/main/java/com/chesscard/shengji/persistence/JpaRoomRepository.java `
  server/src/main/java/com/chesscard/shengji/api/RoomController.java `
  server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java `
  server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java `
  server/src/test/java/com/chesscard/shengji/persistence/JpaRoomRepositoryTest.java `
  app/lib/api_client.dart `
  app/lib/room_page.dart `
  app/test/api_client_test.dart `
  app/test/widget_test.dart
git commit -m "Add room lobby"
```

Expected: commit succeeds. Do not stage unrelated pre-existing changes in `server/src/main/java/com/chesscard/shengji/service/GameService.java` or `server/src/test/java/com/chesscard/shengji/service/GameServicePlayTest.java` unless they become necessary and are reviewed separately.
