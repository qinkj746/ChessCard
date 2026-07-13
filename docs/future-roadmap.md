# ChessCard 后续路线图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前本机单人 MVP 演进为可多人联网、可持续扩展、可部署验证的升级棋牌项目。

**Architecture:** 先补工程基线和稳定接口，再做房间与实时同步；账号、战绩、聊天等产品能力建立在房间和玩家身份之上。规则与 AI、客户端体验、部署工程化并行推进，但每项都必须以可测试的小步交付为单位。

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Web, Spring Data JPA, MySQL, Redis, Flutter, Dart, HTTP, 后续 WebSocket。

---

## 使用方式

这份文档从根 README 的“后续待办”抽出，并展开成可执行路线图。后续开发时建议按阶段推进，不要一次混做多个子系统。

推荐顺序：

1. 先做 **阶段 0：工程基线**，保证后续每个功能能稳定验证。
2. 再做 **阶段 1：房间与实时同步**，这是联网、账号、聊天、战绩的共同基础。
3. 然后做 **阶段 2：玩家身份、账号与战绩**。
4. 规则、AI、客户端体验和部署可以在阶段 1 之后分支并行，但每次只认领一个任务。

执行约定：

- 每个任务都先写测试，再写实现，再跑定向验证，再跑相关回归。
- 每个任务完成后更新本文件对应复选框和 `README.md` 摘要。
- 如果实现中发现任务过大，先拆成新的 `docs/superpowers/plans/YYYY-MM-DD-<feature>.md`，再继续。
- 后端 API 字段变更必须同步更新 `app/lib/models.dart`、`app/lib/api_client.dart` 和 Flutter 测试。
- 所有新业务错误都要有后端测试覆盖，并明确返回 `400`、`401`、`403`、`404` 或 `409`。

## 当前基线

当前已完成：

- 本机单人 MVP：1 名真人玩家 + 3 名 AI。
- 后端 MySQL/JPA 牌局快照持久化。
- REST API：创建牌局、获取牌局、叫主、扣底、出牌、AI 推进、下一局。
- Flutter Web 可创建游戏、操作牌桌、展示当前墩和结束状态。
- 大量后端规则、服务、API 测试和 Flutter 模型、API、Widget 测试。

当前限制：

- 没有房间系统和多人座位所有权。
- 后端 WebSocket 事件通道和 Flutter 实时同步已接入；房间版本号与旧事件过滤已完成，自动重连体验仍待后续细化。
- 账号注册、登录、登出、历史战绩查询、房间内文本聊天、好友和房间邀请已具备第一版 API 与客户端接入。
- AI 策略偏合法出牌，不偏竞技最优。
- Flutter UI 已接入响应式牌桌布局、基础出牌/结算动效和统一错误提示；更细腻牌桌表现仍待后续增强。
- Windows 桌面端工程已生成，并已完成基础流程验证。

## 关键代码地图

后端核心文件：

- `server/src/main/java/com/chesscard/shengji/domain/GameState.java`：牌局状态。
- `server/src/main/java/com/chesscard/shengji/service/GameService.java`：创建、叫主、扣底、出牌、AI、下一局。
- `server/src/main/java/com/chesscard/shengji/service/AiPlayer.java`：AI 叫主和出牌选择。
- `server/src/main/java/com/chesscard/shengji/rules/TrickRules.java`：出牌、牌型、毙牌、墩赢家规则。
- `server/src/main/java/com/chesscard/shengji/rules/ScoreRules.java`：计分和升级规则。
- `server/src/main/java/com/chesscard/shengji/api/GameController.java`：REST API。
- `server/src/main/java/com/chesscard/shengji/api/dto/GameStateDto.java`：后端输出给客户端的牌局状态。
- `server/src/main/java/com/chesscard/shengji/persistence/JpaGameRepository.java`：牌局快照持久化。

Flutter 核心文件：

- `app/lib/models.dart`：客户端模型解析。
- `app/lib/api_client.dart`：HTTP API 客户端。
- `app/lib/game_page.dart`：牌桌页面和交互。
- `app/test/models_test.dart`：模型解析测试。
- `app/test/api_client_test.dart`：API 请求契约测试。
- `app/test/widget_test.dart`：牌桌交互测试。

---

## 阶段 0：工程基线

### Task 0.1：建立根目录一键验证入口

**目的：** 后续开发者能从根目录快速知道项目是否健康。

**Files:**

- Create: `scripts/test-server.ps1`
- Create: `scripts/test-app.ps1`
- Create: `scripts/verify-all.ps1`
- Modify: `README.md`
- Test: 手动运行脚本

- [x] **Step 1: 创建后端测试脚本**

创建 `scripts/test-server.ps1`，内容包含设置 `JAVA_HOME`、进入 `server/`、执行 `mvn test`。脚本失败时保留 Maven 原始退出码。

- [x] **Step 2: 创建 Flutter 测试脚本**

创建 `scripts/test-app.ps1`，内容包含进入 `app/`、执行 `flutter test` 和 `flutter analyze`。先跑测试，再跑分析。

- [x] **Step 3: 创建总验证脚本**

创建 `scripts/verify-all.ps1`，按顺序调用 `test-server.ps1` 和 `test-app.ps1`。后端失败时停止，不继续跑 Flutter。

- [x] **Step 4: 更新 README**

在 `README.md` 的”测试与验证”章节加入根目录脚本用法，并保留原始手动命令。

- [x] **Step 5: 验证**

Run: `powershell -ExecutionPolicy Bypass -File scripts/verify-all.ps1`

Expected: 后端测试、Flutter 测试和 Flutter analyze 都通过；如果本机缺 Flutter 或 Java，脚本输出明确缺失项。

> ✅ 已完成 2026-06-26。三个脚本已创建并包含 JAVA_HOME 自动设置、退出码传递和分步输出。README 已更新。

### Task 0.2：明确 API 错误响应契约

