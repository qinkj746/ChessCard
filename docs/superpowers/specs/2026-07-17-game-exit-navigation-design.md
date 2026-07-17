# Game Exit Navigation Design

## Goal

Make the left side of the game app bar the single, predictable exit point. Local single-player games should leave immediately, while an active game in a real-player room must ask for confirmation before the player leaves.

## Scope

- Add a leading back arrow to `GamePage` for every game state.
- Keep the existing local-game restart action in the app bar.
- Preserve the existing room leave API and its error handling.
- Apply the same room-game confirmation rule to platform back navigation, not just the app-bar arrow.

## Interaction Rules

### Local single-player game

- The left app-bar arrow is visible.
- Selecting it immediately pops `GamePage` and returns to the previous page.
- There is no confirmation dialog and no room API request.

### Active real-player room game

- The left app-bar arrow is visible.
- Selecting it opens a confirmation dialog rather than leaving immediately.
- The dialog states that the player's seat will become system-controlled and the other players can continue.
- `继续游戏` dismisses the dialog and keeps the player on the game page.
- `立即退出` calls `leavePlayingRoom`, then returns to the previous page when the request succeeds.
- A failed leave request keeps the player on the game page and uses the existing error banner.

### Finished real-player room game

- The left app-bar arrow returns immediately because the active-turn flow no longer needs protection.

### System Back Navigation

- Local single-player and finished room games may pop normally.
- Back navigation during an active room game is intercepted and follows the same confirmation flow as the app-bar arrow.

## Component Design

`GamePage` owns a single exit coordinator that branches on room mode and game phase:

1. Local mode pops immediately.
2. Finished room mode pops immediately.
3. Active room mode shows the confirmation dialog and, after confirmation, invokes the existing `leavePlayingRoom` API.

The app bar uses `leading` for the arrow and disables Flutter's implicit leading button. Room-mode actions no longer duplicate the arrow in the right action area. A `PopScope` delegates blocked active-room back attempts to the same coordinator so there is no path that skips confirmation.

## Error Handling

- No action is available while another game request or room-exit request is loading.
- Cancelling the dialog does not change room or game state.
- If the leave API fails, the route stays open and the existing retry-capable error banner displays the returned error.

## Testing

- The app bar exposes one left-side back arrow for local and room games.
- A local game exits through that arrow with no confirmation and no room API call.
- An active room game shows the confirmation dialog from the arrow and from system back navigation.
- Cancelling remains on the game page.
- Confirming calls `leavePlayingRoom` and returns after success.
- A leave failure remains on the game page and shows the error.
- A finished room game returns without a confirmation dialog.

## Non-Goals

- Changing room ownership, bot takeover, or server-side leave semantics.
- Changing the restart or next-game behavior.
- Adding a confirmation prompt to local single-player games.
