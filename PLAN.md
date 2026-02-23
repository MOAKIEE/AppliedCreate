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

---

## Phase 2 — 黄铜样板供应器（Brass Pattern Provider）

### 概述

新增**黄铜样板供应器**（Brass Pattern Provider）方块，作为 AE2 ME 网络与 Create 动力合成器阵列之间的桥梁。它接收 AE2 处理样板（processing pattern），根据样板的产物匹配 `create:mechanical_crafting` 配方，然后从 ME 网络提取原材料，按正确的配方网格布局直接插入到相邻的动力合成器阵列中。

### 需求

1. 使用 AE2 **处理样板**（processing pattern），AE2 作为必需依赖
2. 玩家需要将多个动力合成器使用扳手链接成一个整体
3. 黄铜样板供应器必须紧贴动力合成器阵列放置
4. 原材料来自 AE 网络，与普通的 ME 样板供应器逻辑保持一致
5. 遵循 AE 默认的自动请求材料获取
6. 不输出包裹，直接将材料以正确的配方形式输出到紧贴着的动力合成器中
7. 通过样板中的**产物信息**解决配方歧义（相同原料不同产物的配方）

### AE2 API 分析

通过反编译 AE2 Forge 15.4.10 的 jar 文件，确认以下关键接口和类：

#### ICraftingProvider（核心接口）

```java
public interface ICraftingProvider extends IGridNodeService {
    List<IPatternDetails> getAvailablePatterns();    // 向网络报告可用样板
    default int getPatternPriority();                // 样板优先级
    boolean pushPattern(IPatternDetails, KeyCounter[]); // 网络推送合成任务
    boolean isBusy();                                // 是否繁忙（正在处理）
    default Set<AEKey> getEmitableItems();           // 可发射的物品
    static void requestUpdate(IManagedGridNode);     // 通知网络刷新样板
}
```

#### IPatternDetails（样板详情）

```java
public interface IPatternDetails {
    AEItemKey getDefinition();           // 样板物品本身
    IInput[] getInputs();               // 输入材料列表
    GenericStack getPrimaryOutput();     // 主要产物
    GenericStack[] getOutputs();        // 所有产物
    boolean supportsPushInputsToExternalInventory();
    void pushInputsToExternalInventory(KeyCounter[], PatternInputSink);
}
```

#### PatternProviderLogic（样板供应器逻辑 — 可复用）

```java
public class PatternProviderLogic implements InternalInventoryHost, ICraftingProvider {
    public PatternProviderLogic(IManagedGridNode, PatternProviderLogicHost);
    public PatternProviderLogic(IManagedGridNode, PatternProviderLogicHost, int numPatternSlots);
    public List<IPatternDetails> getAvailablePatterns();
    public boolean pushPattern(IPatternDetails, KeyCounter[]);
    public boolean isBusy();
    public InternalInventory getPatternInv();
    // ... NBT, config, return inventory 等
}
```

#### PatternProviderLogicHost（宿主接口）

```java
public interface PatternProviderLogicHost extends IConfigurableObject, IPriorityHost, PatternContainer {
    PatternProviderLogic getLogic();
    BlockEntity getBlockEntity();
    EnumSet<Direction> getTargets();   // 输出方向
    void saveChanges();
    AEItemKey getTerminalIcon();
    // ... 终端显示、菜单、优先级等
}
```

#### AENetworkBlockEntity（基类）

```java
public class AENetworkBlockEntity extends AEBaseBlockEntity implements IGridConnectedBlockEntity {
    public IManagedGridNode getMainNode();  // ME 网格节点
    public void onReady();                 // 加入网格
    // ... load/save NBT
}
```

### Create 动力合成器 API 分析

#### MechanicalCrafterBlockEntity

```java
public class MechanicalCrafterBlockEntity extends KineticBlockEntity {
    public MechanicalCrafterBlockEntity.Inventory getInventory();    // 1 槽物品
    public ConnectedInputHandler.ConnectedInput getInput();          // 链接信息
    public <T> LazyOptional<T> getCapability(Capability<T>, Direction); // IItemHandler 能力
}
```

#### ConnectedInput（链接组管理）

```java
public class ConnectedInputHandler.ConnectedInput {
    public void attachTo(BlockPos controllerPos, BlockPos selfPos);
    public IItemHandler getItemHandler(Level, BlockPos);              // 合并后的物品处理器
    public List<Inventory> getInventories(Level, BlockPos);           // 按顺序的所有子库存
}
```

#### 物品插入流程

1. 获取相邻方块的 `MechanicalCrafterBlockEntity`
2. 通过 `getInput()` 获取 `ConnectedInput`，找到控制器和所有链接成员
3. 通过 `RecipeGridHandler.getAllCraftersOfChain()` 获取链中所有合成器
4. 每个合成器的 `getInventory()` 返回 1 槽的 `SmartInventory`
5. 通过 `insertItem()` 插入物品到对应位置
6. 槽位排序：按 Y 降序、X 按朝向轴排序（与配方网格对应）

### 实现方案

#### 方案选择：自定义 ICraftingProvider + 直接插入

**不复用** `PatternProviderLogic`，原因：
- `PatternProviderLogic.pushPattern()` 内部通过 `PatternProviderTarget` 向相邻库存推送物品，使用 `insert(AEKey, long, Actionable)` 逐个推送
- 我们需要将物品**按网格位置**插入到特定合成器的特定槽位，而非简单地逐个推送到相邻库存
- 自定义实现允许我们精确控制物品的插入位置