**目的：** 后续联网和账号功能需要稳定错误结构，客户端才能统一展示错误和重试状态。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/api/dto/ErrorResponse.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/GameController.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/GameControllerErrorTest.java`

- [x] **Step 1: 写失败测试**

在 `GameControllerErrorTest` 增加断言：非法操作返回 JSON 字段 `code`、`message`、`requestId`。牌局不存在返回 `404`，非法出牌返回 `400`。

- [x] **Step 2: 运行定向测试确认失败**

Run: `cd server; mvn -Dtest=GameControllerErrorTest test`

Expected: 新增断言因响应字段缺失失败。

- [x] **Step 3: 新增错误响应 DTO**

新增 `ErrorResponse`，字段使用 `code`、`message`、`requestId`。`code` 使用稳定英文枚举字符串，例如 `GAME_NOT_FOUND`、`INVALID_OPERATION`、`BAD_REQUEST`。

- [x] **Step 4: 收敛异常处理**

在 `GameController` 或独立 `@ControllerAdvice` 中统一构造 `ErrorResponse`。保留现有 HTTP 状态码语义。

- [x] **Step 5: 更新 Flutter 错误模型**

在 `app/lib/api_client.dart` 中解析错误响应。如果后端返回标准错误，抛出包含 `code` 和 `message` 的客户端异常。

- [x] **Step 6: 增加 Flutter API 错误测试**

在 `app/test/api_client_test.dart` 中模拟 `400` 标准错误 JSON，断言客户端异常包含错误码和消息。

- [x] **Step 7: 验证**

Run: `cd server; mvn -Dtest=GameControllerErrorTest test`

Run: `cd app; flutter test test/api_client_test.dart`

Expected: 后端和 Flutter 定向测试通过。

> ✅ 已完成 2026-06-26。新增 `ErrorResponse` DTO、更新 `GameController` 异常处理器、Flutter `GameApiException` 客户端类、测试已更新。

### Task 0.3：补跑 Windows 桌面端验证

**目的：** 关闭当前 README 记录的桌面端验证缺口。

**Files:**

- Modify: `README.md`
- Modify: `docs/future-roadmap.md`
- Test: Windows 桌面运行验证

- [x] **Step 1: 确认环境**

Run: `flutter doctor -v`

Expected: Visual Studio 显示 Desktop development with C++ 工作负载可用。

- [x] **Step 2: 启动后端**

Run: `cd server; mvn spring-boot:run`

Expected: `GET http://localhost:8080/api/infrastructure/health` 可返回健康信息。

- [x] **Step 3: 启动 Windows 客户端**

Run: `cd app; flutter run -d windows`

Expected: Windows 应用启动，点击“创建游戏”后能进入牌桌。

- [x] **Step 4: 验证核心交互**

手动验证创建游戏、叫主、扣底、出牌、AI 推进和下一局按钮。记录任何平台特有 UI 问题。

- [x] **Step 5: 更新文档**

如果验证通过，移除 `README.md` 中“Windows 桌面端未完整验证”的限制说明，改成验证日期和环境。若失败，记录失败命令、错误摘要和下一步。

> ✅ 已完成 2026-06-29。Windows 桌面端已完成基础流程验证：创建游戏、叫主、扣底、出牌、AI 推进和下一局流转可用；README 已同步移除“未完整验证”限制说明。

---

## 阶段 1：房间与实时同步

### Task 1.1：先引入访客玩家身份

**目的：** 在完整账号系统之前，先让后端能区分不同玩家，为房间座位所有权和 WebSocket 连接打基础。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/PlayerProfile.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/PlayerService.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/PlayerController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/PlayerProfileDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/PlayerServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/PlayerControllerTest.java`
- Modify: `app/lib/api_client.dart`
- Modify: `app/lib/models.dart`
- Test: `app/test/api_client_test.dart`

- [x] **Step 1: 定义访客创建行为**

行为约定：客户端调用 `POST /api/players/guest`，后端返回 `playerId`、`displayName`、`guest`。第一版不做密码，不做账号绑定。

- [x] **Step 2: 写服务层失败测试**

新增 `PlayerServiceTest.createsGuestPlayerWithStableIdAndDisplayName`。断言创建结果有非空 ID，昵称形如 `Guest-1234`，`guest == true`。

- [x] **Step 3: 实现最小 PlayerService**

先用内存仓储或 JPA 仓储二选一。推荐 JPA，因为后续账号和战绩会复用身份。新增表结构由 Hibernate `ddl-auto=update` 生成。

- [x] **Step 4: 写 Controller 测试**

新增 `PlayerControllerTest.createGuestPlayerReturnsProfile`，断言 `POST /api/players/guest` 返回 `200` 和正确 JSON。

- [x] **Step 5: 实现 PlayerController**

暴露 `POST /api/players/guest`，返回 `PlayerProfileDto`。

- [x] **Step 6: Flutter 接入访客创建**

在 `ApiClient` 增加 `createGuestPlayer()`，在 `models.dart` 增加 `PlayerProfileModel`。

- [x] **Step 7: Flutter 写请求契约测试**

在 `api_client_test.dart` 中断言 `createGuestPlayer()` 发送 `POST /api/players/guest`，并正确解析 `playerId`。

- [x] **Step 8: 验证**

Run: `cd server; mvn -Dtest=PlayerServiceTest,PlayerControllerTest test`

Run: `cd app; flutter test test/api_client_test.dart`

Expected: 定向测试通过。

> ✅ 已完成 2026-06-26。PlayerProfile 实体、PlayerRepository/JpaPlayerRepository、PlayerService、PlayerController、PlayerProfileDto 均已实现。Flutter 端新增 PlayerProfileModel 和 createGuestPlayer()。

### Task 1.2：新增房间领域模型和 REST API

**目的：** 让多个玩家可以进入同一个房间，并明确四个座位的占用关系。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/RoomState.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/RoomSeat.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/RoomPhase.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/RoomRepository.java`
- Create: `server/src/main/java/com/chesscard/shengji/persistence/JpaRoomRepository.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`

- [x] **Step 1: 定义房间状态**

第一版房间字段：`roomId`、`phase`、`seats`、`ownerPlayerId`、`gameId`、`createdAt`、`updatedAt`。`phase` 使用 `WAITING`、`PLAYING`、`FINISHED`。

- [x] **Step 2: 写创建房间失败测试**

新增 `RoomServiceTest.createsRoomWithOwnerOnSouthSeat`。断言创建房间后 owner 占用 `SOUTH`，房间处于 `WAITING`。

- [x] **Step 3: 实现 RoomService.createRoom**

创建房间并持久化。第一版规定创建者默认坐南家。

- [x] **Step 4: 写加入座位失败测试**

新增 `RoomServiceTest.joinSeatRejectsOccupiedSeat` 和 `RoomServiceTest.joinSeatAssignsPlayerToRequestedSeat`。

- [x] **Step 5: 实现 RoomService.joinSeat**

同一玩家不能同时占多个座位；被占座位返回 `409`。

- [x] **Step 6: 写离开座位失败测试**

新增 `RoomServiceTest.leaveSeatClearsPlayerSeat`。房主离开时先不转移房主，只清座位，后续再补房主管理。

- [x] **Step 7: 实现 REST API**

新增接口：`POST /api/rooms`、`GET /api/rooms/{id}`、`POST /api/rooms/{id}/seats/{seat}`、`DELETE /api/rooms/{id}/seats/{seat}`。

- [x] **Step 8: Controller 错误码测试**

在 `RoomControllerTest` 覆盖房间不存在 `404`、座位冲突 `409`、非法座位 `400`。

- [x] **Step 9: 验证**

