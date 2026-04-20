# 🌊 YiyunAFKpond

**轻量 · 高性能 · 全功能新一代挂机池插件**

为 Minecraft 服务器打造的新一代挂机奖励系统 —— 玩家步入指定区域即可自动获取经验、金币、点券等奖励，零操作、零负担。

***

## ✨ 为什么选择 YiyunAFKpond？

| <br />   | 传统挂机插件     | YiyunAFKpond                                 |
| -------- | ---------- | -------------------------------------------- |
| 区域创建     | 手写坐标，反复试错  | 🪄 **选区工具**，左键右键即可框定                         |
| 奖励类型     | 仅经验/金币     | 🎁 **经验 + 金币 + 点券 + 自定义命令**                  |
| 奖励模式     | 固定数值       | 🎲 **随机 / 固定** 双模式，可设倍率                      |
| 进度显示     | 无或简陋       | 📊 **BossBar + ActionBar + Title** 三位一体可自由选择 |
| 数据存储     | 仅 YAML     | 💾 **YAML / SQLite / MySQL** 三引擎，自带连接池       |
| Folia 支持 | ❌          | ✅ **原生 Folia 兼容**（FoliaLib 适配）               |
| 防作弊      | 基础         | 🛡️ **传送拦截 + 权限隔离 + 审计日志**                   |
| 配置热重载    | 需手动 reload | 🔥 **文件监听自动热重载**                             |

**这些设计选择背后，是对服务器性能的极致追求：**

- 🏷️ **选区工具** → 告别手动坐标，一次到位，减少配置错误导致的运行时异常
- 💾 **三引擎存储** → 小型服 YAML 易配置、中型服 SQLite 高性能、大服 MySQL + HikariCP 连接池支撑千人在线挂机
- 🚀 **Folia 原生** → 完全适配区域化 tick，充分利用多核并行，告别传统插件卡顿
- 🎯 **O(1) 区域检测** → 按世界分组 + 缓存，玩家位置检测从 O(n) 遍历降至 O(1)
- 💽 **批量异步保存** → 双缓冲队列减少大量磁盘写入，告别 I/O 阻塞
- 🎨 **统一 UI 调度** → 脏标记机制单线程批量更新，N 个玩家只需 1 个定时器
- ⚡ **毫秒级热重载** → 文件监听实时响应，零停机更新配置

***

## 🎯 核心功能

### 🏊 多池管理

- **无限挂机池** —— 按需创建，每个池独立配置
- **可视化选区** —— 金锭选区工具，左键/右键设定区域，告别手写坐标
- **独立开关** —— 单个池可随时启用/禁用，不影响其他池运行
- **权限控制** —— 每个池可设置独立进入权限，实现 VIP 专属池等场景

### 🎁 四维奖励体系

- **经验奖励** —— 支持随机/固定模式，可触发经验修补
- **金币奖励** —— 通过 Vault 发放，支持随机/固定模式
- **点券奖励** —— 通过 PlayerPoints 发放，支持随机/固定模式
- **自定义命令** —— 周期性对池内玩家执行任意命令，想象力无限

每种奖励均可独立设置：**发放间隔、随机范围、固定数值、每日上限、倍率**。

### 📊 沉浸式 UI

- **BossBar** —— 三条独立进度条（经验/金币/点券），动态变色（蓝→绿→黄→红），达到上限自动隐藏
- **ActionBar** —— 实时显示当前收益信息
- **Title** —— 进入/离开挂机池时的大字提示，支持自定义淡入淡出

### 💾 三引擎数据存储

| 引擎         | 适用场景         | 特性                   |
| ---------- | ------------ | -------------------- |
| **YAML**   | 小型服务器 / 快速上手 | 零配置，开箱即用             |
| **SQLite** | 中型服务器        | 单文件数据库，无需外部服务        |
| **MySQL**  | 大型/群组服务器     | HikariCP 连接池，高性能并发写入 |

### 🛡️ 安全与防作弊

- **传送拦截** —— 阻止玩家通过传送进入挂机池区域（可绕过权限）
- **权限隔离** —— 无权限玩家无法进入受限池
- **审计日志** —— 所有管理操作自动记录到 `audit.log`

### 🔧 开发者友好

- **完整 API** —— `YiyunAFKpondAPI` 提供池管理、玩家状态、数据查询等 30+ 接口
- **PlaceholderAPI** —— 玩家占位符 + 动态池属性占位符，灵活对接全服 UI
- **配置热重载** —— 修改配置文件自动生效，无需手动 reload

***

## 🚀 快速开始

### 环境要求

- Paper 1.20+ （或 Folia）
- Java 21+

