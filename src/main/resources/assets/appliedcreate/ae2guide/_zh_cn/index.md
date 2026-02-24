---
navigation:
  title: 应用机动
  icon: appliedcreate:andesite_pattern_provider
  position: 0
---

# 应用机动

应用机动将 [机械动力](https://www.curseforge.com/minecraft/mc-mods/create) 的动力合成与 [应用能源2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) 的自动化系统连接起来。

它引入了专用的样板供应器，能够理解机械动力的动力合成器网格布局，自动将材料分配到正确的槽位——实现由ME网络驱动的全自动动力合成。

## 物品与方块

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_pattern_provider" scale="4" />

### <ItemLink id="appliedcreate:andesite_pattern_provider" />

紧凑型样板供应器，支持存放9个处理样板。是自动化动力合成配方的理想起步选择。

</Column>
<Column>

<ItemImage id="appliedcreate:brass_pattern_provider" scale="4" />

### <ItemLink id="appliedcreate:brass_pattern_provider" />

升级版样板供应器，支持存放36个处理样板。适合需要自动化大量不同配方的复杂场景。

</Column>
</Row>

## 工作原理

1. 在AE2样板终端中创建**处理样板**，其输入对应动力合成配方的材料，输出对应配方产物
2. 将样板供应器放置在动力合成器阵列**旁边**
3. 将供应器连接到ME网络并放入处理样板
4. 当合成请求到达时，供应器会自动识别匹配的配方，并将材料分配到正确的动力合成器槽位
5. 动力合成器随后完成配方合成——成品可通过任意方式（如漏斗、溜槽或输入总线）返回ME网络

## 升级路径

对安山样板供应器使用 <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> 即可原地升级为黄铜样板供应器，已存放的所有样板将被保留。
