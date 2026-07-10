# ChessCard 下一步行动计划

> **定位：** 从 `future-roadmap.md` 中提取当前最优先、最可执行的任务，按依赖关系重排，每个步骤都给出具体文件、改动内容和验证命令。
>
> **原则：** 每步小到可以一个会话完成；每步都有独立验收标准；做完一步跑一次全量回归。
>
> **原文档：** 详细 5 阶段路线图见 `docs/future-roadmap.md`；MVP 设计见 `docs/superpowers/specs/2026-05-21-shengji-mvp-design.md`。本文档只覆盖其中最紧迫的子集，并在每一步标注对应的原任务编号。

---

## 当前基线（已完成）

- 单人 MVP：1 真人 (SOUTH) + 3 AI，双副牌 108 张
- 完整游戏循环：创建 → 叫主 → 扣底 → 出牌 → 结算 → 下一局
- 规则：单张/对子/基础拖拉机、毙牌、甩牌退化、抠底倍率、K 级通关
- 持久化：MySQL `game_session` 表，JSON 快照
- REST API 7 个端点，错误映射 400/404
- 后端 297+ 测试通过，Flutter 19 测试通过
- Flutter Web 已验证可运行

---

## 第一批：工程基线（预计 1-2 天）

> 对应原 Roadmap **阶段 0**。先让项目可一键验证、错误可机器解析，后续所有功能才能稳定迭代。

---

### Step 1 — 根目录一键验证脚本 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 从项目根目录跑一条命令就知道后端和前端是否健康 |
| **原任务** | Task 0.1 |
| **新建文件** | `scripts/test-server.ps1`、`scripts/test-app.ps1`、`scripts/verify-all.ps1` |
| **修改文件** | `README.md`（测试章节补充脚本用法） |

**具体工作：**

1. **`scripts/test-server.ps1`** — 后端测试脚本
   ```powershell
   # 设置 JAVA_HOME，进入 server/，运行 mvn test
   # 保留 mvn 原始退出码（$LASTEXITCODE）
   # 优先从环境变量读取 JAVA_HOME，否则用默认路径
   ```

2. **`scripts/test-app.ps1`** — Flutter 测试脚本
   ```powershell
   # 进入 app/，先跑 flutter test，再跑 flutter analyze
   # 任一步失败即停止，保留退出码
   ```

3. **`scripts/verify-all.ps1`** — 总验证脚本
   ```powershell
   # 按顺序调用 test-server.ps1 和 test-app.ps1
   # 后端失败则停止，不继续跑 Flutter
   # 输出汇总：通过/失败数量
   ```

4. **`README.md`** — 在"测试与验证"章节加入脚本用法

**验收：** 运行 `powershell -ExecutionPolicy Bypass -File scripts/verify-all.ps1`，后端和 Flutter 测试全部通过

---

### Step 2 — 统一 API 错误响应 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 后端所有错误返回统一 JSON 结构 `{code, message, requestId}`，客户端可以可靠解析 |
| **原任务** | Task 0.2 |
| **新建文件** | `server/.../api/dto/ErrorResponse.java` |
| **修改文件** | `server/.../api/GameController.java`、`app/lib/api_client.dart`、`app/test/api_client_test.dart`、`server/.../api/GameControllerErrorTest.java` |
| **测试文件** | `GameControllerErrorTest`（新增 requestId 唯一性测试）、`api_client_test.dart`（适配新 JSON 格式） |

**具体工作：**

1. **新增 `ErrorResponse` DTO**
   - 字段：`code`（英文枚举：`GAME_NOT_FOUND` / `INVALID_OPERATION` / `BAD_REQUEST`）、`message`（中文原文）、`requestId`（UUID）
   - 使用 Java `record`

2. **Controller 层统一错误处理**
   - 在 `GameController` 已有 `@ExceptionHandler` 基础上，统一返回 `ErrorResponse` 而非裸字符串
   - 保留 HTTP 状态码语义：`GameNotFoundException` → 404、`IllegalArgumentException` → 400

