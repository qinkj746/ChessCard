# Room Lobby Design

## Goal

The room entry opens to a lobby first. Players see joinable rooms created by others, join one if it looks right, or create a new room when there is no suitable room.

## Scope

This feature adds a minimal public lobby for the existing room system.

- Show only rooms that are `WAITING` and not full.
- Do not show `PLAYING` or `FINISHED` rooms.
- Do not add spectators, search, filters, pagination, or global lobby WebSocket updates in this pass.
- Keep the existing room detail page behavior after a room is created or joined.

## Backend Design

Add a joinable room listing path through the existing room boundaries:

- `RoomRepository` adds a method for listing waiting rooms that still have at least one empty human/bot seat.
- `RoomService` adds `listJoinableRooms()`.
- `RoomController` exposes `GET /api/rooms`.
- The response reuses `RoomStateDto` so client models stay unchanged.

The repository-level implementation fetches persisted rooms and filters by the stored `RoomState` snapshot. The joinable rule is:

- `phase == WAITING`
- `seats.size() < 4`

This treats bot seats as occupied, matching the current start-game rule.

## App Design

`RoomPage` starts in lobby mode after player identity is ready.

The lobby shows:

- Current player display name.
- A refresh action.
- A create-room action.
- A list of joinable rooms, each with room id, owner, occupied seat count, and a join button.
- An empty state when no joinable rooms exist, with the same create-room action.

When the user taps join, the app picks the first empty seat in `SOUTH`, `WEST`, `NORTH`, `EAST` order, calls the existing `joinSeat` API, then switches into the existing room detail view and subscribes to that room's events.

When the user creates a room, the existing create flow remains: the new room becomes the active room detail view and subscribes to events.

This pass does not add a back-to-lobby action inside the room detail view. The platform back button continues to leave the room page as it does today.

## Error Handling

Lobby load failures show the existing `StatusBanner` with a retry action.

Joining may fail if the room fills between listing and tapping join. In that case the app shows the error and reloads the lobby list so the stale room disappears if it is no longer joinable.

Creating a room keeps the current error behavior.

## Testing

Backend tests:

- `RoomServiceTest` verifies `listJoinableRooms()` includes waiting rooms with empty seats.
- `RoomServiceTest` verifies full, playing, and finished rooms are excluded.
- `RoomControllerTest` verifies `GET /api/rooms` returns `RoomStateDto` entries.
- Repository tests verify persisted rooms can be listed through the joinable path.

Flutter tests:

- `api_client_test.dart` verifies `fetchRooms()` sends `GET /api/rooms` and decodes a list.
- `widget_test.dart` verifies the room page shows the lobby after player initialization.
- `widget_test.dart` verifies empty lobby still offers create room.
- `widget_test.dart` verifies tapping join chooses the first empty seat and enters the room detail view.

## Future Work

Later improvements may add lobby live refresh with a global room event topic, manual room-id entry, filters, private rooms, and reconnecting directly into an existing room.