Run: `cd server; mvn -Dtest=RoomServiceTest,RoomControllerTest test`

Expected: 房间服务和 API 测试通过。

> ✅ 已完成 2026-06-26。RoomPhase/RoomSeat/RoomState 领域模型、RoomRepository/JpaRoomRepository 持久化、RoomService、RoomController、RoomStateDto 均已实现。

### Task 1.3：房间开始游戏并绑定 GameState

**目的：** 房间从等待状态进入游戏状态，后续所有出牌操作都要知道操作者是谁、坐在哪个座位。

**Files:**

- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/domain/GameState.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/RoomController.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/GameStateDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/RoomControllerTest.java`

- [x] **Step 1: 定义开局规则**

第一版允许未坐满时用 AI 填补空座。房间开始后创建 `GameState`，并记录 `roomId` 和每个真人座位的 `playerId`。

- [x] **Step 2: 写失败测试**

新增 `RoomServiceTest.startGameCreatesGameAndMovesRoomToPlaying`。断言房间 `phase == PLAYING`，有 `gameId`，返回的 `GameState` 仍处于 `DECLARE`。

- [x] **Step 3: 给 GameState 增加 roomId 和 seatOwners**

在 `GameState` 中增加 `roomId` 和 `Map<PlayerSeat, String> seatOwners`。已有单人模式可以让 `SOUTH` owner 为空或使用访客 ID，AI 座位 owner 为空。

- [x] **Step 4: 改造 GameService.createGame**

保留原 `createGame()` 供单机入口使用，新增 `createGameForRoom(roomId, seatOwners)`。不要破坏现有测试。

- [x] **Step 5: 实现 RoomService.startGame**

校验房间处于 `WAITING`，创建游戏，绑定 `gameId`，房间改为 `PLAYING`。

- [x] **Step 6: 暴露 API**

新增 `POST /api/rooms/{id}/start`，返回房间状态和牌局状态，或只返回 `RoomStateDto` 并包含 `gameId`。推荐返回 `RoomStateDto`，客户端再按 `gameId` 拉牌局。

- [x] **Step 7: 验证**

Run: `cd server; mvn -Dtest=RoomServiceTest,RoomControllerTest,GameServiceCreateTest test`

Expected: 房间开局通过，现有创建牌局测试不回归。

> ✅ 已完成 2026-06-26。GameState 新增 roomId/seatOwners，GameService.createGameForRoom() 支持房间开局，RoomService.startGame() 校验房主权限并自动 AI 填补空座。

### Task 1.4：引入 WebSocket 事件通道

**目的：** 多人牌桌不再依赖手动刷新；任何玩家操作后，房间内其他客户端都能收到最新状态。

**Files:**

- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/chesscard/shengji/api/websocket/WebSocketConfig.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/websocket/RoomEvent.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/websocket/RoomEventPublisher.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomService.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomEventPublishingTest.java`

- [x] **Step 1: 选择协议**

推荐使用 Spring WebSocket + STOMP。事件目的地约定为 `/topic/rooms/{roomId}`，客户端动作仍先走 REST，WebSocket 只负责广播状态变化。

- [x] **Step 2: 添加依赖**

在 `server/pom.xml` 添加 `spring-boot-starter-websocket`。

- [x] **Step 3: 定义事件结构**

`RoomEvent` 字段：`type`、`roomId`、`gameId`、`version`、`payload`。事件类型先定义 `ROOM_UPDATED`、`GAME_UPDATED`、`PLAYER_JOINED`、`PLAYER_LEFT`。

- [x] **Step 4: 写发布器测试**

`RoomEventPublisherTest` 使用 mock `SimpMessagingTemplate`，断言发布到 `/topic/rooms/{roomId}`。

- [x] **Step 5: 实现发布器**

封装 `SimpMessagingTemplate.convertAndSend`，业务服务只调用 `publishGameUpdated(roomId, gameState)`。

- [x] **Step 6: 在关键动作后发布事件**

房间加入、离开、开始游戏、叫主、扣底、出牌、AI 推进、下一局创建后发布事件。

- [x] **Step 7: 验证** ✅ 已完成 2026-07-09

Run: `cd server; mvn -Dtest=RoomEventPublisherTest,RoomServiceTest,GameServicePlayTest test`

Expected: 发布器和关键服务测试通过。

> ✅ 已完成 2026-07-09。新增 Spring WebSocket/STOMP 配置、`RoomEvent`/`RoomEventType`、`RoomEventPublisher` 与 `WebSocketRoomEventPublisher`，在房间创建/入座/离座/开局及房间牌局状态保存后向 `/topic/rooms/{roomId}` 发布 `ROOM_UPDATED`/`GAME_UPDATED`。新增 `RoomEventPublishingTest` 覆盖服务层发布和 WebSocket 目的地；通过 `mvn -Dtest=RoomEventPublishingTest test` 与后端全量 `mvn test`（354 个测试通过）。

### Task 1.5：Flutter 接入房间和实时同步

**目的：** 客户端能创建房间、入座、开始游戏，并在其他玩家操作后自动刷新牌桌。

**Files:**