3. **Flutter 端解析错误**
   - `api_client.dart` 中 `_decode()` 方法：收到 4xx/5xx 时尝试解析 `ErrorResponse` JSON
   - `models.dart` 新增 `ErrorResponseModel`（可选，也可以用专用异常类 `AppError`）
   - 抛出包含 `code` + `message` 的异常，去掉 `Exception: ` 前缀

4. **补齐测试**
   - 后端：`GameControllerErrorTest` 断言 400 响应 JSON 包含 `code`、`message`、`requestId` 字段
   - Flutter：`api_client_test.dart` 模拟 400 标准错误 JSON，断言客户端异常包含 `code` 和 `message`

**验收：**
- `mvn -Dtest=GameControllerErrorTest test` 通过
- `flutter test test/api_client_test.dart` 通过
- 手动 `curl` 非法操作验证 JSON 结构

> ✅ 已完成 2026-06-26。`ErrorResponse` DTO 新增 `code`/`message`/`requestId`；`GameController` 异常处理器返回结构化 JSON；Flutter `ApiClient` 解析 JSON 错误并抛出 `GameApiException`；测试已更新。

---

### Step 3 — Windows 桌面端验证（环境前提） ✅ 已完成 (2026-06-29)

| 项 | 内容 |
|---|---|
| **目标** | 确认 Windows 桌面端是否可运行，关闭 README 中的验证缺口 |
| **原任务** | Task 0.3 |
| **前置条件** | Visual Studio 已安装"Desktop development with C++"工作负载 |

**具体工作：**

1. `flutter doctor -v` 确认 Windows 桌面工具链就绪
2. 启动后端 `mvn spring-boot:run`
3. `cd app && flutter run -d windows` 启动桌面应用
4. 手动验证：创建游戏 → 叫主 → 扣底 → 出牌 → AI 推进 → 下一局
5. 根据结果更新 `README.md`：成功则记录验证日期和 VS 版本；失败则记录错误和下一步

**验收：** Windows 桌面端状态文档化（通过或明确阻塞原因）

---

## 第二批：房间系统基础（预计 1-2 周）

> 对应原 Roadmap **阶段 1**。目标：两个真人可以进入同一个房间、坐到不同座位、开始一局游戏。这是联网的"骨架"。

---

### Step 4 — 访客玩家身份 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 后端能区分不同客户端，为房间座位所有权打基础 |
| **原任务** | Task 1.1 |
| **新建文件** | `server/.../domain/PlayerProfile.java`、`server/.../service/PlayerService.java`、`server/.../api/PlayerController.java`、`server/.../api/dto/PlayerProfileDto.java` |
| **修改文件** | `app/lib/api_client.dart`、`app/lib/models.dart` |
| **新增接口** | `POST /api/players/guest` → `{ playerId, displayName, guest: true }` |

**具体工作：**

1. **`PlayerProfile`** — JPA 实体
   - 字段：`playerId`(UUID)、`displayName`(如 "Guest-1234")、`guest`(true)、`createdAt`
   - JPA 表 `player_profile`，由 `ddl-auto=update` 生成

2. **`PlayerService`** — 创建访客
   - `createGuest()` 生成 UUID + 随机昵称，持久化
   - 先写测试再写实现

3. **`PlayerController`** — REST 端点
   - `POST /api/players/guest` 返回 `PlayerProfileDto`

4. **Flutter 接入**
   - `ApiClient.createGuestPlayer()` → `POST /api/players/guest`
   - `models.dart` 新增 `PlayerProfileModel`

5. **测试**
   - 后端：`PlayerServiceTest`（访客创建、ID 唯一）、`PlayerControllerTest`（200 + 正确 JSON）
   - Flutter：`api_client_test.dart` 增加 `createGuestPlayer` 请求契约测试

**验收：**
- `mvn -Dtest=PlayerServiceTest,PlayerControllerTest test` 通过
- `flutter test test/api_client_test.dart` 通过
- `curl -X POST http://localhost:8080/api/players/guest` 返回访客 JSON

> ✅ 已完成 2026-06-26。新增 `PlayerProfile` JPA 实体、`PlayerRepository`/`JpaPlayerRepository` 持久化、`PlayerService` 服务层、`PlayerController` REST 端点、`PlayerProfileDto`。Flutter 端新增 `PlayerProfileModel` 和 `createGuestPlayer()` 方法。后端 5 个测试、Flutter 1 个新测试。