#### 核心流程

```
玩家放入处理样板 → 黄铜样板供应器注册到 ME 网络
                → 网络发起合成请求
                → pushPattern(pattern, inputs) 被调用
                → 读取 pattern.getPrimaryOutput() 获取目标产物
                → 遍历所有 create:mechanical_crafting 配方
                → 找到产物匹配的配方
                → 获取配方的网格布局（width × height + ingredients）
                → 检测相邻的动力合成器阵列
                → 获取阵列的网格尺寸和各位置的合成器
                → 将 inputs 中的材料按配方网格布局插入到对应合成器
                → 报告 isBusy() = true
                → 等待合成完成（监听合成器状态变化或结果产物出现）
                → 报告合成完成，将结果返回 ME 网络
```

#### 类结构

```
src/main/kotlin/com/loliball/appliedcreate/
├── AppliedCreate.kt                             # 新增注册
├── block/
│   ├── MechanicalCraftEncoderBlock.kt           # （已有）
│   ├── BrassPatternProviderBlock.kt             # 新增
│   └── entity/
│       ├── MechanicalCraftEncoderBlockEntity.kt # （已有）
│       └── BrassPatternProviderBlockEntity.kt   # 新增 — 核心逻辑
└── gui/
    ├── MechanicalCraftEncoderMenu.kt            # （已有）
    ├── MechanicalCraftEncoderScreen.kt          # （已有）
    ├── BrassPatternProviderMenu.kt              # 新增
    └── BrassPatternProviderScreen.kt            # 新增
```

#### BrassPatternProviderBlockEntity 核心设计

```kotlin
class BrassPatternProviderBlockEntity : AENetworkBlockEntity, ICraftingProvider {
    // 9 个样板槽（与 AE2 标准样板供应器一致）
    private val patternInventory: InternalInventory  // AE2 的 AppEngInternalInventory
    private var patterns: List<IPatternDetails>       // 解码后的样板
    private var busy: Boolean = false                 // 是否正在处理合成

    // ICraftingProvider 实现
    override fun getAvailablePatterns(): List<IPatternDetails> = patterns
    override fun isBusy(): Boolean = busy

    override fun pushPattern(pattern: IPatternDetails, inputs: Array<KeyCounter>): Boolean {
        // 1. 获取样板产物
        val output = pattern.primaryOutput ?: return false
        val outputItem = (output.what() as? AEItemKey)?.toStack() ?: return false

        // 2. 查找匹配的 mechanical_crafting 配方
        val recipe = findMatchingRecipe(outputItem) ?: return false

        // 3. 检测相邻动力合成器阵列
        val crafterArray = detectAdjacentCrafterArray() ?: return false

        // 4. 验证阵列尺寸与配方匹配
        if (!validateGridSize(crafterArray, recipe)) return false

        // 5. 将材料按网格布局插入合成器
        insertIngredientsIntoCrafters(recipe, inputs, crafterArray)

        busy = true
        return true
    }
}
```

#### 配方歧义解决

处理样板包含**产物信息**（`IPatternDetails.getPrimaryOutput()`），因此：

1. 玩家在 AE2 终端中编码处理样板时，输入原料 + 输出产物
2. 黄铜样板供应器收到合成请求时，通过 `pattern.getPrimaryOutput()` 获取目标产物 ItemStack
3. 遍历所有 `create:mechanical_crafting` 配方时，用 `recipe.getResultItem()` 与目标产物比较
4. 即使多个配方原料相同（如铁活板门和铁靴子都是 4 个铁），产物不同，可以唯一确定配方

#### 合成完成检测

两种可行策略：

**策略 A — 轮询检测**（推荐，简单可靠）：
- `pushPattern` 后标记 `busy = true`
- 每 tick 检查相邻合成器阵列状态
- 当所有合成器变为 IDLE 且结果出现在输出位置时，标记完成
- 将产物返回 ME 网络（通过 `IStorageService` 插入）

**策略 B — 事件监听**：
- 监听合成器的状态变化事件
- 更响应式但依赖 Create 内部 API 稳定性

### 新增依赖

```properties
# gradle.properties
ae2_version=15.4.10
```

```kotlin
// build.gradle.kts
compileOnly(fg.deobf("appeng:appliedenergistics2-forge:${project.extra["ae2_version"]}"))
```

```toml
# mods.toml 新增
[[dependencies.appliedcreate]]
    modId="ae2"
    mandatory=true
    versionRange="[15.0,)"
    ordering="AFTER"
    side="BOTH"
```

### 资源文件

- `blockstates/brass_pattern_provider.json` — 4 方向变体
- `models/block/brass_pattern_provider.json` — cube_bottom_top（黄铜机壳风格）
- `models/item/brass_pattern_provider.json` — 引用方块模型
- `textures/block/brass_pattern_provider_{top,side,bottom}.png` — 使用齿轮盛宴纹理或黄铜风格
- `textures/gui/brass_pattern_provider.png` — 9 个样板槽 + 状态指示
- `lang/en_us.json` — "Brass Pattern Provider"
- `lang/zh_cn.json` — "黄铜样板供应器"
- `recipes/brass_pattern_provider.json` — 合成配方

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
3. 安装 Create 6.0.6+
4. 安装 Applied Energistics 2 (15.x)
5. 将 `appliedcreate-1.0.0.jar` 放入 `mods/` 文件夹
