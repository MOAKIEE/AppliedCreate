# Applied Create - 动力合成编码器独立移植方案

## 概述

本项目将"齿轮盛宴整合包"（Create-Delight-Remake）中的**动力合成编码器**（Mechanical Craft Encoder）从 MBD2 + KubeJS 实现移植为一个独立的 Forge Mod，使其可以脱离整合包独立运行。

## 原始实现分析

### 三层架构

动力合成编码器在原整合包中由三层技术栈实现：

| 层级 | 技术 | 职责 |
|------|------|------|
| 方块定义层 | MBD2 `.sm` 状态机文件 | 方块注册、GUI 定义（81个输入槽 + 1个过滤槽 + 1个输出槽）、状态机、模型 |
| 业务逻辑层 | KubeJS `MBDMachineEvents` | 配方匹配、物品消耗、包裹输出 |
| 数据层 | KubeJS 服务端/客户端脚本 | 合成配方、可挖掘标签、Ctrl 提示文字 |

### 核心逻辑

源自 `kubejs/server_scripts/mbd2/mechanical_craft_encoder.js`：

1. **触发条件**：每 30 tick 检测一次 + 需要红石信号，或方块邻居变化 + 红石信号
2. **配方搜索**：扫描所有 `create:mechanical_crafting` 类型配方
3. **过滤功能**：过滤槽中放入物品可按输出物品过滤配方
4. **宽度控制**：GUI 中的文本框可设置 1-9 的宽度值，按最接近的宽度排序配方
5. **物品消耗**：通过快照/恢复机制模拟消耗检查可行性
6. **输出产物**：生成 Create 的 `PackageItem`（包裹物品），内含 `PackageOrderWithCrafts` NBT 数据

### 关键 Create API

- `com.simibubi.create.content.logistics.box.PackageItem` — `containing(ItemStackHandler)` 静态方法
- `com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts` — `singleRecipe(List<BigItemStack>)`
- `com.simibubi.create.content.logistics.BigItemStack` — 构造函数接受 `ItemStack`
- `com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe` — 继承 ShapedRecipe

以上 4 个类均已确认存在于 Create 1.20.1 版本 6.0.8 中。

### 原始合成配方

```
A A B A A
A B C B A
B C D C B
A B C B A
A A B A A
```

- A = `create:brass_sheet`（黄铜板）
- B = `ae2:molecular_assembler`（分子装配室）
- C = `create:brass_casing`（黄铜机壳）
- D = `create:factory_gauge`（工厂仪表）

## 移植方案

### 技术选型

| 项目 | 选择 | 理由 |
|------|------|------|
| Mod 加载器 | Forge (MinecraftForge) | 用户指定 |
| Minecraft 版本 | 1.20.1 | 用户指定 |
| 开发语言 | Kotlin（零 Java 代码） | 用户指定 |
| 构建脚本 | Gradle Kotlin DSL (.gradle.kts) | 用户指定 |
| Kotlin 运行时 | KotlinForForge 4.11.0 | Forge 1.20.1 的 Kotlin 适配 |
| Mappings | Parchment 2023.09.03-1.20.1 | 提供人类可读的参数名 |
| Create 依赖 | 6.0.8 (compile-only) | 不捆绑，运行时由用户安装 |

### 模块结构

```
src/main/kotlin/com/loliball/appliedcreate/
├── AppliedCreate.kt              # 主 Mod 类，DeferredRegister 注册
├── block/
│   ├── MechanicalCraftEncoderBlock.kt      # 方块类
│   └── entity/
│       └── MechanicalCraftEncoderBlockEntity.kt  # 方块实体（核心逻辑）
└── gui/
    ├── MechanicalCraftEncoderMenu.kt       # 容器/菜单
    └── MechanicalCraftEncoderScreen.kt     # 客户端 GUI 渲染

src/main/resources/
├── META-INF/mods.toml
├── pack.mcmeta
├── assets/appliedcreate/
│   ├── blockstates/mechanical_craft_encoder.json
│   ├── models/block/mechanical_craft_encoder.json
│   ├── models/item/mechanical_craft_encoder.json
│   ├── textures/block/mechanical_craft_encoder.png  # 占位纹理
│   ├── textures/gui/mechanical_craft_encoder.png    # GUI 纹理
│   └── lang/
│       ├── en_us.json
│       └── zh_cn.json
└── data/
    ├── appliedcreate/recipes/mechanical_craft_encoder.json
    └── minecraft/tags/blocks/mineable/pickaxe.json
```

### 移植要点

#### 1. 方块注册（取代 MBD2 .sm 文件）

使用 Forge 的 `DeferredRegister` 注册方块、方块物品、方块实体类型、菜单类型和创造模式标签页。

#### 2. 方块属性

- 硬度 1.5、抗爆性 6.0
- 可用镐挖掘
- 水平旋转（HORIZONTAL_FACING）

#### 3. GUI 系统（取代 MBD2 GUI 定义）

使用 Forge 标准的 `AbstractContainerMenu` + `AbstractContainerScreen` 实现：
- 9×9 输入网格（81 槽位）
- 1 个过滤槽
- 1 个输出槽
- 宽度控制字段（整数 1-9）
- 玩家背包 + 快捷栏

#### 4. 核心逻辑（取代 KubeJS 脚本）

将 JavaScript 业务逻辑直接翻译为 Kotlin，在 `BlockEntity.tick()` 中实现：
- 30 tick 间隔计时器
- 红石信号检测
- 配方遍历与匹配
- 物品快照/恢复模拟
- PackageItem 生成与 NBT 写入

#### 5. 依赖关系

- **运行时必须**：Forge 47.x、Minecraft 1.20.1、Create 6.0.8、KotlinForForge 4.x
- **运行时可选**：AE2（仅合成配方需要 `ae2:molecular_assembler`）
- **编译时**：Create slim jar（不打包进最终 mod）

### 与原版的差异

1. **无 MBD2 依赖**：使用原生 Forge 方块系统替代 MBD2 状态机
2. **无 KubeJS 依赖**：业务逻辑编译为 Kotlin 字节码，性能更优
3. **独立安装**：只需 Forge + Create + KotlinForForge
4. **占位纹理**：初始版本使用简单纹理，后续可替换为精美资源
5. **AE2 可选**：合成配方中的分子装配室需要 AE2，但编码器本身不依赖 AE2 运行

## 构建与安装

```bash
# 构建
./gradlew build

# 输出 JAR 位于
build/libs/appliedcreate-1.0.0.jar
```

安装步骤：
1. 安装 Forge 1.20.1 (47.x)
2. 安装 Kotlin for Forge 4.x
3. 安装 Create 6.0.8
4. 将 `appliedcreate-1.0.0.jar` 放入 `mods/` 文件夹
