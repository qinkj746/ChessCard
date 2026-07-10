# ChessCard

ChessCard 是一个“升级 / 拖拉机 / 80 分”棋牌 MVP。项目当前聚焦本机单人玩法：1 名真人玩家加 3 名简单 AI，先跑通创建牌局、叫主、扣底、出牌、结算和下一局流转，再为后续联网房间预留接口边界。

技术栈分为两部分：

- `server/`：Java 17 + Spring Boot 3.3.5 后台，负责规则、AI 推进、牌局状态和 HTTP API。
- `app/`：Flutter 客户端，负责牌桌展示、玩家操作和调用后台 API。

## 当前状态

当前 MVP 主链路已经基本完成：

- 创建牌局并完成双副牌发牌。
- 真人叫主、AI 自动叫主、庄家扣底。
- 出牌阶段支持单张、对子、基础拖拉机、甩牌退化、主牌毙牌和一轮结算。
- 计分已接入本墩收分、最后一墩抠底倍率、胜方队伍、升级步数、下一局级牌和 K 级通关状态。
- 支持从已结束且未通关的牌局创建下一局，后端服务、API 和 Flutter 按钮流转均已接入。
- 后台已改为 MySQL/JPA 持久化牌局快照，Redis 仅作为可选基础设施健康检查项。
- Flutter Web 已验证可运行；Windows 桌面工程已生成，但本机验证依赖 Visual Studio C++ 桌面开发工作负载。

## 目录结构

```text
.
├── app/      Flutter 牌桌客户端
├── docs/     设计文档、实现计划和继续开发记录
└── server/   Spring Boot 后台规则服务
```

主要文档：

- `docs/superpowers/specs/2026-05-21-shengji-mvp-design.md`：MVP 设计、当前实现状态和继续开发记录。
- `docs/superpowers/plans/2026-05-21-shengji-mvp-implementation.md`：初版实现计划和验证记录。
- `server/README.md`：后台环境变量、启动和测试说明。
- `app/README.md`：Flutter 默认项目说明，当前以根目录 README 为主入口。

## 快速启动

### 后台服务

环境要求：

- JDK 17
- Maven 3.9+
- MySQL
- Redis 可选

后台默认连接本机 MySQL，并会自动创建数据库：

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path='D:\Java\jdk17\bin;' + $env:Path

$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='chess_card'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的数据库密码'

cd server
mvn spring-boot:run
```

启动后可检查健康状态：

```text
GET http://localhost:8080/api/infrastructure/health
```

Redis 默认配置为 `localhost:6379`、密码 `123456`。如果 Redis 不可用，健康检查会显示 Redis `DOWN`，但不影响创建和推进牌局。

### Flutter Web

```powershell
cd app
flutter pub get
flutter run -d edge
```

Flutter 客户端默认请求本机后台 `http://localhost:8080`。启动后台后，在页面中点击“创建游戏”即可进入牌桌。

### Windows 桌面端

```powershell
cd app
flutter run -d windows
```

Windows 桌面端需要安装 Visual Studio 的 “Desktop development with C++” 工作负载。当前文档记录中，本机因为缺少该工作负载，桌面运行未完成验证；Flutter Web 已完成可用性验证。

## API 概览

后台 API 以 JSON 形式交换牌局状态：

```text
GET  /api/infrastructure/health
POST /api/games
GET  /api/games/{id}
POST /api/games/{id}/declare
POST /api/games/{id}/kitty
POST /api/games/{id}/play
POST /api/games/{id}/ai/step
POST /api/games/{id}/next
```

错误处理当前约定：

- 牌局不存在返回 `404`。
- 非法操作、非法牌面、阶段不匹配等业务错误返回 `400`。

## 测试与验证

一键验证脚本：

```powershell
# 从项目根目录运行全部验证（后端 + Flutter）
powershell -ExecutionPolicy Bypass -File scripts/verify-all.ps1

# 单独运行后端测试
powershell -ExecutionPolicy Bypass -File scripts/test-server.ps1

# 单独运行 Flutter 测试和分析
powershell -ExecutionPolicy Bypass -File scripts/test-app.ps1
```

手动命令仍可使用：

后台单元测试：

```powershell
cd server
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path='D:\Java\jdk17\bin;' + $env:Path
mvn test
```

后台集成测试：

```powershell
cd server
mvn -Pintegration test
```

集成测试需要先准备真实 MySQL/Redis 配置。

Flutter 测试：

```powershell
cd app
flutter test
flutter analyze
```

当前测试覆盖重点：

- 后台规则：牌堆、叫主、排序、计分、出牌、墩赢家、最后一墩抠底、下一局创建。
- 后台 API：健康检查、牌局创建、下一局、错误输入和非法操作。
- AI：叫主、出牌选择、主牌毙牌、空手牌跳过、异常状态防线。
- Flutter：模型解析、API 请求契约、牌桌渲染、叫主候选项、出牌按钮状态、当前墩展示、下一局按钮。

## 已实现能力

### 后台规则

- 双副牌创建和发牌。
- 叫主候选计算，包含普通花色和大小王相关限制。
- 主牌排序、级牌排序和基础牌面比较。
- 出牌支持单张、对子、拖拉机和多张甩牌。
- 跟牌优先匹配首攻有效花色，拖拉机和对子有优先策略。
- 主牌可毙副牌，成功甩牌可被足量主牌毙掉。
- 四家出完后自动结算本墩，赢家成为下一轮首攻。
- 牌局结束时计算胜方、升级步数、下一局级牌和通关状态。

### 后台服务与持久化

- `GameService` 负责创建、叫主、扣底、出牌、AI 推进和下一局。
- `GameRepository` 保留仓储边界。
- `JpaGameRepository` 将 `GameState` 序列化保存到 MySQL 的 `game_session.snapshot_json`。
- `GameSchemaInitializer` 会修正 `snapshot_json` 为 `LONGTEXT`，避免完整牌局快照被截断。

### Flutter 客户端

- 创建游戏并展示四个座位、阶段、主花色、级牌、庄家和闲家分数。
- 展示南家手牌，支持选择、清空选择、扣底和出牌。
- 展示当前墩已出牌内容。
- 根据后台返回的叫主候选项显示叫主按钮。
- 支持 AI 推进一步。
- 结束阶段展示胜方、升级步数、下局级牌和通关状态。
- 未通关时可点击“下一局”进入新局。

## 已知限制

- 当前是本机 MVP，不支持真实多人联网同步。
- 已具备第一版账号注册、登录、登出、客户端 token 保存、历史战绩查询、房间内文本聊天、好友和房间邀请；支付仍待后续实现。
- AI 以规则驱动和可用牌选择为主，尚不是强策略 AI。
- Flutter UI 以功能验证为主，动画和牌桌表现仍比较基础。
- Windows 桌面端需要额外安装 Visual Studio C++ 桌面开发工作负载后再完整验证。
- Redis 当前只参与健康检查，不是核心牌局流转依赖。

## 后续待办

详细执行路线图见 `docs/future-roadmap.md`。该文档已经把后续工作拆成可追踪任务，包含推荐顺序、涉及文件、具体步骤和验证命令。

高层优先级：

- 先补工程基线：根目录验证脚本、统一错误响应、Windows 桌面端验证。
- 再做联网基础：访客身份、房间模型、入座、开始游戏、WebSocket 同步和多人权限校验。
- 然后扩展产品能力：支付、匹配和更完整的社交体验。
- 并行增强长期质量：规则夹具、复杂边界回归、AI 策略、响应式牌桌、错误重试体验和 Docker 部署。