- Modify: `app/pubspec.yaml`
- Create: `app/lib/room_models.dart`
- Create: `app/lib/room_api_client.dart`
- Create: `app/lib/room_connection.dart`
- Create: `app/lib/room_page.dart`
- Modify: `app/lib/main.dart`
- Modify: `app/lib/game_page.dart`
- Test: `app/test/room_models_test.dart`
- Test: `app/test/room_api_client_test.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 添加 WebSocket 客户端依赖**

选择 Flutter 生态稳定库，例如 `stomp_dart_client`。添加后运行 `flutter pub get`。

- [x] **Step 2: 定义 Room 模型**

新增 `RoomStateModel`、`RoomSeatModel`、`RoomEventModel`。先写 `room_models_test.dart`，覆盖 JSON 解析。

- [x] **Step 3: 实现 Room REST 客户端**

新增 `RoomApiClient`，包含 `createRoom`、`getRoom`、`joinSeat`、`leaveSeat`、`startGame`。

- [x] **Step 4: 写 REST 契约测试**

在 `room_api_client_test.dart` 中断言每个方法的 HTTP method、path 和 body。

- [x] **Step 5: 实现 RoomConnection**

负责连接 `/ws`，订阅 `/topic/rooms/{roomId}`，把事件转换成 Dart stream。断线时先实现手动重连方法，自动重连放后续任务。

- [x] **Step 6: 新增房间页面**

`RoomPage` 展示四个座位、创建者、入座按钮、离座按钮和开始游戏按钮。开始游戏后进入 `GamePage`。

- [x] **Step 7: 改造 GamePage**

让 `GamePage` 可接收 `roomId` 和 `playerId`。当有 WebSocket `GAME_UPDATED` 事件时，拉取最新 `GameStateModel` 并刷新。

- [x] **Step 8: Widget 测试**

覆盖创建房间后显示四个座位、点击入座更新座位、开始游戏后进入牌桌。

- [x] **Step 9: 验证**

Run: `cd app; flutter test test/api_client_test.dart test/room_models_test.dart test/room_connection_test.dart test/widget_test.dart`

Expected: 房间模型、API 和页面测试通过。

> ✅ 已完成 2026-07-10。Flutter 端新增 stomp_dart_client 依赖和 RoomConnection 事件源，RoomEventModel 支持解析后端房间事件；ApiClient 增加 getGame 查询，RoomPage 订阅房间事件后以 getRoom 刷新服务端状态，GamePage 在房间模式下订阅 GAME_UPDATED 并拉取最新牌局。新增/更新 API、连接、模型和 Widget 测试；通过 flutter test 与 flutter analyze。

### Task 1.6：多人动作权限校验

**目的：** 防止任意玩家替其他座位叫主、扣底或出牌。

**Files:**

- Modify: `server/src/main/java/com/chesscard/shengji/api/GameController.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/DeclareRequest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/KittyRequest.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/PlayRequest.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/GameServicePlayTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/GameControllerErrorTest.java`
- Modify: `app/lib/api_client.dart`
- Test: `app/test/api_client_test.dart`

- [x] **Step 1: 定义操作者传递方式**

第一版使用请求体 `playerId` 字段（而非请求头）。后续账号上线后替换为认证上下文。

- [x] **Step 2: 写权限失败测试**

增加测试：`WEST` owner 不能替 `SOUTH` 出牌，非庄家不能扣底，不是当前行动座位不能出牌。

- [x] **Step 3: 服务层增加 actor 校验**

为 `declare`、`setKitty`、`play` 增加带 `playerId` 的重载方法。单人旧入口保留，内部使用空 actor 兼容。

- [x] **Step 4: Controller 读取请求体 playerId**

多人房间牌局从请求体读取 `playerId`。缺失返回 400，座位不匹配返回 403（`PermissionDeniedException`）。

- [x] **Step 5: Flutter API 加 playerId 参数**

`ApiClient` 的 `declare`、`kitty`、`play` 方法均增加可选 `playerId` 命名参数，有值时加入请求体。

- [x] **Step 6: 验证** ✅ 已完成 2026-06-26

新增 `GameServicePermissionTest`（10 个测试覆盖越权出牌、AI 座位拒绝真人、单人模式兼容）。
更新 `GameControllerErrorTest`（DTO 构造器适配、新增 403 映射测试）。
新增 `PermissionDeniedException`，`GameController` 新增 `@ExceptionHandler` 返回 403。
Flutter `FakeApiClient` 同步更新方法签名。

### Task 1.7：断线重连和状态重放

**目的：** 客户端断线后能重新进入房间，并恢复最新牌局状态。

**Files:**

- Modify: `server/src/main/java/com/chesscard/shengji/domain/RoomState.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/RoomStateDto.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/RoomEventPublisher.java`
- Modify: `app/lib/room_connection.dart`
- Modify: `app/lib/game_page.dart`
- Test: `server/src/test/java/com/chesscard/shengji/service/RoomServiceTest.java`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 引入房间版本号**

`RoomState` 增加 `version`，任何座位或游戏状态变化后递增。`RoomEvent.version` 使用该值。

- [x] **Step 2: 写版本递增测试**

加入座位、离开座位、开始游戏后版本号递增。

- [x] **Step 3: 客户端保存最后版本**

`RoomConnection` 记录最后收到的 `version`。重连后先 `GET /api/rooms/{id}`，再按 `gameId` 拉最新牌局。

- [x] **Step 4: 处理旧事件**

客户端收到小于等于当前版本的事件时忽略，避免乱序覆盖新状态。

- [x] **Step 5: Widget 测试**

模拟旧事件和新事件，断言旧事件不刷新，新事件触发拉取。

- [x] **Step 6: 验证**

Run: `cd server; mvn -Dtest=RoomServiceTest test`

Run: `cd app; flutter test test/widget_test.dart`

Expected: 版本和重连相关测试通过。

> ✅ 已完成 2026-07-10。RoomState 增加 version 并在创建、入座、离座、开局时递增，RoomStateDto 和 ROOM_UPDATED 事件输出版本；Flutter RoomStateModel 解析 version，RoomConnection 记录 lastVersion 并忽略旧事件，RoomPage/GamePage 对注入事件源同样按版本过滤。新增后端版本递增/事件版本测试和 Flutter 模型、连接、Widget 旧事件测试；通过 mvn test、flutter test 与 flutter analyze。

---

## 阶段 2：账号、战绩和社交能力

### Task 2.1：从访客升级到账号

**目的：** 支持稳定玩家身份，为战绩、好友和跨设备恢复做准备。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/UserAccount.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/AuthService.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/AuthController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/LoginRequest.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/AuthSessionDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/AuthServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/AuthControllerTest.java`
- Modify: `app/lib/api_client.dart`
- Create: `app/lib/auth_models.dart`
- Test: `app/test/api_client_test.dart`

- [x] **Step 1: 定义第一版认证范围**

第一版只做用户名 + 密码注册登录，不接第三方登录。密码必须哈希保存，不保存明文。

- [x] **Step 2: 写注册失败测试**

覆盖用户名重复、密码过短、注册成功后返回 session token。

- [x] **Step 3: 实现 AuthService.register**

保存账号、哈希密码、生成 session token。session 可先持久化到 MySQL，Redis 会话放后续优化。

- [x] **Step 4: 写登录失败测试**

覆盖错误密码返回 `401`，正确密码返回 token。

- [x] **Step 5: 实现 AuthController**

接口：`POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`。

- [x] **Step 6: 兼容访客身份**

允许把当前访客 `playerId` 绑定到新账号，避免用户注册后丢失当前房间身份。

- [x] **Step 7: Flutter 增加登录状态**

新增 `AuthSessionModel`，`ApiClient` 支持保存 token，并把 token 放到 `Authorization` 请求头。

- [x] **Step 8: 验证**

Run: `cd server; mvn -Dtest=AuthServiceTest,AuthControllerTest test`

Run: `cd app; flutter test test/api_client_test.dart`

Expected: 注册、登录、token 请求头测试通过。

> ✅ 已完成 2026-07-10。新增 UserAccount/AuthSession 领域模型、账号仓储、AuthService、AuthController 与注册/登录/登出 DTO；密码使用 PBKDF2 哈希保存，注册可绑定既有访客 playerId，登录刷新 session token，登出清空账号 token。Flutter 新增 AuthSessionModel，ApiClient 支持 register/login/logout 并保存 Authorization token。通过后端 mvn test、Flutter flutter test 与 flutter analyze。

