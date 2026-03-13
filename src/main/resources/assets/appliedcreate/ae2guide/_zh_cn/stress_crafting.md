---
navigation:
  title: 应力合成材料
  icon: appliedcreate:stress_processor
  parent: appliedcreate:ae2guide/index.md
  position: 70
item_ids:
  - appliedcreate:stress_circuit_board
  - appliedcreate:advanced_stress_circuit_board
  - appliedcreate:stress_processor
  - appliedcreate:advanced_stress_processor
  - appliedcreate:stress_storage_component_1k
  - appliedcreate:stress_storage_component_4k
  - appliedcreate:stress_storage_component_16k
  - appliedcreate:stress_storage_component_64k
  - appliedcreate:stress_storage_component_256k
  - appliedcreate:stress_storage_component_1m
  - appliedcreate:stress_storage_component_4m
  - appliedcreate:stress_storage_component_16m
  - appliedcreate:stress_storage_component_64m
  - appliedcreate:stress_storage_component_256m
  - appliedcreate:andesite_stress_cell_housing
  - appliedcreate:brass_stress_cell_housing
---

# 应力合成材料

这些物品构成了制造应力存储元件的合成进阶体系。该系统参照AE2的标准存储合成链，但使用了机械动力风格的材料。

## 电路板

电路板是应力处理器的基础。可通过AE2压印器或电路切割器制作。

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_circuit_board" scale="4" />

### <ItemLink id="appliedcreate:stress_circuit_board" />

安山级应力处理的基础电路板。

<RecipeFor id="appliedcreate:stress_circuit_board" />

</Column>
<Column>

<ItemImage id="appliedcreate:advanced_stress_circuit_board" scale="4" />

### <ItemLink id="appliedcreate:advanced_stress_circuit_board" />

黄铜级应力处理的高级电路板。

<RecipeFor id="appliedcreate:advanced_stress_circuit_board" />

</Column>
</Row>

## 处理器

处理器由电路板通过AE2压印器组装而成。

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_processor" scale="4" />

### <ItemLink id="appliedcreate:stress_processor" />

用于安山级（1k–256k）存储组件配方。

<RecipeFor id="appliedcreate:stress_processor" />

</Column>
<Column>

<ItemImage id="appliedcreate:advanced_stress_processor" scale="4" />

### <ItemLink id="appliedcreate:advanced_stress_processor" />

用于黄铜级（1M–256M）存储组件配方。

<RecipeFor id="appliedcreate:advanced_stress_processor" />

</Column>
</Row>

## 存储组件

存储组件是决定元件容量的核心部件。它们遵循分级合成链——每一级基于前一级构建。

### 安山等级（1k – 256k）

<RecipeFor id="appliedcreate:stress_storage_component_1k" />

### 黄铜等级（1M – 256M）

<RecipeFor id="appliedcreate:stress_storage_component_1m" />

## 元件外壳

元件外壳与存储组件组合即可制成成品存储元件。

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_stress_cell_housing" scale="4" />

### <ItemLink id="appliedcreate:andesite_stress_cell_housing" />

用于安山级（1k–256k）存储组件。

<RecipeFor id="appliedcreate:andesite_stress_cell_housing" />

</Column>
<Column>

<ItemImage id="appliedcreate:brass_stress_cell_housing" scale="4" />

### <ItemLink id="appliedcreate:brass_stress_cell_housing" />

用于黄铜级（1M–256M）存储组件。

<RecipeFor id="appliedcreate:brass_stress_cell_housing" />

</Column>
</Row>

## 合成进阶

应力存储的完整合成链：

1. **电路板** — 通过压印器或电路切割器制作
2. **处理器** — 在压印器中组装电路板 + 硅 + 红石
3. **存储组件** — 使用处理器合成（每一级需要前一级的组件）
4. **元件外壳** — 合成对应的外壳类型
5. **存储元件** — 外壳 + 组件 = 成品元件
