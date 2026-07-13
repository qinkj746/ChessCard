# Account login UI completion design

**Date:** 2026-07-13
**Status:** Approved for specification review

## Goal

Complete the user-visible Flutter authentication experience that the prior roadmap marked complete prematurely: users can register, log in, see their account state, log out, and retain a session across app restarts while still being allowed to use the app as a guest.

## Decisions

- Guest play remains available for single-player games and rooms.
- The home page uses a top account card. When signed out it presents `以访客身份游戏` and a `登录 / 注册` action; when signed in it displays the account name and a logout action.
- The app persists the account session and restores it at startup using `flutter_secure_storage`.
- A lightweight app-owned `AuthController` based on `ChangeNotifier` is used. The project does not add Provider, Riverpod, or another full state-management package.
- Login and registration share one page with a mode switcher.
- Registration validates non-empty username/password and matching confirmation before calling the backend. Server-side validation remains authoritative.

## User flows

### Startup and guest flow

1. The root widget initializes the authentication controller.
2. The controller loads a stored account session. A loading view is displayed until the read completes.
3. If no usable stored session exists, the home page is shown in visitor mode. The user can still open the single-player game or room page.
4. The controller creates and caches a guest profile only when a feature needs a player identity, rather than creating one during application startup.

### Register and login flow

1. On the home account card, a visitor taps `登录 / 注册`.
2. The authentication page defaults to login and provides a register mode.
3. Login submits the existing `GameApi.login()` call. Registration submits `GameApi.register()`, supplying the cached guest `playerId` when one exists.
4. A successful response replaces any guest identity with the returned account session, seeds the shared API client token, writes the session to secure storage, notifies the UI, and returns to the home page.
5. A failed request leaves the current state unchanged and exposes an error message generated through `AppError`.

### Signed-in and logout flow

1. The home account card displays the authenticated display name/username and an explicit logout action.
2. Logout calls the existing backend API when a token is available. It clears the API token and local secure-storage entry even if the remote call fails, then changes the home card back to visitor mode.
3. Entering a room passes the logged-in account `playerId`; a visitor receives a guest profile via the same shared controller. A room page must no longer create a competing guest identity itself.

## Architecture

### Shared API client and authentication state

The root application widget owns one `ApiClient` and one `AuthController`. It supplies both to the home, authentication, game, and room routes. `ApiClient` gains explicit session-token assignment/clearing so that restored sessions can attach their `Authorization` header before the first request.

`AuthController` owns:

- the loaded `AuthSessionModel?`;
- an optional cached guest `PlayerProfileModel`;
- initialization and in-flight operation state;
- login, registration, logout, and `ensurePlayerIdentity()` methods;
- saving, loading, and deleting account-session data through a storage abstraction.

The storage abstraction permits memory-backed fake storage in tests while the production implementation uses `flutter_secure_storage`.

### UI

- `main.dart` becomes a stateful composition root and renders a loading state until the authentication controller has initialized.
- `HomePage` reads the controller with `AnimatedBuilder`/`ListenableBuilder`. Its top account card follows the selected layout B.
- `AuthPage` is a `StatefulWidget` with fields, client-side form validation, a submit progress state, mode-specific labels, and a visible error region.
- `RoomPage` receives the shared `GameApi` plus identity information/controller. It requests an identity through the controller and uses the returned `playerId` for room, chat, friend, and invitation calls.
- `GamePage` receives the shared `GameApi` so it shares the same authorization token. Its existing room-mode `playerId` behavior stays intact.

## Error handling and limits

- Network and API failures shown by authentication actions use `AppError.fromException`.
- Buttons are disabled during an in-flight submit.
- An unreadable, incomplete, or absent stored session is deleted and treated as signed out.
- This work does **not** introduce server-wide bearer-token authorization. The existing backend currently emits session tokens and accepts login/logout requests, but normal game and room endpoints do not establish an actor from the `Authorization` header. This feature continues to send the token and uses the account `playerId` in existing request contracts. Enforcing server-side authorization is a separate backend task.

## Files

### Create

- `app/lib/auth_controller.dart`
- `app/lib/auth_session_storage.dart`
- `app/lib/auth_page.dart`
- `app/test/auth_controller_test.dart`
- `app/test/auth_page_test.dart`

### Modify

- `app/pubspec.yaml` and generated lockfile: add `flutter_secure_storage`.
- `app/lib/api_client.dart`: restore/clear a token explicitly.
- `app/lib/main.dart`: root setup, startup restoration, account card, and routes.
- `app/lib/game_page.dart`: accept the shared API client.
- `app/lib/room_page.dart`: consume the shared identity/API client instead of creating a local guest independently.
- `app/test/api_client_test.dart`: restored-token/clearing coverage.
- `app/test/widget_test.dart`: adjust existing constructors/fakes and preserve room/game coverage.
- `docs/future-roadmap.md` and `README.md`: correct the account feature completion summary after verification.

## Tests and acceptance criteria

1. `AuthController` saves a successful account session, restores it, and clears it on logout or malformed storage.
2. `AuthPage` rejects mismatched registration passwords locally, submits successful login/registration, and renders failures without discarding state.
3. `HomePage` has visitor and authenticated card states and routes to the authentication page.
4. A signed-in user entering a room uses their account `playerId`; a signed-out user receives exactly one guest identity.
5. `ApiClient` uses a restored session token on authenticated requests and clears it after logout.
6. `flutter test` and `flutter analyze` pass after implementation.