### Task 2.2：增加历史牌局和战绩

**目的：** 每局结束后保存可查询的结果，支撑个人战绩和复盘入口。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/GameRecord.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/GameRecordService.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/GameRecordController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/GameRecordDto.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/GameRecordServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/GameRecordControllerTest.java`
- Create: `app/lib/record_models.dart`
- Modify: `app/lib/api_client.dart`
- Test: `app/test/models_test.dart`
- Test: `app/test/api_client_test.dart`

- [x] **Step 1: 定义记录字段**

字段包括 `recordId`、`gameId`、`roomId`、`startedAt`、`finishedAt`、`players`、`winningTeam`、`attackerScore`、`levelDelta`、`nextLevelRank`、`completed`。

- [x] **Step 2: 写结束时创建记录测试**

在 `GameRecordServiceTest` 中构造结束牌局，断言只创建一条记录，重复结束不会重复插入。

- [x] **Step 3: 接入 GameService**

当 `GameService.play` 或 `createNextGame` 确认上一局已结束时，调用 `GameRecordService.recordFinishedGame`。

- [x] **Step 4: 增加查询 API**

接口：`GET /api/players/{playerId}/records`、`GET /api/games/{gameId}/record`。

- [x] **Step 5: Flutter 模型和 API**

新增 `GameRecordModel`，`ApiClient` 增加 `fetchPlayerRecords(playerId)`。

- [x] **Step 6: 验证**

Run: `cd server; mvn -Dtest=GameRecordServiceTest,GameRecordControllerTest,GameServiceRecordIntegrationTest,GameServicePlayTest test`

Run: `cd app; flutter test test/models_test.dart test/api_client_test.dart`

Expected: 记录创建和查询测试通过。

> ✅ 已完成 2026-07-10。新增 GameRecord 领域模型、GameRecordRepository/JPA 仓储、GameRecordService、GameRecordController 与 GameRecordDto；牌局最后一墩结算和 createNextGame 会幂等记录已结束牌局，支持按 playerId 查询历史记录和按 gameId 查询单局记录。Flutter 新增 GameRecordModel，ApiClient 支持 fetchPlayerRecords(playerId)。通过后端 mvn test、Flutter flutter test 与 flutter analyze。

### Task 2.3：增加房间内聊天

**目的：** 提供最小可用的房间社交能力，并验证 WebSocket 双向通信链路。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/ChatMessage.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/ChatService.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/ChatController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/ChatMessageDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/ChatServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/ChatControllerTest.java`
- Create: `app/lib/chat_models.dart`
- Modify: `app/lib/room_connection.dart`
- Modify: `app/lib/room_page.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 定义聊天范围**

第一版只支持房间内文本消息。限制单条消息最长 200 字符，空白消息拒绝，暂不做图片、表情包和私聊。

- [x] **Step 2: 写服务测试**

覆盖发送消息成功、空白消息返回 `400`、非房间成员发送返回 `403`。

- [x] **Step 3: 实现 ChatService**

保存消息到 MySQL，并通过 `RoomEventPublisher` 发布 `CHAT_MESSAGE` 事件。

- [x] **Step 4: 暴露 API**

接口：`POST /api/rooms/{roomId}/messages`、`GET /api/rooms/{roomId}/messages`。

- [x] **Step 5: Flutter 接入**

房间页增加消息列表和输入框。发送后清空输入框；收到 WebSocket 消息后追加显示。

- [x] **Step 6: 验证**

Run: `cd server; mvn -Dtest=ChatServiceTest,ChatControllerTest test`

Run: `cd app; flutter test test/widget_test.dart`

Expected: 聊天服务、API 和房间页测试通过。

> ✅ 已完成 2026-07-10。新增 ChatMessage 领域模型、ChatMessageRepository/JPA 仓储、ChatService、ChatController、ChatMessageDto 与 SendChatMessageRequest；发送消息会校验房间成员、拒绝空白和超长文本、保存到 MySQL，并通过 RoomEventPublisher 发布 CHAT_MESSAGE 事件。Flutter 新增 ChatMessageModel，ApiClient 支持 fetchRoomMessages/sendRoomMessage，RoomPage 增加消息列表、输入框、发送后清空，以及收到 CHAT_MESSAGE WebSocket 事件后追加显示。通过后端 mvn test、Flutter flutter test 与 flutter analyze。

### Task 2.4：增加好友和邀请入桌

**目的：** 支持玩家从好友列表邀请进入房间。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/domain/Friendship.java`
- Create: `server/src/main/java/com/chesscard/shengji/domain/RoomInvitation.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/FriendService.java`
- Create: `server/src/main/java/com/chesscard/shengji/service/InvitationService.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/FriendController.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/FriendServiceTest.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/InvitationServiceTest.java`
- Modify: `app/lib/room_page.dart`
- Create: `app/lib/friend_models.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 实现好友关系**

先做双向好友，不做关注。接口：发送请求、接受请求、删除好友、查看好友列表。

- [x] **Step 2: 实现邀请**

房主或已入座玩家可以邀请好友进入房间。邀请包含 `roomId`、`fromPlayerId`、`toPlayerId`、`expiresAt`、`status`。

- [x] **Step 3: WebSocket 通知**

被邀请玩家在线时收到 `ROOM_INVITATION` 事件；离线时下次查询邀请列表可看到。

- [x] **Step 4: Flutter 入口**

房间页增加邀请入口，好友列表中点击邀请。收到邀请后展示接受/拒绝操作。

- [x] **Step 5: 验证**

Run: `cd server; mvn -Dtest=FriendServiceTest,InvitationServiceTest test`

Run: `cd app; flutter test test/widget_test.dart`

Expected: 好友、邀请和基础 UI 测试通过。

> ✅ 已完成 2026-07-10。新增 Friendship/RoomInvitation 领域模型、状态枚举、服务层仓储接口、JPA 仓储适配、FriendService、InvitationService 和 FriendController；支持发送/接受/删除好友、查询好友列表、已入座玩家邀请好友、查询待处理邀请、接受/拒绝邀请，并通过 ROOM_INVITATION WebSocket 事件通知在线玩家。Flutter 新增 FriendshipModel/RoomInvitationModel，ApiClient 接入好友和邀请 API，RoomPage 增加好友邀请入口与收到邀请后的接受/拒绝操作。通过后端 mvn test（397 tests）、Flutter flutter test（51 tests）与 flutter analyze。

---

## 阶段 3：规则与 AI 深化

### Task 3.1：建立规则夹具库

**目的：** 后续补复杂牌型时不用在每个测试里手写大量牌面。

**Files:**

- Create: `server/src/test/java/com/chesscard/shengji/testutil/CardFixtures.java`
- Modify: `server/src/test/java/com/chesscard/shengji/rules/TrickRulesTest.java`
- Modify: `server/src/test/java/com/chesscard/shengji/service/GameServicePlayTest.java`

- [x] **Step 1: 整理重复造牌代码**

从现有测试中找出重复的 `new Card(...)` 组合，抽成 `CardFixtures.card(suit, rank, deckIndex)`、`pair(...)`、`tractor(...)`。

- [x] **Step 2: 先迁移一个小测试**

选择 `TrickRulesTest` 中最简单的对子测试，替换为夹具方法。

- [x] **Step 3: 运行测试**

Run: `cd server; mvn -Dtest=TrickRulesTest test`

Expected: 测试通过，确认夹具不改变语义。

- [x] **Step 4: 批量迁移高重复测试**

迁移 `TrickRulesTest` 和 `GameServicePlayTest` 中 3 到 5 个重复度最高的用例。不要一次重写全部测试。

- [x] **Step 5: 验证**

Run: `cd server; mvn -Dtest=TrickRulesTest,GameServicePlayTest test`

Expected: 规则和出牌服务测试通过。

> ✅ 已完成 2026-07-10。新增 CardFixtures 与 CardFixturesTest，提供 card、pair、tractor 测试夹具；迁移 TrickRulesTest 中基础拖拉机、重复物理牌、跟对子、跟拖拉机和拖拉机张数比较等代表性用例；GameServicePlayTest 的本地 card helper 改为复用 CardFixtures。通过 mvn -Dtest=CardFixturesTest,TrickRulesTest,GameServicePlayTest test（149 tests）与后端全量 mvn test（400 tests）。

### Task 3.2：补全甩牌失败原因和客户端提示

**目的：** 现在甩牌失败会退化为最小单张，后续需要让客户端知道发生了什么，避免用户困惑。

**Files:**

- Modify: `server/src/main/java/com/chesscard/shengji/domain/GameState.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/GameService.java`
- Modify: `server/src/main/java/com/chesscard/shengji/api/dto/GameStateDto.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/GameServicePlayTest.java`
- Modify: `app/lib/models.dart`
- Modify: `app/lib/game_page.dart`
- Test: `app/test/models_test.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 定义提示字段**

