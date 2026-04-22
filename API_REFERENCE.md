<div align="center">

# 📡 YiyunAFKpond Developer API

**版本 1.0.0** · 面向第三方插件开发者的完整接口文档

---

## 快速接入

### 1. 添加依赖

**Maven**

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Chun2919089965</groupId>
    <artifactId>YiyunAFKpond</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Chun2919089965:YiyunAFKpond:1.0.0'
}
```

### 2. 获取 API 实例

```java
import com.yiyunafkpond.api.YiyunAFKpondAPI;

public class MyPlugin extends JavaPlugin {

    private YiyunAFKpondAPI afkApi;

    @Override
    public void onEnable() {
        this.afkApi = YiyunAFKpondAPI.get();

        if (afkApi == null) {
            getLogger().warning("YiyunAFKpond 未安装或未初始化！");
            return;
        }

        if (!afkApi.isFullyInitialized()) {
            getLogger().warning("YiyunAFKpond 尚未完全初始化，请稍后重试！");
            return;
        }

        getLogger().info("已成功对接 YiyunAFKpond API v" + afkApi.getPluginVersion());
    }
}
```

> **重要**：务必在 `onEnable` 中检查 API 是否为 `null` 以及 `isFullyInitialized()` 是否为 `true`。YiyunAFKpond 在服务器启动时通过 `ServerLoadEvent` 自动完成初始化，核心模块在插件加载后 5 ticks 即就绪，挂机池数据在服务器启动完成后自动加载。

### 3. plugin.yml 声明软依赖

```yaml
softdepend:
  - YiyunAFKpond
