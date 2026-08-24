# 更新日志

本项目的重要更新均记录在此文件中。

## [1.0.7] - 2026-08-24

### 新增

- 新增挂机池物品奖励，可与经验、Vault 金币、PlayerPoints 点券和控制台命令奖励独立启停、独立调度。
- 支持按相对权重随机抽取奖励条目，并可配置每周期抽取次数、单次触发概率和随机发放数量。
- 支持每名玩家、每个挂机池的每日成功抽取次数上限；`0` 表示不限制。
- 支持 `minecraft` 原版物品配置，以及通过 `/yafk item add` 捕获玩家主手完整物品。捕获模式会保留名称、Lore、附魔、CustomModelData、PDC 等 Bukkit 可序列化元数据。
- 新增背包溢出策略：`skip` 在空间不足时取消发放，`drop` 将无法放入的物品掉落在玩家脚下。
- 新增物品奖励管理命令：
  - `/yafk item add <池ID> <奖励ID> <权重> [最小数量] [最大数量]`
  - `/yafk item remove <池ID> <奖励ID>`
  - `/yafk item list <池ID>`
  - `/yafk item test <池ID> [玩家]`
- 新增 `/yafk set` 属性：`itemEnabled`、`itemInterval`、`itemRolls`、`itemChance`、`itemMaxDaily`、`itemOverflow`。
- 新增权限 `yiyunafkpond.admin.item` 与 `yiyunafkpond.reward.item`，并补齐命令 Tab 补全。
- 新增物品奖励获得、每日上限和背包空间不足消息。

### 数据与兼容性

- 物品奖励每日次数支持 YAML、SQLite 和 MySQL 持久化，并随每日重置、管理员重置和过期数据清理同步处理。
- 物品发放通过玩家实体调度器执行，兼容 Paper 与 Folia 的线程模型。
- 无效物品材质、损坏条目和非法数值会被安全跳过并记录日志，避免单条错误配置阻止整个挂机池加载。
- 单次抽取数量限制为最多 2304 个物品，避免误配置造成大量物品栈或掉落实体。

### 优化

- 数据库驱动改为按存储类型首次运行时自动下载并缓存到 `plugins/YiyunAFKpond/libs/`，插件 JAR 不再内置 MySQL、SQLite 和 HikariCP。
- 自动下载支持阿里云、华为云和 Maven Central 顺序回退，并对下载文件执行 SHA-256 校验。
- HikariCP 下载失败时，MySQL 自动回退为 JDBC 直连；核心数据库驱动不可用时才回退为 YAML。

### 验证

- Java 21 完整构建通过。