在 `GameState` 增加 `lastActionMessage`。甩牌退化时写入“甩牌失败，已按最小单张出牌”。普通成功动作清空或覆盖为最新动作消息。

- [x] **Step 2: 写后端失败测试**

在 `GameServicePlayTest` 中断言不成型甩牌退化后，`lastActionMessage` 非空，且实际只移除最小单张。

- [x] **Step 3: 实现服务层消息**

只在状态确实变化后设置消息。非法操作抛异常时不要修改消息。

- [x] **Step 4: DTO 输出字段**

`GameStateDto` 增加 `lastActionMessage`。

- [x] **Step 5: Flutter 解析和展示**

`GameStateModel` 增加 `lastActionMessage`。`GamePage` 在牌桌状态区域展示最近动作消息。

- [x] **Step 6: 验证**

Run: `cd server; mvn -Dtest=GameServicePlayTest test`

Run: `cd app; flutter test test/models_test.dart test/widget_test.dart`

Expected: 甩牌退化提示测试通过。

> ✅ 已完成 2026-07-10。确认并验证甩牌退化提示链路：GameState 已包含 lastActionMessage，GameService 在甩牌退化时写入“甩牌失败，已按最小单张出牌”，普通成功出牌会清空旧消息；GameStateDto 输出该字段，Flutter GameStateModel 解析并由 GamePage 在牌桌状态区显示后渐隐。通过后端 mvn -Dtest=GameServicePlayTest test（109 tests）、Flutter flutter test test/models_test.dart test/widget_test.dart（29 tests）、后端全量 mvn test（400 tests）、Flutter flutter test（51 tests）与 flutter analyze。

### Task 3.3：提升 AI 出牌策略

**目的：** AI 从“能合法出牌”升级为“能做基础得失分决策”。

**Files:**

- Create: `server/src/main/java/com/chesscard/shengji/service/AiPlayStrategy.java`
- Modify: `server/src/main/java/com/chesscard/shengji/service/AiPlayer.java`
- Test: `server/src/test/java/com/chesscard/shengji/service/AiPlayerTest.java`

- [x] **Step 1: 定义策略目标**

第一阶段只做三条规则：闲家能收分时优先赢墩；庄家能阻止闲家收分时优先毙牌；无分可争时优先出低价值牌。

- [x] **Step 2: 写闲家抢分测试**

构造当前墩已有分牌，AI 是闲家且手里有能赢的主牌。断言 AI 选择能赢墩的牌。

- [x] **Step 3: 写庄家防守测试**

构造闲家可能赢含分墩，AI 是庄家队且有足够主牌。断言 AI 尝试毙牌。

- [x] **Step 4: 写无分低牌测试**

构造当前墩无分，AI 无需赢墩。断言 AI 选择低价值合法牌。

- [x] **Step 5: 提取 AiPlayStrategy**

`AiPlayer` 负责输入校验和调用策略，`AiPlayStrategy` 负责候选牌排序和选择。

- [x] **Step 6: 保持旧测试通过**

Run: `cd server; mvn -Dtest=AiPlayerTest,GameServicePlayTest test`

Expected: 新旧 AI 测试都通过。

> ✅ 已完成 2026-07-10。新增 `AiPlayStrategy` 承担 AI 候选牌排序与选择，`AiPlayer` 保留输入校验后委托策略；补充闲家抢分、庄家防守和无分垫低牌 3 个红灯用例，确认旧逻辑会错误选择手牌中第一个主牌。策略现在会在有分可争时选择最低可赢主牌，在无分可争时优先垫低价值非主牌。验证：`mvn -f server\pom.xml -Dtest=AiPlayerTest test`（红灯 3 failures 后绿灯 50 tests）、`mvn -f server\pom.xml -Dtest=AiPlayerTest,GameServicePlayTest test`（159 tests）、`mvn -f server\pom.xml test`（403 tests）均通过。

### Task 3.4：增加残局回归样本

**目的：** 将真实或手工构造的复杂局面固定下来，防止未来规则调整回归。

**Files:**

- Create: `server/src/test/resources/fixtures/endgames/`
- Create: `server/src/test/java/com/chesscard/shengji/service/GameEndgameFixtureTest.java`
- Modify: `server/pom.xml` if resource loading needs configuration

- [x] **Step 1: 定义残局 JSON 格式**

字段包括 `name`、`gameState`、`action`、`expectedPhase`、`expectedWinner`、`expectedAttackerScore`。

- [x] **Step 2: 添加第一个简单残局**

从最后一墩闲家抠底流程抽一个 JSON 样本，保证夹具加载器能跑通。

- [x] **Step 3: 写加载测试**