---

### Step 5 — 房间模型与 API ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 玩家可以创建房间、入座、离开座位 |
| **原任务** | Task 1.2 |
| **新建文件** | `server/.../domain/RoomState.java`、`server/.../domain/RoomSeat.java`、`server/.../domain/RoomPhase.java`、`server/.../service/RoomService.java`、`server/.../api/RoomController.java`、`server/.../api/dto/RoomStateDto.java`、`server/.../persistence/JpaRoomRepository.java` |
| **新增接口** | `POST /api/rooms`、`GET /api/rooms/{id}`、`POST /api/rooms/{id}/seats/{seat}`、`DELETE /api/rooms/{id}/seats/{seat}` |

**具体工作：**

1. **领域模型**
   - `RoomPhase`: `WAITING` → `PLAYING` → `FINISHED`
   - `RoomSeat`: 记录座位号、`playerId`、入座时间
   - `RoomState`: `roomId`、`phase`、`seats`(Map<PlayerSeat, RoomSeat>)、`ownerPlayerId`、`gameId`、`createdAt`、`updatedAt`

2. **`RoomService`**
   - `createRoom(ownerPlayerId)` — 创建房间，owner 默认坐 SOUTH，房间 `WAITING`
   - `joinSeat(roomId, playerId, seat)` — 入座；座位被占返回 409；同一玩家不能占多个座位
   - `leaveSeat(roomId, playerId, seat)` — 离开座位

3. **`RoomController`**
   - `POST /api/rooms` (body: `{ playerId }`) → 返回 `RoomStateDto`
   - `GET /api/rooms/{id}` → 返回 `RoomStateDto`
   - `POST /api/rooms/{id}/seats/{seat}` (body: `{ playerId }`) → 入座
   - `DELETE /api/rooms/{id}/seats/{seat}` (body: `{ playerId }`) → 离座

4. **测试**
   - `RoomServiceTest`：创建房间、入座/冲突、离座
   - `RoomControllerTest`：200/404/409/400

**验收：**
- `mvn -Dtest=RoomServiceTest,RoomControllerTest test` 通过
- 手动 curl：创建房间 → 查看房间 → 入座 → 离座

> ✅ 已完成 2026-06-26。新增 `RoomPhase`、`RoomSeat`、`RoomState` 领域模型，`RoomRepository`/`JpaRoomRepository` 持久化，`RoomService`（创建/查询/入座/离座），`RoomController` REST 端点，`RoomStateDto`/`CreateRoomRequest`/`JoinSeatRequest` DTO。后端 12 个测试覆盖创建、入座冲突、离座权限。

---

### Step 6 — 房间开始游戏 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 房间从 WAITING 进入 PLAYING，创建 GameState 并绑定 |
| **原任务** | Task 1.3 |
| **修改文件** | `RoomService`、`GameService`、`GameState`（增加 `roomId` 和 `seatOwners`）、`RoomController` |
| **新增接口** | `POST /api/rooms/{id}/start` |

**具体工作：**

1. **`GameState` 扩展**
   - 增加 `roomId`（可为 null，兼容旧单人模式）
   - 增加 `seatOwners: Map<PlayerSeat, String>`（playerId，AI 座位为 null）

2. **`RoomService.startGame(roomId, playerId)`**
   - 校验：仅房主可开始、房间处于 WAITING、至少有房主在座
   - 未满座自动用 AI 填补
   - 调用 `GameService.createGame()`，传入 `seatOwners`
   - 房间 `phase → PLAYING`，记录 `gameId`

3. **`POST /api/rooms/{id}/start`** — 返回 `GameStateDto`

4. **测试**
   - `RoomServiceTest.startGameCreatesGameAndMovesRoomToPlaying`
   - 未满座时 AI 填补
   - 非房主开局被拒绝

**验收：**
- `mvn -Dtest=RoomServiceTest test` 通过
- 手动：创建玩家 → 创建房间 → 开局 → GET 房间返回 PLAYING + gameId

