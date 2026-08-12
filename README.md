# Dungeon Escape

回合制 2D 地牢逃脱游戏，同时包含：

- Android 原生触屏版：一体化像素地牢 UI、完整五关、声音与震动设置。
- JavaFX 桌面版：基于 Java 21 与 JavaFX 21，通过 Maven 构建。

游戏规则由 `model`、`service` 和 `persistence` 共享，Android 与 JavaFX 界面复用同一套核心逻辑。

## Android 版

Android 工程位于 `android-app/`，当前应用版本为 `2.1.0`。

主要功能：

- 竖屏触控操作和贴图方向控制台
- 五张关卡地图及统一的连续地牢道路、墙体渲染
- 主菜单、选关、游戏 HUD 和通关弹窗贴图 UI
- 按键发光区域与点击判定共用同一边界
- 声音与震动独立开关并持久保存
- 移动、撞墙、门锁、推石、拾取、开门和通关差异化反馈

构建要求：JDK 17、Android SDK 36、Gradle 8.13。

```powershell
cd android-app
gradle clean assembleDebug lintDebug
```

调试 APK 输出到 `android-app/app/build/outputs/apk/debug/app-debug.apk`。

## 简介

玩家控制角色在地牢中移动与探索：穿过迷宫、收集宝物、拾取钥匙开门、推动巨石压住机关，
最终达成各关卡目标（到达出口、收集宝物、覆盖机关及其组合）即可通关。

内置 **5 个关卡**，难度递增，覆盖全部机制与目标类型。游戏规则全部位于纯后端
（`model` / `service` / `persistence` 包，零 JavaFX 依赖），界面层（`ui` 包）只负责
展示与输入转发，不复制任何游戏规则。

## 功能

- 回合制移动：WASD / 方向键，每步成功移动计 1 步
- 实体与机制：墙壁、出口、宝物、钥匙、门（需编号匹配的钥匙）、巨石（可推）、地板机关（被巨石压住触发）
- 目标系统：出口 / 收集所有宝物 / 覆盖所有机关，支持 AND / OR 递归组合
- 内置 5 个关卡（classpath 资源 `levels/level1.json` ~ `level5.json`），JSON 加载带严格校验
- 三场景界面：主菜单 → 选关 → 游戏；深色主题（FXML + CSS）
- 胜利弹窗（只弹一次）：显示关卡名与步数，可直接重新开始或返回选关
- 出错提示：关卡加载失败等均以信息明确的对话框呈现，程序不崩溃
- 美术图集：Codex 生成的像素风精灵图集（classpath 资源，4 列 × 3 行、每格 362×362），
  图集缺失/出错/尺寸不符时自动回退到内置 Shape 绘制，不影响启动
- 关键操作键：`R` 重新开始本关，`Esc` 返回选关

## 环境要求

- JDK 21 或更高版本
- Maven 3.9+（建议 3.9.x 及以上）
- 操作系统：Windows / macOS / Linux（Linux 需具备 JavaFX 运行所需的 GTK 库）
- 无需手动安装 JavaFX —— 依赖由 Maven 从中央仓库自动下载

## 常用命令

```bash
# 清理并运行全部测试（含编译）
mvn clean test

# 启动游戏窗口
mvn javafx:run

# 清理、测试并打包（生成 target/dungeon-escape-1.0.0.jar）
mvn clean package
```

> 项目未采用 JPMS 模块（无 `module-info.java`）；`mvn javafx:run` 由
> `javafx-maven-plugin`（pom 中配置的 `mainClass`）处理 JavaFX 模块路径并启动
> `com.example.dungeonescape.DungeonEscapeApplication`。

## 操作键

| 按键 | 功能 |
| ---- | ---- |
| `W` / `↑` | 向上移动 |
| `S` / `↓` | 向下移动 |
| `A` / `←` | 向左移动 |
| `D` / `→` | 向右移动 |
| `R` | 重新开始当前关卡 |
| `Esc` | 返回选关界面 |

界面底部另有「重新开始 / 选择关卡 / 主菜单」按钮；胜利弹窗内也可选择
「重新开始」或「选择关卡」。键盘监听挂在场景上，焦点在任意位置均生效。

## 内置关卡

| 关卡 | 名称 | 目标 | 引入机制 |
| ---- | ---- | ---- | ---- |
| 1 | 01基础迷宫 | 到达出口 | 迷宫与移动 |
| 2 | 02宝物猎人 | 收集所有宝物 **且** 到达出口 | 宝物拾取 |
| 3 | 03钥匙与门 | 到达出口 | 钥匙与门 |
| 4 | 04推箱机关 | 覆盖所有机关 **且** 到达出口 | 推巨石、地板机关 |
| 5 | 05综合挑战 | 到达出口 **且**（收集宝物 **或** 覆盖机关） | 复合目标（AND + OR） |

## 美术图集

