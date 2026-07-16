# In-Game Room Exit Design

## Goal

When a player tries to exit during an active room game, the app should warn them clearly and protect the remaining players from losing the room or getting stuck.

## Scope

This feature covers exits from room-backed games while the room is in `PLAYING` phase.

- Add a confirmation prompt before a player exits an active game.
- Treat confirmed in-game exits as leaving the room and handing that seat to system control.
- Keep the room alive while at least one real player remains.
- Clean up the room only when no real players remain.
- Do not change normal waiting-room seat management except where it needs the same room cleanup rule.

## Product Rules

If a player exits while the game is running, the prompt says that the game is in progress and that their seat will be system-controlled if they leave now.

If the player cancels, nothing changes.

If the player confirms, the player leaves the room, but the seat is not opened for another human in the current game. The seat becomes a system-controlled seat so the existing game can continue.

The game continues for remaining players. When the departed player's seat must call trump, bury kitty, or play cards, existing AI flow handles that seat.

For this feature, a real player means a non-bot room seat with a `playerId`. The room remains if at least one real player is still present. If all real players have left, the room is deleted to match the existing waiting-room owner-exit cleanup behavior.

The owner has no special destructive power during an active game. If the owner exits and another real player remains, the room stays. Ownership transfers to one remaining real player so future room-level actions still have an owner.

## Backend Design

Add an explicit in-game room exit path instead of reusing waiting-room `leaveSeat`.

`RoomService` should expose a method that:

- Finds the room and validates it is in `PLAYING`.
- Finds the human seat occupied by the exiting player.
- Replaces that seat with a bot/system-controlled room seat.
- Transfers `ownerPlayerId` to a remaining human player when the owner exits.
- Deletes the room when no human players remain.
- Publishes a room update event after the change.

The existing game state already stores seat owners separately. The implementation should ensure AI actions are permitted for seats whose room seat is now system-controlled, without allowing a departed player to keep making authenticated moves for that seat.

The existing waiting-room `leaveSeat` should continue to clear seats. If the last human leaves a waiting room, the room is deleted. If a non-owner leaves and humans remain, the room remains. If the owner leaves and another human remains, ownership transfers instead of deleting the room.

## App Design

In `GamePage`, room mode exit actions should go through a confirmation dialog when the game phase is not `FINISHED`.

The dialog copy:

- Title: `游戏正在进行`
- Body: `现在退出后，你的座位将由系统托管，其他玩家可以继续游戏。确定要退出吗？`
- Cancel action: `继续游戏`
- Confirm action: `退出`

Confirming calls the new in-game room exit API, then pops back out of the game page. If the request fails, the player stays on the game page and sees the existing error banner.

Finished games keep the current fast return behavior because there is no live turn flow to protect.

## Error Handling

If the room no longer exists, the app exits the game page and lets the room page refresh or show its existing error.

If the server rejects the exit because the player is not seated in the room, the app shows the existing error banner and does not navigate away.

If the server successfully deletes the room because the last human left, no other active player should be left in that room. The exiting client can safely return to the previous screen.

## Testing

Backend tests:

- Exiting a playing room converts the player's seat to a bot/system-controlled seat.
- The room remains when another human player remains.
- The room is deleted when the last human player exits.
- Owner exit transfers ownership to a remaining human player.
- Waiting-room owner exit transfers ownership when another human remains and deletes only when no humans remain.
- Departed players can no longer act as their old seat.

Flutter tests:

- A room-backed active game shows the confirmation dialog before exiting.
- Cancel keeps the player on the game page.
- Confirm calls the in-game exit API and then navigates back.
- Exit API failure keeps the player on the game page and displays the error.
- Finished room-backed games can return without the in-progress warning.

## Future Work

Later work may add reconnection to reclaim a system-controlled seat, visible "托管中" badges in the room UI, and a room history entry explaining who left during the game.