```

---

## API 完整参考

### 📦 池管理

#### `getAllPonds()`

获取所有挂机池的列表。

```java
List<Pond> ponds = api.getAllPonds();
for (Pond pond : ponds) {
    getLogger().info(pond.getName() + " - " + pond.getId());
}
```

| 返回值 | 说明 |
|---|---|
| `List<Pond>` | 所有挂机池的副本列表（修改不影响内部数据） |

---

#### `getPondById(String pondId)`

根据 ID 获取挂机池对象。

```java
Pond pond = api.getPondById("vip-pool");
if (pond != null) {
    getLogger().info("池名称: " + pond.getName());
    getLogger().info("是否启用: " + pond.isEnabled());
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池的唯一标识 |
| **返回值** | `Pond` | 池对象，不存在则返回 `null` |

---

#### `pondExists(String pondId)`

检查指定 ID 的挂机池是否存在。

```java
if (api.pondExists("vip-pool")) {
    getLogger().info("VIP 池存在！");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池的唯一标识 |
| **返回值** | `boolean` | 存在返回 `true` |

---

#### `getAllPondIds()`

获取所有挂机池的 ID 列表。

```java
List<String> ids = api.getAllPondIds();
// ["vip-pool", "free-pool", "event-pool"]
```

| 返回值 | 说明 |
|---|---|
| `List<String>` | 所有池 ID 的列表 |

---

#### `getPondCount()`

获取挂机池总数。

```java
int count = api.getPondCount();
```

| 返回值 | 说明 |
|---|---|
| `int` | 挂机池数量 |

---

#### `getPondByLocation(Location location)`

根据坐标获取该位置所属的挂机池。

```java
Location loc = player.getLocation();
Pond pond = api.getPondByLocation(loc);
if (pond != null) {
    player.sendMessage("你正在 " + pond.getName() + " 中！");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `location` | `Location` | 要检测的坐标 |
| **返回值** | `Pond` | 该位置的池，不在任何池内则返回 `null` |

> 仅返回**已启用**的池。

---

#### `createPond(...)`

创建一个新的挂机池。创建成功后会自动保存配置并启动奖励任务。

```java
Pond pond = api.createPond(
    "event-pool",       // 池 ID（唯一标识）
    "活动挂机池",        // 池名称
    "world",            // 世界名称
    100, 60, 100,       // 最小点 (x, y, z)
    120, 75, 120        // 最大点 (x, y, z)
);

if (pond != null) {
    pond.setExpInterval(3);
    pond.setExpRandomMin(10);
    pond.setExpRandomMax(50);
    pond.setMoneyEnabled(true);
    pond.setMoneyInterval(15);
    getLogger().info("活动池创建成功！");
} else {
    getLogger().warning("创建失败：ID 已存在或世界不存在");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 池唯一标识（字母、数字、下划线） |
| `name` | `String` | 池显示名称 |
| `worldName` | `String` | 世界名称 |
| `minX, minY, minZ` | `double` | 区域最小坐标 |
| `maxX, maxY, maxZ` | `double` | 区域最大坐标 |
| **返回值** | `Pond` | 创建成功返回池对象，失败返回 `null` |

> 坐标无需按大小排序，API 会自动调整。创建后可通过 Pond 的 setter 方法进一步配置属性。

---

#### `deletePond(String pondId)`

删除一个挂机池。删除前会自动停止奖励任务并将池内玩家移出。

```java
boolean success = api.deletePond("event-pool");
if (success) {
    getLogger().info("活动池已删除");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池 ID |
| **返回值** | `boolean` | 删除成功返回 `true` |

> 删除操作会将池内所有玩家移出挂机状态，并自动保存配置。

---

#### `togglePond(String pondId)`

切换挂机池的启用/禁用状态。

```java
boolean newState = api.togglePond("vip-pool");
getLogger().info("VIP 池当前状态: " + (newState ? "启用" : "禁用"));
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池 ID |
| **返回值** | `boolean` | 切换后的状态（`true` = 启用） |

---

#### `getPlayersInPond(String pondId)`

获取指定池内的所有在线玩家。

```java
List<Player> players = api.getPlayersInPond("vip-pool");
getLogger().info("VIP 池当前有 " + players.size() + " 人");
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池 ID |
| **返回值** | `List<Player>` | 池内玩家列表，池不存在则返回空列表 |

---

#### `getPlayerCountInPond(String pondId)`

获取指定池内的在线玩家数量（比 `getPlayersInPond().size()` 更高效）。

```java
int count = api.getPlayerCountInPond("vip-pool");
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `pondId` | `String` | 池 ID |
| **返回值** | `int` | 玩家数量 |

---

### 👤 玩家状态查询

#### `isPlayerAfk(Player player)`

检查玩家是否处于挂机状态。

```java
if (api.isPlayerAfk(player)) {
    player.sendMessage("你正在挂机中！");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `player` | `Player` | 玩家对象 |
| **返回值** | `boolean` | 挂机中返回 `true` |

---

#### `isPlayerInAnyPond(Player player)`

检查玩家是否在任意挂机池内。

```java
boolean inPool = api.isPlayerInAnyPond(player);
```

---

#### `isPlayerInPond(Player player, String pondId)`

检查玩家是否在指定的挂机池内。

```java
if (api.isPlayerInPond(player, "vip-pool")) {
    player.sendMessage("你在 VIP 池中！");
}
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `player` | `Player` | 玩家对象 |
| `pondId` | `String` | 池 ID |
| **返回值** | `boolean` | 在指定池内返回 `true` |

---

#### `getPlayerCurrentPond(Player player)`

获取玩家当前所在的挂机池对象。

```java
Pond pond = api.getPlayerCurrentPond(player);
if (pond != null) {
    player.sendMessage("当前池: " + pond.getName());
}
```

| 返回值 | 说明 |
|---|---|
| `Pond` | 当前池对象，不在池内返回 `null` |

---

#### `getPlayerCurrentPondId(Player player)`

获取玩家当前所在挂机池的 ID（比 `getPlayerCurrentPond` 更轻量）。

```java
String pondId = api.getPlayerCurrentPondId(player);
// "vip-pool" 或 null
```

| 返回值 | 说明 |
|---|---|
| `String` | 池 ID，不在池内返回 `null` |

---

### 📊 玩家数据查询

#### 总计数据

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerTotalAfkTime(Player)` | `long` | 总挂机时间（秒） |
| `getPlayerTotalXp(Player)` | `long` | 总经验获得 |
| `getPlayerTotalMoney(Player)` | `double` | 总金币获得 |
| `getPlayerTotalPoint(Player)` | `int` | 总点券获得 |

```java
long totalSeconds = api.getPlayerTotalAfkTime(player);
double totalMoney = api.getPlayerTotalMoney(player);
```

#### 今日数据（全局）

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerTodayXp(Player)` | `long` | 今日经验 |
| `getPlayerTodayMoney(Player)` | `double` | 今日金币 |
| `getPlayerTodayPoint(Player)` | `int` | 今日点券 |

#### 今日数据（按池）

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerTodayXpInPond(Player, String)` | `long` | 在指定池的今日经验 |
| `getPlayerTodayMoneyInPond(Player, String)` | `double` | 在指定池的今日金币 |
| `getPlayerTodayPointInPond(Player, String)` | `int` | 在指定池的今日点券 |

```java
long xpInVip = api.getPlayerTodayXpInPond(player, "vip-pool");
double moneyInFree = api.getPlayerTodayMoneyInPond(player, "free-pool");
```

#### 按池挂机时间

```java
long afkInVip = api.getPlayerAfkTimeInPond(player, "vip-pool");
```

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerAfkTimeInPond(Player, String)` | `long` | 在指定池的挂机时间（秒） |

#### 各池挂机时间汇总

```java
Map<String, Long> afkTimes = api.getPlayerPondAfkTimes(player);
// {"vip-pool": 3600, "free-pool": 7200}  (单位: 毫秒)
```

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerPondAfkTimes(Player)` | `Map<String, Long>` | 各池挂机时间（毫秒，不可修改） |

> 返回的是不可修改的 Map 副本，时间单位为毫秒。

#### 直接获取 PlayerData

```java
PlayerData data = api.getPlayerData(player);
if (data != null) {
    // 访问 PlayerData 的所有方法
    data.getPoolTodayExp();
    data.getPoolTodayMoney();
    data.getPoolTodayPoint();
}
```

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPlayerData(Player)` | `PlayerData` | 玩家数据对象，未加载则返回 `null` |

> **注意**：`PlayerData` 对象是可变的，直接修改可能影响插件内部状态。建议只读取数据，写入操作请使用 API 提供的方法。

---

### ⚡ 玩家状态操作

#### `setPlayerAfk(Player player, String pondId)`

将玩家设置为挂机状态并关联到指定池。

```java
api.setPlayerAfk(player, "vip-pool");
```

| 参数 | 类型 | 说明 |
|---|---|---|
| `player` | `Player` | 玩家对象 |
| `pondId` | `String` | 要关联的池 ID |

> 此操作会自动注册 UI 更新并排队保存数据。**不会**检查玩家是否实际在该池区域内，也不会触发进入消息。如需完整流程，建议通过传送将玩家移入池区域。

---

#### `setPlayerNotAfk(Player player)`

将玩家设置为非挂机状态。

```java
api.setPlayerNotAfk(player);
```

> 此操作会清除玩家的当前池关联，注销 UI 更新并排队保存数据。**不会**触发离开消息。

---

#### `resetPlayerDailyData(Player player)`

重置玩家的今日数据（经验、金币、点券归零）。

```java
api.resetPlayerDailyData(player);
```

> 重置后 BossBar 等显示会自动更新。

---

### 🌐 全局查询

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getAllAfkPlayers()` | `List<Player>` | 所有处于挂机状态的在线玩家 |
| `getTotalAfkPlayerCount()` | `int` | 当前挂机玩家总数 |

```java
List<Player> afkPlayers = api.getAllAfkPlayers();
int count = api.getTotalAfkPlayerCount();
```

---

### 🏛️ 服务器限制

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getServerExpDailyLimit()` | `long` | 服务器经验每日总上限（0 = 无限制） |
| `getServerMoneyDailyLimit()` | `double` | 服务器金币每日总上限 |
| `getServerPointDailyLimit()` | `double` | 服务器点券每日总上限 |

```java
long expLimit = api.getServerExpDailyLimit();
if (expLimit > 0) {
    getLogger().info("经验每日上限: " + expLimit);
}
```

---

### ℹ️ 插件信息

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `getPluginVersion()` | `String` | 插件版本号 |
| `isDebugMode()` | `boolean` | 是否处于调试模式 |
| `isFullyInitialized()` | `boolean` | 插件是否完全初始化 |

---

## 🏊 Pond 对象参考

`Pond` 是挂机池的核心数据对象，通过 API 获取后可直接读取其属性：

### 基础属性

| Getter | 返回类型 | 说明 |
|---|---|---|
| `getId()` | `String` | 池唯一标识 |
| `getName()` | `String` | 池显示名称 |
| `getWorld()` | `World` | 所属世界 |
| `getWorldName()` | `String` | 世界名称 |
| `isEnabled()` | `boolean` | 是否启用 |
| `getSize()` | `int` | 区域方块数 |
| `getCenterLocation()` | `Location` | 区域中心坐标 |
| `getRequiredPermission()` | `String` | 进入权限（`null` = 无限制） |

### 奖励配置

| Getter | 返回类型 | 说明 |
|---|---|---|
| `isExpEnabled()` | `boolean` | 经验奖励是否启用 |
| `isMoneyEnabled()` | `boolean` | 金币奖励是否启用 |
| `isPointEnabled()` | `boolean` | 点券奖励是否启用 |
| `getExpInterval()` | `long` | 经验发放间隔（秒） |
| `getMoneyInterval()` | `long` | 金币发放间隔（秒） |
| `getPointInterval()` | `long` | 点券发放间隔（秒） |
| `getExpRewardMode()` | `String` | 经验模式（"random" / "fixed"） |
| `getExpRandomMin()` | `long` | 经验随机最小值 |
| `getExpRandomMax()` | `long` | 经验随机最大值 |
| `getExpFixedAmount()` | `long` | 经验固定值 |
| `getExpMaxDaily()` | `long` | 每日经验上限（0 = 无限制） |
| `getMoneyRandomMin()` | `double` | 金币随机最小值 |
| `getMoneyRandomMax()` | `double` | 金币随机最大值 |
| `getMoneyFixedAmount()` | `double` | 金币固定值 |
| `getMoneyMaxDaily()` | `double` | 每日金币上限 |
| `getPointRandomMin()` | `double` | 点券随机最小值 |
| `getPointRandomMax()` | `double` | 点券随机最大值 |
| `getPointFixedAmount()` | `double` | 点券固定值 |
| `getPointMaxDaily()` | `double` | 每日点券上限 |
| `getXpRate()` | `double` | 经验倍率 |
| `getMoneyRate()` | `double` | 金币倍率 |
| `getPointRate()` | `double` | 点券倍率 |
| `isExpApplyMending()` | `boolean` | 经验是否触发经验修补 |

### 命令与消息

| Getter | 返回类型 | 说明 |
|---|---|---|
| `getCommands()` | `List<String>` | 自定义命令列表 |
| `getCommandInterval()` | `long` | 命令执行间隔（秒） |
| `getEnterMessage()` | `String` | 进入消息模板 |
| `getLeaveMessage()` | `String` | 离开消息模板 |

### 区域检测

```java
Pond pond = api.getPondById("vip-pool");
boolean inside = pond.isInPond(player.getLocation());
```

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `isInPond(Location)` | `boolean` | 检查坐标是否在池区域内 |
| `getMinPoint()` | `Location` | 区域最小点 |
| `getMaxPoint()` | `Location` | 区域最大点 |

---

## 💡 常见场景

### 场景一：检测玩家进入挂机池后给予额外奖励

```java
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    if (!event.hasChangedBlock()) return;

    Player player = event.getPlayer();
    YiyunAFKpondAPI api = YiyunAFKpondAPI.get();
    if (api == null) return;

    if (!api.isPlayerAfk(player)) return;

    Pond pond = api.getPlayerCurrentPond(player);
    if (pond == null) return;

    // 首次进入该池时给予欢迎礼包
    if (api.getPlayerAfkTimeInPond(player, pond.getId()) < 2) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "give " + player.getName() + " golden_apple 1");
    }
}
```

### 场景二：定时广播挂机池状态

```java
public void broadcastPoolStatus() {
    YiyunAFKpondAPI api = YiyunAFKpondAPI.get();
    if (api == null) return;

    for (Pond pond : api.getAllPonds()) {
        if (!pond.isEnabled()) continue;

        int count = api.getPlayerCountInPond(pond.getId());
        if (count > 0) {
            Bukkit.broadcastMessage(
                "§b" + pond.getName() + " §7当前有 §f" + count + " §7人挂机中"
            );
        }
    }
}
```

### 场景三：为 VIP 玩家动态创建活动池

```java
public void createEventPool(String eventId, Location corner1, Location corner2) {
    YiyunAFKpondAPI api = YiyunAFKpondAPI.get();
    if (api == null) return;

    Pond pond = api.createPond(
        "event-" + eventId,
        "活动池-" + eventId,
        corner1.getWorld().getName(),
        corner1.getX(), corner1.getY(), corner1.getZ(),
        corner2.getX(), corner2.getY(), corner2.getZ()
    );

    if (pond != null) {
        pond.setExpInterval(3);
        pond.setExpRandomMin(20);
        pond.setExpRandomMax(100);
        pond.setMoneyEnabled(true);
        pond.setMoneyInterval(10);
        pond.setMoneyRandomMin(10.0);
        pond.setMoneyRandomMax(50.0);
        pond.setRequiredPermission("yiyunafkpond.pool.event-" + eventId);

        api.togglePond(pond.getId());
    }
}
```

### 场景四：查询玩家在各池的收益分布

```java
public void showPlayerEarnings(Player player) {
    YiyunAFKpondAPI api = YiyunAFKpondAPI.get();
    if (api == null) return;

    player.sendMessage("§6=== 你的挂机收益 ===");
    player.sendMessage("§e今日经验: §f" + api.getPlayerTodayXp(player));
    player.sendMessage("§e今日金币: §f" + String.format("%.2f", api.getPlayerTodayMoney(player)));
    player.sendMessage("§e今日点券: §f" + api.getPlayerTodayPoint(player));

    Map<String, Long> afkTimes = api.getPlayerPondAfkTimes(player);
    for (Map.Entry<String, Long> entry : afkTimes.entrySet()) {
        String pondId = entry.getKey();
        long seconds = entry.getValue() / 1000;
        long xpInPond = api.getPlayerTodayXpInPond(player, pondId);
        player.sendMessage("§7- " + pondId + ": §f" + seconds + "秒, §a+" + xpInPond + "经验");
    }
}
```

---

## ⚠️ 注意事项

1. **线程安全**：所有 API 方法均可在主线程安全调用。内部涉及数据库的操作已通过异步队列处理。
2. **空值检查**：始终检查 `YiyunAFKpondAPI.get()` 是否为 `null`，以及 `isFullyInitialized()` 是否为 `true`。
3. **数据一致性**：通过 `setPlayerAfk` / `setPlayerNotAfk` 修改状态不会触发进入/离开消息和 Title 显示。如需完整流程，请将玩家传送到池区域内。
4. **Pond 对象可变性**：通过 API 获取的 `Pond` 对象是引用而非副本，通过 setter 修改属性会影响插件内部状态。修改后需调用 `plugin.getPondManager().savePonds(true)` 持久化。
5. **PlayerData 只读建议**：通过 `getPlayerData()` 获取的 `PlayerData` 对象可直接修改，但建议仅用于读取。写入操作请优先使用 API 方法，以确保 UI 更新和数据保存的一致性。
6. **废弃方法**：`YiyunAFKpond.getAPI()` 已标记为 `@Deprecated`，请使用 `YiyunAFKpondAPI.get()` 静态方法。

---

**YiyunAFKpond API** © [CHL_chun](https://github.com/Chun2919089965)
