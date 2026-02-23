---
navigation:
  title: 安山样板供应器
  icon: appliedcreate:andesite_pattern_provider
  parent: appliedcreate:ae2guide/index.md
  position: 20
item_ids:
  - appliedcreate:andesite_pattern_provider
  - appliedcreate:andesite_pattern_provider_part
---

# 安山样板供应器

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_pattern_provider" scale="4" />

安山样板供应器是专为机械动力的动力合成器设计的AE2样板供应器。它可以存放最多**9个处理样板**，并自动将材料分配到相邻动力合成器的正确网格位置。

</Column>
</Row>

## 主要特性

- **9个样板槽** — 可存放最多9个AE2处理样板
- **智能分配** — 根据配方网格布局自动将材料放入正确的动力合成器槽位
- **AE2集成** — 作为完整的AE2网络设备运行，从ME网络请求材料
- **线缆子部件** — 可作为线缆附属部件使用，实现紧凑布局
- **可升级** — 可使用 <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> 升级为 <ItemLink id="appliedcreate:brass_pattern_provider" />

## 安装设置

### 方块形态

1. **搭建**动力合成器阵列并用扳手连接
2. **放置**安山样板供应器在动力合成器阵列旁边
3. **连接**安山样板供应器到ME网络（使用AE2线缆）
4. **放入**处理样板到供应器的GUI中

### 线缆子部件形态

<ItemImage id="appliedcreate:andesite_pattern_provider_part" scale="2" />

安山样板供应器也可作为线缆子部件使用。将其放置在紧邻动力合成器阵列的AE2线缆面上，实现更紧凑的布局。

## 处理样板

安山样板供应器使用**AE2处理样板**工作。创建样板时：

1. 打开AE2样板终端
2. 切换到**处理**模式
3. 将**输入**设置为与动力合成配方的材料匹配
4. 将**输出**设置为配方产物
5. 使用 <ItemLink id="appliedcreate:mechanical_craft_encoder" /> 作为正确材料布局的参考

当ME系统收到合成请求时，供应器会：
1. 从ME网络请求所有需要的材料
2. 根据样板的产物匹配对应的动力合成配方
3. 将材料分配到正确的动力合成器槽位
4. 动力合成器自动完成配方合成

## 合成配方

<RecipeFor id="appliedcreate:andesite_pattern_provider" />

## 升级

对安山样板供应器使用 <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> 即可升级为拥有36个样板槽的 <ItemLink id="appliedcreate:brass_pattern_provider" />。升级过程中所有已存放的样板将被保留。