实体精灵由 Codex（AI 工具）生成，作为 classpath 资源随应用分发，仅供本项目实验使用
（AI 生成，非商业素材，请勿外传或商用）。

- classpath 路径：`com/example/dungeonescape/images/dungeon-spritesheet.png`
  （源文件位于 `src/main/resources/com/example/dungeonescape/images/`）
- 规格：1448×1086 PNG，32 位 RGBA（透明背景），4 列 × 3 行，每格精确 362×362
- 运行时只解析一次并缓存共享，不按格重复加载、不写任何文件、不引用外部绝对路径

### 4 × 3 映射（0 基列/行）

| 列 \ 行 | 0 | 1 | 2 | 3 |
| ---- | ---- | ---- | ---- | ---- |
| 0 | Player | Wall | Exit | Treasure |
| 1 | Key | Door（关） | Door（开） | Boulder |
| 2 | FloorSwitch（未触发） | FloorSwitch（触发） | floor 地砖 | （空） |

动态状态按当前局面选格：门按开合选（1,1）或（2,1）；地板机关按
`GameState.isSwitchCovered`（被巨石压住）选（0,2）或（1,2）。墙与 floor 地砖铺满整格
48×48（避免越界/留缝），其余精灵以 44×44 上限、保留纵横比、关闭平滑插值（像素风）居中显示。

### Shape 回退

图集资源缺失、Image 解码出错或尺寸不符 1448×1086 时，`EntityViewFactory` 自动回退到
内置 Shape/Label 绘制（棋盘格地板、色块墙、圆形玩家等），应用照常启动与游玩，
不因图片问题崩溃。

### 通关插画

胜利弹窗另有通关插画素材 `victory-next-level-art.png`（classpath 路径
`com/example/dungeonescape/images/`）：1536×1024 概念图，含大标题、中部绿色传送门/
魔法光效与底部按钮区。弹窗 graphic 只裁取中部传送门/魔法光效（`ui.VictoryArt` 的
`GRAPHIC_VIEWPORT`，严格在图内并避开头尾文字按钮），素材经 SHA-256 锁定、
缺失/出错/尺寸不符时自动回退内置占位图，任何情况弹窗都有 graphic，不因图片问题失败。

## 游戏规则

- 目标格越界或为墙：移动失败，不计步
- 关闭的门：须持有编号匹配的钥匙才能开门进入，开门消耗钥匙
- 巨石：向同方向推一格，推动目标须在地图内且无墙、关闭的门或其他巨石，否则失败
- 进入目标格后自动拾取宝物；未持有钥匙时可拾取钥匙（已持有钥匙时钥匙留在地图，仍可进入该格）
- 成功移动（含开门、推石、拾取）计 1 步；失败不计步
- 每次成功移动后判定目标，达成即胜利（状态变为 WON），此后输入被忽略
- 胜利判定只看当前局面：曾踩过出口但已离开不算达成

## 关卡 JSON 格式

关卡存放于 `src/main/resources/com/example/dungeonescape/levels/`，UTF-8 编码。示例：

```json
{
  "name": "01基础迷宫",
  "width": 8,
  "height": 6,
  "entities": [
    { "type": "player", "id": "player", "x": 1, "y": 1 },
    { "type": "exit", "id": "exit-1", "x": 6, "y": 5 },
    { "type": "wall", "id": "wall-1", "x": 0, "y": 0 }
  ],
  "goal": { "type": "EXIT" }
}
```

字段说明：

| 字段 | 说明 |
| ---- | ---- |
| `name` | 关卡名称（非空白字符串） |
| `width` / `height` | 网格尺寸，允许范围 5..50 |
| `entities[]` | 实体列表，每个实体含 `type`（小写）、`id`（唯一且非空白）、`x`、`y`（在网格内） |
| `goal` | 胜利目标定义，见下方类型表 |

实体类型：`player` / `wall` / `exit` / `treasure` / `key` / `door` / `boulder` / `switch`。
`key` 需带 `keyId`，`door` 需带 `doorId`（两者匹配才算有对应钥匙），`door` 可选 `open`
（默认 `false` 关闭）。

目标类型（大写）：`EXIT` / `TREASURE` / `SWITCHES` / `AND` / `OR`。复合目标
`AND` / `OR` 通过 `children` 递归嵌套至少 2 个子目标，例如：

```json
"goal": {
  "type": "AND",
  "children": [
    { "type": "EXIT" },
    { "type": "OR", "children": [ { "type": "TREASURE" }, { "type": "SWITCHES" } ] }
  ]
}
```

加载校验（失败抛出携带来源与原因的 `InvalidLevelException`）：名称非空白、尺寸在
5..50、实体 id 唯一且坐标在网格内、恰好 1 个玩家且至少 1 个出口、实体/目标类型已知、
禁止非法初始重叠（多阻挡实体同格、玩家+墙、玩家+巨石、多玩家同格；允许玩家+出口、
玩家+机关、巨石+机关、巨石+出口）、每扇门至少存在一把同 `doorId` 的钥匙、
目标所需实体存在。**不做关卡可解性搜索**。