> ✅ 已完成 2026-06-26。`GameState` 新增 `roomId` 和 `seatOwners` 字段；`GameService.createGameForRoom()` 支持房间开局；`RoomService.startGame()` 校验房主权限并自动 AI 填补空座；`POST /api/rooms/{id}/start` 端点已实现。后端新增 3 个 startGame 测试。

---

### Step 7 — 出牌权限校验 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 在房间模式下，只能操作自己座位的牌；AI 座位拒绝真人操作 |
| **原任务** | Task 1.6 |
| **修改文件** | `GameService.play()`、`GameService.declare()`、`GameService.setKitty()`、`GameController` |

**具体工作：**

1. **`GameService` 增加 `playerId` 参数**
   - `play(gameId, seat, playerId, cards)`：校验 `seatOwners.get(seat) == playerId`，不匹配返回 403
   - `declare` / `setKitty` 同理
   - 单人模式（无 roomId）时 `playerId` 可为 null，保持向后兼容

2. **`GameController` 从请求中提取 `playerId`**
   - 暂时从请求体或 Header 读取，后续接入 JWT 后改为从 Token 提取

3. **测试**
   - 越权出牌返回 403
   - AI 座位拒绝真人出牌

**验收：**
- `mvn -Dtest=GameServicePlayTest test` 通过（新旧测试都兼容）
- 手动：两个玩家入座后，A 尝试操作 B 的座位返回 403

> ✅ 已完成 2026-06-26。`GameService` 的 `play()`、`declare()`、`setKitty()` 均增加 `playerId` 可选参数；新增 `resolveHumanSeat()` 校验座位所有权；新增 `PermissionDeniedException` 返回 403；`GameController` 从请求体传递 `playerId`；请求 DTOs 增加 `playerId` 字段；Flutter `ApiClient` 同步更新。单人模式（无 roomId）向后兼容，`playerId` 为 null 时跳过校验。新增 `GameServicePermissionTest` 覆盖越权出牌、AI 座位拒绝真人等场景。

---

### Step 8 — Flutter 房间页面 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | Flutter 有独立的房间页面：创建/加入房间、入座、开始游戏，然后进入牌桌 |
| **原任务** | Task 1.7 |
| **新建文件** | `app/lib/room_page.dart` |
| **修改文件** | `app/lib/main.dart`（增加路由）、`app/lib/game_page.dart`（接收 roomId/playerId） |

**具体工作：**

1. **`RoomPage`**
   - 顶部：玩家身份显示（访客昵称）
   - 创建房间按钮 → 调用 `POST /api/rooms`
   - 四个座位展示：已占用显示玩家昵称、空位显示入座按钮
   - 开始游戏按钮（仅房主可见，满座或确认 AI 填补后可用）
   - 开始后跳转到 `GamePage`，传入 `gameId` + `playerId`

2. **`ApiClient` 扩展**
   - `createGuestPlayer()`、`createRoom()`、`joinRoom()`、`leaveRoom()`、`getRoom()`、`startGame()`

3. **`GamePage` 适配**
   - 接收 `playerId` 参数，出牌/叫主/扣底请求带上 `playerId`

4. **测试**
   - `widget_test.dart` 增加房间页面测试（创建房间、显示座位）

**验收：**
- `flutter test` 通过
- 手动：启动两个浏览器窗口 → 分别创建访客 → 一个创建房间 → 另一个加入 → 开局 → 进入牌桌

> ✅ 已完成 2026-06-26。新增 `RoomPage`（创建房间、四座位展示、入座/离座、开始游戏跳转牌桌）；`models.dart` 新增 `SeatInfo`/`RoomStateModel`；`ApiClient` 新增 5 个房间方法；`GamePage` 支持 `playerId`/`roomId` 参数，房间模式下出牌携带 playerId；`main.dart` 首页改为双入口（单人/房间）。新增 3 个房间页面测试。

---

## 第三批：客户端体验增强（可并行）

> 对应原 Roadmap **阶段 4**。在房间系统开发的同时或之后，提升 Flutter 端的用户体验。

---

### Step 9 — 统一错误与加载状态 ✅ 已完成 (2026-06-26)

