---
navigation:
  title: 黄铜样板供应器
  icon: appliedcreate:brass_pattern_provider
  parent: appliedcreate:ae2guide/index.md
  position: 30
item_ids:
  - appliedcreate:brass_pattern_provider
  - appliedcreate:brass_pattern_provider_part
  - appliedcreate:brass_pattern_provider_upgrade
---

# 黄铜样板供应器

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:brass_pattern_provider" scale="4" />

黄铜样板供应器是 <ItemLink id="appliedcreate:andesite_pattern_provider" /> 的升级版本。功能完全相同，但可以存放最多**36个处理样板**，非常适合需要自动化大量不同动力合成配方的场景。

</Column>
</Row>

## 主要特性

- **36个样板槽** — 安山版本的四倍容量
- **智能分配** — 与安山版本相同的智能材料分配功能
- **AE2集成** — 完整的ME网络设备，支持材料请求
- **线缆子部件** — 可作为线缆附属部件使用
- **原地升级** — 从安山版本升级时保留所有样板

## 对比

| 特性 | 安山版 | 黄铜版 |
|------|--------|--------|
| 样板槽数 | 9 | 36 |
| 线缆部件 | ✓ | ✓ |
| ME网络 | ✓ | ✓ |
| 智能分配 | ✓ | ✓ |

## 安装设置

安装方式与 <ItemLink id="appliedcreate:andesite_pattern_provider" /> 完全相同：

1. **搭建**动力合成器阵列并用扳手连接
2. **放置**黄铜样板供应器在动力合成器阵列旁边
3. **连接**到ME网络（使用AE2线缆）
4. **放入**最多36个处理样板

### 线缆子部件形态

<ItemImage id="appliedcreate:brass_pattern_provider_part" scale="2" />

同样可作为线缆子部件使用，适合紧凑型设计。

## 获取方式

### 直接合成

<RecipeFor id="appliedcreate:brass_pattern_provider" />

### 从安山版升级

对已有的 <ItemLink id="appliedcreate:andesite_pattern_provider" /> 使用 <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> 即可原地升级。安山版中已存放的所有样板将被保留。

<RecipeFor id="appliedcreate:brass_pattern_provider_upgrade" />

## 提示

- 当需要自动化大量不同的动力合成配方时，使用黄铜样板供应器
- 线缆子部件形态非常适合高密度ME网络布局
- 参考JEI/REI查阅配方布局，创建正确的处理样板
- 可以在同一个动力合成器阵列旁放置多个供应器，获得更多样板容量