## 项目结构

```
dungeon-escape/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/example/dungeonescape/
    │   │   ├── DungeonEscapeApplication.java   # 入口（JavaFX Application，错误 Alert 兜底）
    │   │   ├── model/                          # 纯后端模型（不依赖 JavaFX）
    │   │   │   ├── Position / Direction / GameStatus / GameState
    │   │   │   ├── entity/                     # Player、Wall、Exit、Treasure、Key、Door、Boulder、FloorSwitch
    │   │   │   └── goal/                       # Goal 接口 + Exit/Treasure/Switch/And/Or 实现
    │   │   ├── persistence/                    # 关卡 JSON：LevelLoader、DTO、EntityFactory、GoalFactory、InvalidLevelException
    │   │   ├── service/                        # GameEngine（回合编排）、MovementService（移动规则）、事件接口与结果类型
    │   │   └── ui/                             # SceneNavigator、3 个控制器、LevelCatalog、EntityViewFactory
    │   └── resources/com/example/dungeonescape/
    │       ├── fxml/                           # main-menu.fxml、level-select.fxml、game.fxml
    │       ├── css/                            # game.css（统一深色主题）
    │       ├── images/                         # dungeon-spritesheet.png（AI 生成图集）、victory-next-level-art.png（通关插画，均见「美术图集」）
    │       └── levels/                         # level1.json ~ level5.json
    └── test/
        └── java/com/example/dungeonescape/     # 测试类（模型/服务/持久化/UI 资源与图集）
```

## 架构分层

- **model**：网格、实体与目标。`GameState` 是核心局面（尺寸、玩家、实体列表、目标、步数、状态）；
  实体基类 `Entity` 提供 id / 坐标 / 阻挡属性，`Door` 按开合动态阻挡；目标接口 `Goal` 判定胜利并提供描述。
- **service**：回合编排与规则。`GameEngine` 负责「移动 → 计步 → 目标判定 → 胜利事件」；
  `MovementService` 只实现移动/开门/推石/拾取规则，不触碰步数与状态。
- **persistence**：关卡 JSON 加载。`LevelLoader` 解析并严格校验，`EntityFactory` / `GoalFactory`
  把 DTO 转换为模型对象，全程 UTF-8，支持 classpath / 输入流 / 文件三种来源。
- **ui**：JavaFX 界面。`SceneNavigator` 统一加载 FXML / CSS 并切换三场景；
  控制器只做展示与输入转发；`EntityViewFactory` 图集可用时用缓存 Image + ImageView viewport
  切图（缺失/出错/尺寸不符自动回退纯 Shape 绘制）；`LevelCatalog` 维护 5 关映射。

## 设计模式

- **Factory（工厂）**：`persistence.EntityFactory` 按 JSON 中的类型字符串创建具体实体，
  `persistence.GoalFactory` 递归创建目标；`ui.EntityViewFactory` 把后端实体转换为 JavaFX 节点。
- **Composite（组合）**：`AndGoal` / `OrGoal` 把目标组织成可任意嵌套的树，
  对叶子与组合节点统一调用 `isSatisfied` / `description`。
- **Observer（观察者）**：`GameEventListener` 接口 + `GameEngine` 维护监听器列表，
  胜利时通知所有监听器；`GameController` 注册监听器以弹出胜利对话框
  （每局重建引擎、弹窗只弹一次）。
- **Strategy（策略）**：移动规则被封装为 `MovementService`，经构造器注入 `GameEngine`
  （测试可替换实现）；`Goal` 接口同样是可替换的目标判定策略。

## 已知限制

- 玩家只能持有一把钥匙：已持有钥匙时地图上的钥匙不会被拾取（可进入该格，钥匙保留）。
- 多扇门共用一把钥匙时，只能打开其中一扇（开门即消耗钥匙）。
- 关卡校验只保证结构合法（类型、数量、坐标、重叠、钥匙配对），不保证可解性。
- 未实现失败条件：`GameStatus.LOST` 与怪物等实体为模型预留，当前游戏没有怪物与失败机制。
- 无存档 / 读档、步数排行榜、音效与动画。
- 界面文案为中文。
- 图集素材由 AI 生成，仅供本项目实验使用；角色仅有静态单帧，无动画帧。

## 扩展方向

- 怪物与回合制战斗、陷阱等失败机制（复用 `GameStatus.LOST`）
- 存档 / 读档与步数排行榜（复用 `persistence` 的 Jackson 读写）
- 关卡编辑器与自动求解 / 可解性校验
- 更多实体（传送门、熔岩、压力板开门）与目标类型
- 音效、移动动画与粒子效果
- 无障碍：更大的字体、键盘提示与操作引导
