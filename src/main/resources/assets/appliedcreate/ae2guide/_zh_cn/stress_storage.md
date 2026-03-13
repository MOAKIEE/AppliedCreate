---
navigation:
  title: 应力存储元件
  icon: appliedcreate:stress_storage_cell_1k
  parent: appliedcreate:index.md
  position: 60
item_ids:
  - appliedcreate:stress_storage_cell_1k
  - appliedcreate:stress_storage_cell_4k
  - appliedcreate:stress_storage_cell_16k
  - appliedcreate:stress_storage_cell_64k
  - appliedcreate:stress_storage_cell_256k
  - appliedcreate:stress_storage_cell_1m
  - appliedcreate:stress_storage_cell_4m
  - appliedcreate:stress_storage_cell_16m
  - appliedcreate:stress_storage_cell_64m
  - appliedcreate:stress_storage_cell_256m
  - appliedcreate:creative_stress_cell
---

# 应力存储元件

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_storage_cell_1k" scale="4" />

应力存储元件可以在ME网络中存储机械动力的旋转应力值，就像物品或流体存储元件存储物品和流体一样。将其插入ME驱动器，即可缓存应力以供 <ItemLink id="appliedcreate:me_gearbox" /> 后续使用。

</Column>
</Row>

## 元件等级

### 安山等级（1k – 256k）

使用 <ItemLink id="appliedcreate:andesite_stress_cell_housing" /> 和安山级存储组件制造。

| 元件 | 存储容量 | 空闲功耗 |
|------|---------|---------|
| <ItemLink id="appliedcreate:stress_storage_cell_1k" /> | 1 KB | 0.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_4k" /> | 4 KB | 1.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_16k" /> | 16 KB | 1.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_64k" /> | 64 KB | 2.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_256k" /> | 256 KB | 2.5 AE/t |

### 黄铜等级（1M – 256M）

使用 <ItemLink id="appliedcreate:brass_stress_cell_housing" /> 和黄铜级存储组件制造。

| 元件 | 存储容量 | 空闲功耗 |
|------|---------|---------|
| <ItemLink id="appliedcreate:stress_storage_cell_1m" /> | 1 MB | 3.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_4m" /> | 4 MB | 3.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_16m" /> | 16 MB | 4.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_64m" /> | 64 MB | 4.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_256m" /> | 256 MB | 5.0 AE/t |

### 创造应力元件

<ItemImage id="appliedcreate:creative_stress_cell" scale="2" />

<ItemLink id="appliedcreate:creative_stress_cell" /> 提供无限应力存储，用于创造模式测试。

## 合成配方

应力存储元件通过将存储组件与对应的元件外壳组合制造：

<RecipeFor id="appliedcreate:stress_storage_cell_1k" />

<RecipeFor id="appliedcreate:stress_storage_cell_1m" />

## 使用方法

1. 制造所需等级的应力存储元件
2. 将其插入AE2 ME驱动器
3. 使用 <ItemLink id="appliedcreate:me_gearbox" /> 的输入模式存储应力
4. 使用 <ItemLink id="appliedcreate:me_gearbox" /> 的输出模式取出应力并生成旋转

## 提示

- 应力元件存储单一类型：旋转应力（SU）。ME终端会显示存储的应力量。
- 更高等级的元件容纳更多应力，但消耗ME网络更多空闲功率
- 每个元件上的LED指示灯显示其填充状态（空、部分填充、已满）
- 应力元件可在ME终端中与常规物品和流体一同查看