`GameEndgameFixtureTest` 加载 JSON，反序列化为 `GameState`，执行动作，断言期望结果。

- [x] **Step 4: 添加复杂样本**

至少加入三个样本：甩牌退化、主牌毙甩牌、拖拉机跟牌不足。

- [x] **Step 5: 验证**

Run: `cd server; mvn -Dtest=GameEndgameFixtureTest test`

Expected: 残局样本全部通过。

> ✅ 已完成 2026-07-10。残局夹具库已建立：新增 `GameEndgameFixtureTest` 加载 `fixtures/endgames/*.json`，反序列化 `GameState` 后执行 `action` 并断言 `expectedPhase`、`expectedWinner`、`expectedAttackerScore` 等结果。已添加 4 个样本：最后一墩闲家抠底计分、甩牌失败退化提示、主牌毙成功甩牌、拖拉机跟牌不足时保留合法对子。验证：先运行 `mvn -f server\pom.xml -Dtest=GameEndgameFixtureTest test` 确认红灯为资源目录缺失，补样本后该命令通过（1 test）；`mvn -f server\pom.xml -Dtest=GameEndgameFixtureTest,GameServicePlayTest test` 通过（110 tests）；`mvn -f server\pom.xml test` 通过（404 tests）。

---

## 阶段 4：客户端体验

### Task 4.1：重构牌桌布局为响应式

**目的：** 让 Web、移动端和 Windows 桌面端都有稳定牌桌布局。

**Files:**

