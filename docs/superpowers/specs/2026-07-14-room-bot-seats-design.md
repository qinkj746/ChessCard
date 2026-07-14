# Room Bot Seats Design

## Problem

Creating a room seats the owner at SOUTH and leaves the other three positions visibly empty. The game service already treats seats without a human owner as AI-controlled after a game starts, but the room has no explicit bot occupants, no owner controls for managing them, and no requirement that all four seats be filled before starting. This makes the room state misleading and gives the owner no way to choose which positions should remain available for people.

## Goals

- Let the room owner add a bot independently to any empty seat while the room is waiting.
- Let the room owner remove a bot before the game starts.
- Keep bot seats occupied until the owner removes them; a human cannot automatically replace a bot.
- Require all four seats to contain either a human or a bot before starting.
- Persist bot occupants and synchronize changes through the existing room update event flow.
- Reuse the existing AI declaration, play, and game-advance behavior.

## Non-Goals

- Bot difficulty, personality, names, avatars, or strategy selection.
- Allowing non-owners to manage bots.
- Automatically filling every empty seat with one action.
- Automatically replacing a bot when a human joins.
- Adding bots after the room leaves the waiting phase.
- Changing the existing AI play strategy.

## Domain Model

`RoomSeat` will distinguish human and bot occupants explicitly. A human seat retains its stable `playerId`; a bot seat has no human player ID and carries a bot marker. The model will expose constructors or factories that preserve these invariants rather than requiring callers to assemble them manually.

The bot marker must be backward compatible with existing room snapshots. A missing marker deserializes as `false`, so all persisted seats created before this feature remain human seats.

`RoomStateDto.SeatInfo` will expose the bot marker. Human seat responses continue to contain `playerId` and the resolved player display name. Bot seat responses contain a null player ID, `isBot: true`, and the display name `人机`. The Flutter `SeatInfo` model will accept the nullable player ID and use the explicit bot marker for rendering and ownership checks.

## Server Operations

Two seat-specific operations will be added under the existing room resource:

- `POST /api/rooms/{roomId}/seats/{seat}/bot` adds a bot.
- `DELETE /api/rooms/{roomId}/seats/{seat}/bot` removes a bot.

Both requests identify the acting player with the same request shape currently used by room seat and start operations. `RoomService` will enforce that the room is waiting, the actor is the room owner, and the requested seat is valid. Adding requires an empty seat. Removing requires that the seat currently contains a bot; it cannot remove a human.

Every successful mutation touches and persists the room, increments its version, and publishes the existing `ROOM_UPDATED` event. Existing clients therefore refresh through the established REST-after-event flow.

`startGame` will reject any room whose seat map does not contain all four seats. This validation remains on the server even though the client also disables its start control.

## Game Integration

`GameService.createGameForRoom` will preserve human player IDs in `GameState.seatOwners` and map bot seats to null owners. This is the existing signal that the game service uses for AI-controlled positions, so bot seats reuse current declaration, play selection, and automatic advancement without introducing a second AI execution path.

Membership, invitation, chat, and duplicate-seat checks will compare only non-null human player IDs. A bot must never count as an authenticated room member or be resolvable as a player profile.

## Flutter Experience

Each room seat keeps a stable card size and has a state-specific action area:

- Empty seat, current user is owner and already seated: show an `添加人机` action with a robot icon.
- Empty seat, current user is not seated: show the existing `入座` action.
- Empty seat, owner is not seated: show both `入座` and `添加人机`, allowing the owner to reserve one position for themselves and fill other positions with bots.
- Human seat occupied by the current user: show `离开`.
- Human seat occupied by another player: show the player's display name and no seat action.
- Bot seat: show a robot icon and `人机`; show `移除` only to the owner.

Adding or removing a bot calls the new API and replaces the local room snapshot with the response. Global room loading prevents duplicate mutations. Failures use the existing status banner and leave the last confirmed room state intact.

The start button remains visible to the owner but is disabled until all four seats are occupied. Its seat count continues to show the current progress. The server remains authoritative if state changes between rendering and the request.

## Data Flow

1. The owner creates a room and sees one human seat plus three empty seats.
2. The owner selects `添加人机` on a specific empty position.
3. The client sends the room ID, seat, and owner player ID to the bot-seat endpoint.
4. The server validates ownership, waiting phase, and vacancy, then stores a bot `RoomSeat`.
5. The response updates the initiating client; `ROOM_UPDATED` causes other connected clients to fetch the same room version.
6. The owner may remove that bot, or fill remaining seats with humans or bots.
7. Once all four positions are occupied, the owner can start the game.
8. Game creation maps bot occupants to AI-controlled seat owners and human occupants to their player IDs.

## Error Handling

- Non-owner add or remove attempts fail with a permission error.
- Bot changes outside the waiting phase fail without mutating the room.
- Adding to an occupied seat fails whether the occupant is human or bot.
- Removing from an empty or human seat fails.
- Human join requests continue to fail for every occupied seat, including bot seats.
- Starting with fewer than four occupied seats fails on the server.
- Stale client operations surface the server response through the existing error banner; the next room event or refresh restores current state.

## Testing

- Room service tests cover adding a bot, removing a bot, version changes, owner-only permissions, waiting-phase restrictions, occupied-seat conflicts, and bot-only removal.
- Room service tests prove starting fails below four occupied seats and succeeds with any valid mix of humans and bots.
- Game service tests prove human seats retain their owners while bot seats become AI-controlled null owners.
- Controller tests cover both bot endpoints and DTO serialization of human versus bot seats.
- Persistence tests prove bot markers survive a room snapshot round trip and legacy seats default to human.
- Flutter model tests cover nullable bot player IDs, bot display text, and backward-compatible human parsing.
- API client tests cover the new POST and DELETE routes and request bodies.
- Widget tests prove each empty seat has an owner-only add-bot action, bots can be removed by the owner, non-owners cannot manage bots, occupied bot seats cannot be joined, and start remains disabled until four seats are occupied.
- Focused server and Flutter suites run first, followed by the repository verification scripts appropriate to both modules.

## Success Criteria

- A newly created room shows an independent add-bot action on each of its three empty seats.
- Adding a bot changes only the selected seat and is visible to all room clients.
- The room owner can remove a bot before starting; other players cannot.
- A human cannot replace a bot without the owner removing it first.
- The owner cannot start until all four seats contain humans or bots.
- A full mixed room starts successfully, with bots controlled by the existing AI logic and human permissions unchanged.