### 可选依赖

| 插件                                                                        | 用途    |
| ------------------------------------------------------------------------- | ----- |
| [Vault](https://www.spigotmc.org/resources/vault.34315/)                  | 金币奖励  |
| [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)    | 点券奖励  |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | 变量占位符 |

### 安装

1. 下载最新版 `YiyunAFKpond.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器
4. 编辑配置文件（见下方说明）

### 30 秒创建挂机池

```
/yap selection             ← 获取选区工具
左键点击第一个角              ← 设定起点
右键点击对角                  ← 设定终点
/yap create vip_pool VIP挂机池  ← 创建完成！
```

***

## 📋 命令一览

> 主命令：`/yiyunafkpond`，别名：`/yafkpond`、`/yafk`、`/yap`

| 命令                       | 说明      | 权限                          |
| ------------------------ | ------- | --------------------------- |
| `/yap help`              | 显示帮助信息  | `yiyunafkpond.user`         |
| `/yap selection wand`    | 获取选区工具  | `yiyunafkpond.admin.select` |
| `/yap selection clear`   | 清除当前选区  | `yiyunafkpond.admin.select` |
| `/yap create <ID> <名称>`  | 创建挂机池   | `yiyunafkpond.admin.create` |
| `/yap remove <ID>`       | 删除挂机池   | `yiyunafkpond.admin.delete` |
| `/yap list`              | 列出所有挂机池 | `yiyunafkpond.admin.list`   |
| `/yap info <ID>`         | 查看池详细信息 | `yiyunafkpond.admin.info`   |
| `/yap set <ID> <属性> <值>` | 设置池属性   | `yiyunafkpond.admin.edit`   |
| `/yap toggle <ID>`       | 启用/禁用池  | `yiyunafkpond.admin.toggle` |
| `/yap tp <ID>`           | 传送到池中心  | `yiyunafkpond.admin.tp`     |
| `/yap stats`             | 全局统计    | `yiyunafkpond.admin.stats`  |
| `/yap stats <玩家>`        | 玩家统计    | `yiyunafkpond.admin.stats`  |
| `/yap stats pool <ID>`   | 池统计     | `yiyunafkpond.admin.stats`  |
| `/yap player <玩家>`       | 查看玩家数据  | `yiyunafkpond.admin.player` |
| `/yap reset <玩家>`        | 重置玩家数据  | `yiyunafkpond.admin.reset`  |
| `/yap reload`            | 重载配置    | `yiyunafkpond.admin.reload` |
| `/yap debug`             | 切换调试模式  | `yiyunafkpond.admin.debug`  |
| `/yap perf stats`        | 性能监控    | `yiyunafkpond.performance`  |

### `/yap set` 可用属性

**经验相关**

| 属性                | 说明                 | 示例                                    |
| ----------------- | ------------------ | ------------------------------------- |
| `expInterval`     | 发放间隔（秒）            | `/yap set pool1 expInterval 5`        |
| `expRewardMode`   | 奖励模式（random/fixed） | `/yap set pool1 expRewardMode fixed`  |
| `expRandomMin`    | 随机最小值              | `/yap set pool1 expRandomMin 5`       |
| `expRandomMax`    | 随机最大值              | `/yap set pool1 expRandomMax 20`      |
| `expFixedAmount`  | 固定奖励值              | `/yap set pool1 expFixedAmount 10`    |
| `expMaxDaily`     | 每日上限               | `/yap set pool1 expMaxDaily 2000`     |
| `expApplyMending` | 是否触发经验修补           | `/yap set pool1 expApplyMending true` |
| `xpRate`          | 经验倍率               | `/yap set pool1 xpRate 1.5`           |

**金币相关**

| 属性                 | 说明                 | 示例                                      |
| ------------------ | ------------------ | --------------------------------------- |
| `moneyInterval`    | 发放间隔（秒）            | `/yap set pool1 moneyInterval 30`       |
| `moneyRewardMode`  | 奖励模式（random/fixed） | `/yap set pool1 moneyRewardMode random` |
| `moneyRandomMin`   | 随机最小值              | `/yap set pool1 moneyRandomMin 5.0`     |
| `moneyRandomMax`   | 随机最大值              | `/yap set pool1 moneyRandomMax 15.0`    |
| `moneyFixedAmount` | 固定奖励值              | `/yap set pool1 moneyFixedAmount 10.0`  |
| `moneyMaxDaily`    | 每日上限               | `/yap set pool1 moneyMaxDaily 5000.0`   |
| `moneyRate`        | 金币倍率               | `/yap set pool1 moneyRate 2.0`          |

**点券相关**

| 属性                 | 说明                 | 示例                                      |
| ------------------ | ------------------ | --------------------------------------- |
| `pointInterval`    | 发放间隔（秒）            | `/yap set pool1 pointInterval 60`       |
| `pointRewardMode`  | 奖励模式（random/fixed） | `/yap set pool1 pointRewardMode random` |
| `pointRandomMin`   | 随机最小值              | `/yap set pool1 pointRandomMin 1.0`     |
| `pointRandomMax`   | 随机最大值              | `/yap set pool1 pointRandomMax 5.0`     |
| `pointFixedAmount` | 固定奖励值              | `/yap set pool1 pointFixedAmount 3.0`   |
| `pointMaxDaily`    | 每日上限               | `/yap set pool1 pointMaxDaily 100.0`    |
| `pointRate`        | 点券倍率               | `/yap set pool1 pointRate 1.0`          |

**其他**

| 属性                   | 说明              | 示例                                                        |
| -------------------- | --------------- | --------------------------------------------------------- |
| `requiredPermission` | 进入权限（null 为无限制） | `/yap set pool1 requiredPermission yiyunafkpond.pool.vip` |
| `enabled`            | 是否启用            | `/yap set pool1 enabled true`                             |
| `enterMessage`       | 进入消息            | `/yap set pool1 enterMessage &a欢迎!`                       |
| `leaveMessage`       | 离开消息            | `/yap set pool1 leaveMessage &c再见!`                       |

***

## 🔌 PlaceholderAPI 占位符

> 标识符：`yap`

### 玩家占位符

| 占位符                     | 说明         |
| ----------------------- | ---------- |
| `%yap_afk%`             | 是否在挂机（是/否） |
| `%yap_current_pond%`    | 当前挂机池名称    |
| `%yap_current_pond_id%` | 当前挂机池 ID   |
| `%yap_today_xp%`        | 今日经验       |
| `%yap_today_money%`     | 今日金币       |
| `%yap_today_point%`     | 今日点券       |
| `%yap_total_afk_time%`  | 总挂机时间（秒）   |
| `%yap_total_xp%`        | 总经验获得      |
| `%yap_total_money%`     | 总金币获得      |
| `%yap_total_point%`     | 总点券获得      |
| `%yap_pool_count%`      | 挂机池总数      |

### 池属性占位符

使用 `%yap_pond_<池ID或current>_<属性>%` 格式查询任意池的属性：

| 占位符模板                            | 说明      |
| -------------------------------- | ------- |
| `%yap_pond_current_name%`        | 当前池名称   |
| `%yap_pond_current_online%`      | 当前池在线人数 |
| `%yap_pond_current_today-xp%`    | 当前池今日经验 |
| `%yap_pond_current_today-money%` | 当前池今日金币 |
| `%yap_pond_current_today-point%` | 当前池今日点券 |
| `%yap_pond_current_xp-max%`      | 当前池经验上限 |
| `%yap_pond_current_money-max%`   | 当前池金币上限 |
| `%yap_pond_current_point-max%`   | 当前池点券上限 |
| `%yap_pond_vip-pool_online%`     | 指定池在线人数 |
| `%yap_pond_vip-pool_xp-rate%`    | 指定池经验倍率 |

> 将 `current` 替换为具体池 ID 即可查询任意池，如 `%yap_pond_vip-pool_name%`

***

## ⚙️ 配置文件
📄 config.yml — 核心配置

```yaml
core:
  debug: false
  auto-complete: true

storage:
  type: yaml                    # yaml / sqlite / mysql
  mysql:
    host: localhost
    port: 3306
    database: yiyunafkpond
    username: root
    password: password
    prefix: yiyunafkpond_
    useConnectionPool: true      # HikariCP 连接池

server-limits:
  total-exp-daily: 0            # 0 = 无上限
  total-money-daily: 100000.0
  total-point-daily: 100000.0

reset:
  enabled: true
  time: "00:00"

display:
  update-interval: 1
  player-message-mode: chat     # chat / actionbar
  bossbar:
    enabled: true
    hide-when-max: true
    hide-when-no-limit: true
    style: NOTCHED_20
    dynamic-color: true         # 蓝→绿→黄→红
  actionbar:
    enabled: false
  title:
    enabled: true

security:
  teleport-intercept:
    enabled: true
```


📄 ponds.yml — 挂机池配置

```yaml
ponds:
  vip-pool:
    name: "VIP挂机池"
    world: "world"
    minPoint: { x: 100, y: 60, z: 100 }
    maxPoint: { x: 120, y: 75, z: 120 }
    enabled: true

    expEnabled: true
    expInterval: 5
    expRandomMin: 5
    expRandomMax: 20
    expMaxDaily: 2000

    moneyEnabled: true
    moneyInterval: 30
    moneyRandomMin: 5.0
    moneyRandomMax: 15.0
    moneyMaxDaily: 5000.0

    pointEnabled: true
    pointInterval: 60
    pointRandomMin: 1.0
    pointRandomMax: 5.0
    pointMaxDaily: 100.0

    requiredPermission: "yiyunafkpond.pool.vip"

    commands:
      - "give {player} diamond 1"
    commandInterval: 120

    enterMessage: "&#87CEEB欢迎来到 &#B0E0E6{pool_name}&#87CEEB！"
    leaveMessage: "&#87CEEB感谢使用，下次再见！"
```


📄 messages.yml — 消息自定义

支持三种颜色格式：

- 传统颜色码：`&e`、`&6`
- Hex 颜色码：`&#87CEEB`、`&#B0E0E6`
- MiniMessage 标签：`<gradient:#87CEEB:#B0E0E6>渐变文字</gradient>`

所有玩家可见消息均可自定义，包括进入/离开提示、奖励通知、上限提醒等。


***

## 🧩 开发者 API

📖 完整 API 文档请参阅 [API\_REFERENCE.md](API_REFERENCE.md)

***

## 🏗️ 技术架构

```
YiyunAFKpond
├── 🎯 pond/          挂机池核心（区域检测、选区管理）
├── 🎁 reward/        奖励引擎（经验/金币/点券/命令）
├── 📊 ui/            显示系统（BossBar/ActionBar/Title）
├── 💾 data/          数据持久化（YAML/SQLite/MySQL）
├── ⏱️ scheduler/     调度管理（FoliaLib 适配）
├── 🛡️ security/     安全系统（传送拦截/权限检查）
├── 🔌 hook/          外部对接（Vault/PlayerPoints/PAPI）
├── 📝 audit/         审计日志
├── 🔧 config/        配置管理（热重载）
└── 📡 api/           开发者 API
```

**性能亮点：**

- 🧵 Java 21 Virtual Thread 支持，异步任务零阻塞
- 📦 批量保存机制，减少 I/O 压力
- 🔄 HikariCP 连接池，MySQL 高并发写入
- 🎯 缓存坐标检测，区域判断 O(1) 复杂度
- 🧹 自动清理不活跃玩家数据与过期记录

***

## 📜 权限一览

| 权限                             | 说明       | 默认  |
| ------------------------------ | -------- | --- |
| `yiyunafkpond.use`             | 使用插件基本命令 | 所有人 |
| `yiyunafkpond.user`            | 查看帮助信息   | 所有人 |
| `yiyunafkpond.admin`           | 管理员总权限   | OP  |
| `yiyunafkpond.admin.create`    | 创建挂机池    | OP  |
| `yiyunafkpond.admin.edit`      | 编辑挂机池    | OP  |
| `yiyunafkpond.admin.delete`    | 删除挂机池    | OP  |
| `yiyunafkpond.admin.list`      | 列出挂机池    | OP  |
| `yiyunafkpond.admin.info`      | 查看池信息    | OP  |
| `yiyunafkpond.admin.select`    | 使用选区工具   | OP  |
| `yiyunafkpond.admin.reload`    | 重载配置     | OP  |
| `yiyunafkpond.admin.toggle`    | 启用/禁用池   | OP  |
| `yiyunafkpond.admin.tp`        | 传送到池     | OP  |
| `yiyunafkpond.admin.player`    | 管理玩家数据   | OP  |
| `yiyunafkpond.admin.stats`     | 查看统计     | OP  |
| `yiyunafkpond.admin.reset`     | 重置玩家数据   | OP  |
| `yiyunafkpond.admin.debug`     | 调试模式     | OP  |
| `yiyunafkpond.bypass.teleport` | 绕过传送拦截   | OP  |
| `yiyunafkpond.pool.*`          | 进入所有池    | OP  |
| `yiyunafkpond.reward.money`    | 获得金币奖励   | 所有人 |
| `yiyunafkpond.reward.point`    | 获得点券奖励   | 所有人 |
| `yiyunafkpond.reward.commands` | 获得命令奖励   | 所有人 |
| `yiyunafkpond.performance`     | 性能监控     | OP  |

***

## 📞 联系我们

- **作者**: CHL_chun
- **GitHub**: [CHL_chun](https://github.com/Chun2919089965)
- **QQ**: 2919089965
- **插件交流Q群**: 1093090518

***

**YiyunAFKpond** © [CHL_chun](https://github.com/Chun2919089965) · 让挂机也成为一种享受 🌊