| 项 | 内容 |
|---|---|
| **目标** | 网络失败、后端错误、等待中等状态有清晰的 UI 反馈 |
| **原任务** | Task 4.3 |
| **新建文件** | `app/lib/app_error.dart`、`app/lib/status_banner.dart` |
| **修改文件** | `app/lib/api_client.dart`、`app/lib/game_page.dart`、`app/lib/room_page.dart` |

**具体工作：**

1. **`AppError` 模型**
   - 字段：`code`、`message`、`retryable`
   - 网络超时/5xx → `retryable=true`；400/403/404 → `retryable=false`

2. **`ApiClient` 统一错误转换**
   - 捕获 `http.ClientException`（网络断开）→ `AppError(retryable: true)`
   - 解析后端 `ErrorResponse` JSON → `AppError(code, message)`

3. **`StatusBanner` Widget**
   - 显示加载中、错误消息（可重试时显示重试按钮）、成功提示
   - 错误 3 秒后自动消失（除非用户悬停）

4. **接入现有页面**
   - `GamePage` 和 `RoomPage` 使用 `StatusBanner` 替代当前内联错误显示

5. **测试**
   - `api_client_test.dart` 增加网络错误场景
   - `widget_test.dart` 增加错误展示和重试按钮测试

**验收：**
- `flutter test` 通过
- 手动：断开后端 → 操作 → 看到错误提示和重试按钮

> ✅ 已完成 2026-06-26。新增 `AppError` 统一错误模型（支持 GameApiException/网络异常/通用异常转换）；新增 `StatusBanner` 组件（红色背景、错误图标、可关闭、retryable 错误显示重试按钮）；`GamePage` 和 `RoomPage` 使用 StatusBanner 替换内联错误文本；GamePage 支持重试操作。26 个测试全部通过。

---

### Step 10 — 甩牌失败反馈 ✅ 已完成 (2026-06-29)

| 项 | 内容 |
|---|---|
| **目标** | 甩牌退化时客户端能看到"甩牌失败，已按最小单张出牌"提示 |
| **原任务** | Task 3.2 |
| **修改文件** | `GameState`（增加 `lastActionMessage`）、`GameStateDto`、`GameService`、`models.dart`、`game_page.dart` |

**具体工作：**

1. **`GameState.lastActionMessage`** — 字符串字段
   - 甩牌退化时写入"甩牌失败，已按最小单张出牌"
   - 普通成功动作清空或设为 `null`

2. **`GameStateDto` 输出 `lastActionMessage`**

3. **Flutter 展示**
   - `GameStateModel` 增加 `lastActionMessage`
   - `GamePage` 在当前墩区域上方展示消息（非空时显示，2 秒后淡出）

4. **测试**
   - 后端：`GameServicePlayTest` 断言退化后 `lastActionMessage` 非空
   - Flutter：`widget_test.dart` 断言消息显示

**验收：**
- `mvn -Dtest=GameServicePlayTest test` 通过
- `flutter test test/widget_test.dart` 通过

---

## 执行约定

1. **每步先写测试再写实现**（TDD），跑定向测试确认失败，再写代码让测试通过
2. **每步完成后跑全量回归：**
   - 后端：`mvn test`（~297 测试）
   - Flutter：`flutter test && flutter analyze`（~19 测试）
3. **后端 API 字段变更必须同步更新：**
   - `app/lib/models.dart` 的 `fromJson`
   - `app/lib/api_client.dart` 的请求体
   - 对应的 Flutter 测试
4. **每步完成后在此文档中标记 `[x]`**

---

## 里程碑

| 里程碑 | 包含步骤 | 验收标准 |
|--------|---------|---------|
| M1: 工程可持续 | Step 1-3 | 根目录一键验证通过；错误响应结构统一；Windows 桌面状态明确 |
| M2: 最小联网房间 | Step 4-6 | 两个客户端可进入同一房间、入座、开局 |
| M3: 多人权限安全 | Step 7-8 | 越权操作被拒绝；Flutter 有独立房间页面 |
| M4: 体验可接受 | Step 9-10 | 错误/加载状态清晰；甩牌退化有提示 |

---

> **下一步：** 从 **Step 1** 开始执行。完成后在此文档勾选对应步骤，并提交代码。