- Modify: `app/lib/game_page.dart`
- Create: `app/lib/table_layout.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 抽出布局组件**

从 `GamePage` 中抽出 `TableLayout`，负责四个座位、状态栏、当前墩和南家手牌的位置。

- [x] **Step 2: 写宽屏布局测试**

用固定宽高 pump widget，断言四个座位都存在，南家手牌在底部。

- [x] **Step 3: 写窄屏布局测试**

用手机尺寸 pump widget，断言操作按钮没有溢出，当前墩仍可见。

- [x] **Step 4: 实现响应式布局**

使用 `LayoutBuilder` 根据宽度切换紧凑布局和宽屏布局。按钮区域允许换行。

- [x] **Step 5: 验证**

Run: `cd app; flutter test test/widget_test.dart`

Expected: 宽屏和窄屏布局测试通过，`flutter analyze` 无问题。

> ✅ 已完成 2026-07-10。新增 `TableLayout` 响应式牌桌布局容器，宽屏保持现有纵向牌桌，窄屏改为可滚动紧凑布局并为牌桌区保留稳定高度；`GamePage` 复用现有状态栏、牌桌、历史、动作和手牌组件。新增 Widget 测试覆盖布局容器和 360x520 窄屏无溢出、当前墩可见；先运行 `flutter test test/widget_test.dart` 观察到缺少 `TableLayout` 红灯，补实现后该命令通过（27 tests），随后 `flutter test` 通过（53 tests），`flutter analyze` 无问题。

### Task 4.2：增加出牌和结算动效状态

**目的：** 让用户清楚看到“谁出了什么牌”和“一墩如何结算”。

**Files:**

- Modify: `app/lib/game_page.dart`
- Create: `app/lib/trick_animation.dart`
- Test: `app/test/widget_test.dart`

- [x] **Step 1: 定义动画触发条件**

当 `currentTrick` 从空变非空时播放出牌反馈；当 `currentTrick` 从四家变空且 `currentTurn` 更新时播放结算反馈。

- [x] **Step 2: 写 Widget 测试**

构造前后两个 `GameStateModel`，断言结算后页面显示短暂赢家提示。

- [x] **Step 3: 实现 TrickAnimation**

第一版只做淡入、位置过渡和赢家高亮，不引入复杂物理动画。

- [x] **Step 4: 加入可关闭动画开关**

在测试或低性能设备上允许禁用动画。`GamePage` 增加可选参数 `animationsEnabled`，需要时可在测试或低性能场景中显式关闭。

- [x] **Step 5: 验证**

Run: `cd app; flutter test test/widget_test.dart`

Expected: 动画状态测试通过。

> ✅ 已完成 2026-07-10。新增 `TrickAnimation` 基础动效组件，使用淡入和轻微位移展示出牌/结算反馈；`GamePage` 新增 `animationsEnabled` 可选参数，并在服务端状态从空墩进入当前墩时显示“已出牌”、从四家当前墩结算为空并产生新 `PlayedTrick` 时显示赢家提示。新增 Widget 测试覆盖结算后“西 赢得本墩”提示和关闭动画开关；先运行 `flutter test test/widget_test.dart` 观察到缺少 `TrickAnimation` 与 `animationsEnabled` 红灯，补实现后该命令通过（29 tests），随后 `flutter test` 通过（55 tests），`flutter analyze` 无问题。

### Task 4.3：统一错误、加载和重试体验

**目的：** 网络失败、后端业务错误和等待 AI/其他玩家时，用户能知道发生了什么。

**Files:**

- Create: `app/lib/app_error.dart`
- Create: `app/lib/status_banner.dart`
- Modify: `app/lib/api_client.dart`
- Modify: `app/lib/game_page.dart`
- Modify: `app/lib/room_page.dart`
- Test: `app/test/widget_test.dart`
- Test: `app/test/api_client_test.dart`

- [x] **Step 1: 定义错误模型**

`AppError` 包含 `code`、`message`、`retryable`。网络超时和 5xx 可重试，400/403 通常不可重试。

- [x] **Step 2: API 客户端统一转换错误**

`ApiClient` 捕获 HTTP 错误和解析错误，转换为 `AppError` 或包装异常。

- [x] **Step 3: 新增 StatusBanner**

显示加载中、错误、成功提示。错误可重试时显示重试按钮。

- [x] **Step 4: GamePage 接入**

创建游戏、叫主、扣底、出牌、AI 推进和下一局动作都使用统一状态展示。

- [x] **Step 5: RoomPage 接入**

创建房间、入座、开始游戏和 WebSocket 断线都使用统一状态展示。

- [x] **Step 6: 验证**

Run: `cd app; flutter test test/api_client_test.dart test/widget_test.dart`

Expected: 错误解析和 UI 状态测试通过。

> ✅ 已完成 2026-07-10。`AppError` 和 `StatusBanner` 已接入 Flutter 客户端；`ApiClient` 会把标准后端错误与网络异常转换为可展示错误，`GamePage` 和 `RoomPage` 使用统一横幅展示错误并支持可重试操作。验证随本轮 Flutter 回归完成：`flutter test` 通过（55 tests），`flutter analyze` 无问题。

---

## 阶段 5：部署与扩展

### Task 5.1：整理运行配置（Docker 本版不做）

**目的：** 当前版本只保留本机 Java/Maven + MySQL + 可选 Redis 的运行配置说明；Docker Compose 不作为本版交付项。

**Files:**

- Create/Modify: `server/src/main/resources/application-dev.yml`
- Create/Modify: `server/src/main/resources/application-prod.yml`
- Modify: `README.md`

**本版不做：**

- `docker-compose.yml`
- `server/Dockerfile`

- [x] **Step 1: 拆分配置**

保留 `application.yml` 的通用配置，把开发和生产差异放到 `application-dev.yml`、`application-prod.yml`。

- [x] **Step 2: 标记 Dockerfile 本版不做**

Dockerfile 不作为当前版本交付项；后续需要容器化时再恢复该任务。

- [x] **Step 3: 标记 docker-compose 本版不做**

docker-compose 不作为当前版本交付项；本版继续使用 README 中的本机启动方式。

- [x] **Step 4: 验证本机配置**

Run: `mvn -f server\pom.xml -DskipTests compile`

Expected: 后端配置可编译，健康检查接口由 Task 5.2 覆盖。

> ✅ 已调整 2026-07-11。按当前版本范围，Docker Compose / Dockerfile 不作为本版交付项，不要求安装 Docker Desktop，也不要求补跑 `docker compose up --build`。本版保留 `application.yml`、`application-dev.yml`、`application-prod.yml` 的配置拆分；验证以 `mvn -f server\pom.xml -DskipTests compile` 和 Task 5.2 健康检查测试为准。

- [x] **Step 5: 更新 README**

README 保留本机启动方式，并说明 Docker Compose 不作为当前版本交付项。

### Task 5.2：增加健康检查深度和监控字段

**目的：** 部署后能知道数据库、Redis、版本和构建信息是否正常。

**Files:**

- Modify: `server/src/main/java/com/chesscard/shengji/api/InfrastructureController.java`
- Create: `server/src/main/java/com/chesscard/shengji/api/dto/HealthResponse.java`
- Test: `server/src/test/java/com/chesscard/shengji/api/InfrastructureControllerTest.java`

- [x] **Step 1: 定义健康响应**

字段包括 `status`、`database`、`redis`、`version`、`time`。Redis 不可用时整体状态可以是 `DEGRADED`，数据库不可用时为 `DOWN`。

- [x] **Step 2: 写健康检查测试**

覆盖数据库正常、Redis 不可用时返回 `DEGRADED`。

- [x] **Step 3: 实现响应 DTO**

不要直接返回 Map，使用稳定 DTO，便于后续监控系统解析。

- [x] **Step 4: 验证**

Run: `cd server; mvn -Dtest=InfrastructureControllerTest test`

Expected: 健康检查测试通过。

> ✅ 已完成 2026-07-10。新增 `HealthResponse` DTO，健康检查响应稳定输出 `status`、`database`、`redis`、`version`、`time`；数据库异常时整体状态为 `DOWN`，Redis 异常但数据库可用时为 `DEGRADED`。新增轻量 MockMvc 测试覆盖 Redis 不可用和数据库不可用场景；先运行 `mvn -f server\pom.xml -Dtest=InfrastructureControllerTest test` 观察到缺少 `$.status` 红灯，补实现后定向测试通过（2 tests），随后 `mvn -f server\pom.xml test` 通过（406 tests）。

### Task 5.3：定义长期扩展边界

**目的：** 避免房间、账号、规则、AI、客户端状态后续互相缠在一起。

**Files:**

- Create: `docs/architecture-boundaries.md`
- Modify: `README.md`

- [x] **Step 1: 记录模块职责**

写清楚 `GameService`、`RoomService`、`AuthService`、`GameRecordService`、`AiPlayer` 分别负责什么，不能互相直接承担什么。

- [x] **Step 2: 记录 API 边界**

写清楚 REST 负责命令和查询，WebSocket 负责事件广播；客户端收到 WebSocket 后以服务器状态为准。

- [x] **Step 3: 记录数据边界**

写清楚 `GameState` 是当前牌局状态，`RoomState` 是房间状态，`GameRecord` 是结束后的历史记录，不要混用。

- [x] **Step 4: 记录测试边界**

写清楚规则测试、服务测试、API 测试、Flutter API 契约测试、Widget 测试分别覆盖什么。

- [x] **Step 5: README 链接**

在 README 的主要文档列表中加入 `docs/architecture-boundaries.md`。

> ✅ 已完成 2026-07-10。新增 `docs/architecture-boundaries.md`，记录 `GameService`、`RoomService`、`AuthService`、`GameRecordService`、`AiPlayer` 等模块职责，明确 REST 负责命令和查询、WebSocket 负责事件广播，区分 `GameState`、`RoomState`、`GameRecord`、认证与社交数据边界，并列出规则测试、服务测试、API 测试、Flutter API 契约测试、模型测试、Widget 测试和残局 fixture 的覆盖范围。README 已加入文档链接。

---

## 推荐实施里程碑

Milestone 1：工程可持续

- 完成 Task 0.1、0.2、0.3。
- 验收标准：根目录有一键验证入口；错误响应稳定；Windows 桌面端状态明确。

Milestone 2：最小联网房间

- 完成 Task 1.1、1.2、1.3。
- 验收标准：能创建访客、创建房间、入座、开始游戏，并绑定现有牌局。

Milestone 3：实时多人牌桌

- 完成 Task 1.4、1.5、1.6、1.7。
- 验收标准：两个客户端进入同一房间，一个客户端操作后另一个客户端自动同步；越权操作被拒绝。

Milestone 4：可留存产品

- 完成 Task 2.1、2.2。
- 验收标准：用户可注册登录，牌局结束后能查询历史战绩。

Milestone 5：社交与体验

- 完成 Task 2.3、2.4、4.1、4.3。
- 验收标准：房间内可聊天、邀请好友，客户端在不同尺寸下可用，错误和重试体验清楚。

Milestone 6：规则和 AI 强化

- 完成 Task 3.1、3.2、3.3、3.4。
- 验收标准：复杂规则有夹具化回归，AI 有基础得失分策略，甩牌退化有用户可见提示。

Milestone 7：工程边界与运行配置

- 完成 Task 5.1、5.2、5.3。
- 验收标准：本机运行配置可编译验证，健康检查可用于部署监控，架构边界文档可指导后续开发；Docker Compose / Dockerfile 留到后续版本再评估。

## Self-Review

- Spec coverage: README 中的产品能力、规则与 AI、客户端体验、工程化四组后续待办均已展开为阶段和任务。
- Placeholder scan: 本计划没有未展开的占位项；每项都有目标、文件、步骤和验证方式。
- Type consistency: 文档中新增领域名保持一致：`RoomState`、`RoomService`、`RoomEvent`、`PlayerProfile`、`GameRecord`、`AppError`。
