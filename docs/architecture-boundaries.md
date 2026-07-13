# ChessCard 架构边界

本文档记录长期扩展时的职责边界，避免房间、账号、规则、AI、客户端状态互相承担对方职责。

## 模块职责

### GameService

`GameService` 负责单局牌局生命周期：创建牌局、叫主、扣底、出牌、AI 推进、结算、创建下一局，以及维护 `GameState` 的合法状态流转。它可以调用规则类、AI 和牌局仓储，但不应直接承担账号注册、好友关系、聊天消息、房间邀请或客户端展示逻辑。

### RoomService

`RoomService` 负责房间生命周期：创建房间、查询房间、入座、离座、开局、维护房间版本号和发布房间事件。它可以协调 `GameService` 创建房间牌局，但不应实现出牌规则、账号认证、好友关系或聊天消息持久化。

### AuthService

`AuthService` 负责注册、登录、登出、密码哈希和 session token 生命周期。它可以绑定已有访客 `playerId`，但不应读写牌局、房间座位、好友邀请或聊天内容。

### PlayerService

`PlayerService` 负责玩家档案和访客身份。它提供可被账号、房间和社交能力复用的稳定 `playerId`，但不应承担认证、战绩统计或房间权限判断。

### GameRecordService

`GameRecordService` 负责已结束牌局的历史记录和查询。它消费 `GameState` 的结束快照生成 `GameRecord`，但不应参与当前牌局推进，也不应修改房间状态。

### ChatService

`ChatService` 负责房间内文本消息校验、保存和查询。它可以校验发送者是否属于房间成员，并发布聊天事件，但不应改变房间座位或牌局状态。

### FriendService 和 InvitationService

`FriendService` 负责好友请求、接受、删除和好友列表。`InvitationService` 负责房间邀请、待处理邀请查询、接受和拒绝。它们可以读取房间成员资格，但不应承担开局、出牌、聊天消息或账号密码逻辑。

### AiPlayer 和 AiPlayStrategy

`AiPlayer` 负责在给定 `GameState` 和手牌上下文中选择合法 AI 动作。`AiPlayStrategy` 负责候选牌排序和基础得失分策略。AI 不应直接保存游戏、不应发布事件，也不应绕过 `GameService` 修改状态。

### Rules 包

`DeclarationRules`、`TrickRules`、`ScoreRules`、`ShengjiSorter` 和 `DeckFactory` 只负责纯规则计算。规则类应尽量保持无状态、可单测，不依赖数据库、HTTP、WebSocket 或 Flutter 模型。

## API 边界

REST API 负责命令和查询：创建资源、提交动作、读取当前状态、读取历史记录。REST 请求完成后，服务端状态是唯一事实来源。

WebSocket 负责事件广播：房间变化、牌局变化、聊天消息和邀请通知。WebSocket 事件只告诉客户端“有变化”，客户端收到事件后应通过 REST 拉取最新状态，不能把事件 payload 当作完整状态快照长期使用。

客户端可以乐观展示加载、错误和短暂动画，但不能自行推导权威牌局结果。任何越权、非法出牌、过期事件或断线后的状态冲突，都以服务端 REST 查询结果为准。

## 数据边界

`GameState` 是当前牌局状态，包含牌局阶段、手牌、底牌、当前墩、已完成墩、庄家、级牌、主花色、分数、胜负和下一局信息。它不应包含聊天记录、好友关系、账号密码或长期 UI 状态。

`RoomState` 是房间状态，包含房间阶段、座位、房主、绑定 `gameId`、版本号和更新时间。它不应复制完整 `GameState`，也不应保存聊天消息正文。

`GameRecord` 是结束后的历史记录，来自结束牌局快照。它用于战绩查询和历史展示，不应继续参与当前牌局推进。

`UserAccount` 和 `AuthSession` 是认证状态，负责账号、密码哈希和 token。它们不应直接包含房间状态或牌局快照。

`ChatMessage`、`Friendship` 和 `RoomInvitation` 是社交数据。它们通过 `playerId` 和 `roomId` 关联玩家与房间，不应嵌入大型牌局状态。

Flutter 的 `GameStateModel`、`RoomStateModel`、`RoomEventModel` 等是 API DTO 的客户端镜像。客户端模型可以增加展示便利字段，但字段语义必须跟服务端 DTO 保持一致。

## 测试边界

规则测试覆盖纯算法和边界牌型，不启动 Spring，不依赖数据库。典型文件包括 `TrickRulesTest`、`ScoreRulesTest`、`DeclarationRulesTest` 和 `DeckFactoryTest`。

服务测试覆盖领域服务的状态流转、权限、防线和仓储边界。典型文件包括 `GameServicePlayTest`、`RoomServiceTest`、`AuthServiceTest`、`ChatServiceTest` 和 `FriendServiceTest`。

API 测试覆盖 HTTP 状态码、DTO 字段、错误结构和 Controller 与服务层契约。典型文件包括 `GameControllerErrorTest`、`RoomControllerTest`、`AuthControllerTest` 和 `InfrastructureControllerTest`。

Flutter API 契约测试覆盖请求路径、请求体、响应解析和错误转换。典型文件是 `app/test/api_client_test.dart`。

Flutter 模型测试覆盖 JSON 解析和客户端模型兼容性。典型文件包括 `models_test.dart`、`room_models_test.dart` 和 `chat_models_test.dart`。

Flutter Widget 测试覆盖页面行为、状态展示、响应式布局、错误横幅、房间事件刷新和牌桌动效。典型文件是 `widget_test.dart`。

残局 fixture 测试覆盖跨规则的真实局面回归。新增复杂规则或修复线上局面时，优先把局面固化到 `server/src/test/resources/fixtures/endgames/`。

## 扩展约定

新增能力时先判断属于命令、事件、当前状态、历史记录还是展示状态。命令走 REST，事件走 WebSocket，当前状态放在 `GameState` 或 `RoomState`，历史数据放在独立记录模型，展示状态留在 Flutter。

跨模块调用应从高层服务协调到低层规则或仓储，避免规则类反向依赖服务、Controller 直接写仓储、客户端绕过服务端推导权威结果。

每次完成路线图任务后，同步更新 `docs/future-roadmap.md` 和 README 摘要，保留验证命令和验证结果。