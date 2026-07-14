# Room Player Display Names Design

## Problem

The room page currently renders internal player UUIDs in the current-player label and occupied seat cards. The authentication flow preserves the account `displayName`, but `RoomPage` reduces the active identity to `playerId`. Room responses also expose only `playerId` for each seat, so the client cannot render other signed-in players' usernames.

## Goals

- Show the current signed-in player's username before a room is created.
- Show every seated signed-in player's username after room creation and on room refreshes.
- Keep `playerId` as the stable identifier for room, game, chat, friend, and invitation operations.
- Continue showing the generated display name for guest players.
- Preserve room availability when stale room data references a missing player profile by falling back to the player ID.

## Non-Goals

- Changing authentication or authorization behavior.
- Replacing player IDs in API request contracts or persisted room ownership data.
- Adding account profile editing or username changes.
- Reworking friend, invitation, or chat display names.

## Architecture

The backend will enrich room response seat data at the API boundary. `RoomStateDto.SeatInfo` will contain both `playerId` and `displayName`. The room domain model remains identifier-based, avoiding duplicated profile data and keeping game permissions unchanged.

`RoomController` will use the existing player service to resolve each occupied seat's current display name while producing a room DTO. Resolution is limited to at most four seats. If a stored player ID has no profile, the DTO will use that ID as the display fallback instead of failing the entire room request.

The Flutter `SeatInfo` model will parse `displayName`. `RoomPage` will render this value in occupied seat cards. For the current-player label shown before and after room creation, `AuthController` will expose the active account or guest display name; the standalone `RoomPage(api: ...)` path will retain the complete guest profile returned during initialization.

## Data Flow

1. Login or registration stores an `AuthSessionModel` containing `playerId`, `username`, and `displayName`.
2. Entering the room initializes the active identity without creating a competing guest account.
3. Before room creation, the page displays the active identity's `displayName`.
4. Create, join, leave, and refresh requests continue sending only `playerId`.
5. The backend loads the room and resolves each occupied seat's display name from its player profile.
6. The room response returns each seat as `{ playerId, displayName }`.
7. The page uses `displayName` for rendering and `playerId` for ownership checks and actions.

## Compatibility and Error Handling

- The Flutter model will accept a missing or blank `displayName` and fall back to `playerId`, allowing a rolling client/server update and tolerating stale data.
- A missing player profile will not make room creation or refresh fail; the backend returns the stored ID as the fallback display value.
- Existing room and game permission checks remain unchanged because they continue comparing player IDs.
- Guest profiles use their existing generated display names such as `Guest-0001`.

## Testing

- Add a backend controller test proving room seat DTOs contain account display names and preserve player IDs.
- Add a backend fallback test for a room seat whose profile cannot be resolved.
- Extend Flutter room model tests for `displayName` parsing and player-ID fallback.
- Add widget tests proving a restored account shows its username before room creation and after creation.
- Add a widget test with multiple occupied seats proving each seat renders its supplied username and does not render its UUID.
- Run focused server and Flutter tests, then the existing broader verification commands appropriate to the changed modules.

## Success Criteria

- A signed-in user entering the room sees their username rather than a UUID.
- After a room is created or refreshed, all occupied seats with valid account profiles show usernames.
- Guest seats show generated guest display names.
- Internal player IDs remain unchanged in requests, ownership checks, and game actions.
- Existing authentication and room behavior tests continue to pass.
