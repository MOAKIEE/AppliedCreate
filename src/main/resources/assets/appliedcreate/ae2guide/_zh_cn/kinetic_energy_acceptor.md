---
navigation:
  title: 动能接收器
  icon: appliedcreate:kinetic_energy_acceptor
  parent: appliedcreate:index.md
  position: 45
item_ids:
  - appliedcreate:kinetic_energy_acceptor
---

# 动能接收器

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:kinetic_energy_acceptor" scale="4" />

动能接收器将机械动力的旋转能量转化为AE2电网能量（AE）。将其连接到动力网络即可为ME系统供电——无需能量元件或发电机。

</Column>
</Row>

## 主要特性

- **动力转AE能量** — 将旋转应力转化为AE2电网能量，基础速率为每256 RPM产生640 AE/t
- **可调应力倍率** — 对方块滚轮可调整倍率（1–16×），同时提高功率输出和应力消耗
- **被动发电** — 作为AE2被动能量发生器运行，在接收旋转时持续供电
- **护目镜信息** — 使用工程师护目镜查看当前AE/t输出和应力消耗
- **侧面连接** — AE2线缆连接到与轴垂直的侧面

## 功率输出

转换公式：

**AE/t = 640 × 倍率 × (RPM / 256)**

| RPM | ×1倍率 | ×4倍率 | ×16倍率 |
|-----|--------|--------|---------|
| 32  | 80 AE/t | 320 AE/t | 1,280 AE/t |
| 64  | 160 AE/t | 640 AE/t | 2,560 AE/t |
| 128 | 320 AE/t | 1,280 AE/t | 5,120 AE/t |
| 256 | 640 AE/t | 2,560 AE/t | 10,240 AE/t |

## 应力消耗

基础应力消耗：每RPM 64 SU（×1倍率下）。随倍率设置线性缩放。

## 安装设置

1. **放置**动能接收器，使其面向轴的方向
2. 将机械动力的动力源（轴、齿轮等）**连接**到轴面
3. 将AE2线缆**连接**到侧面
4. 如需调整，**滚轮**方块以改变应力倍率
5. 接收器将立即开始产生AE能量

## 合成配方

<RecipeFor id="appliedcreate:kinetic_energy_acceptor" />

## 提示

- 更高的倍率 = 更多AE能量，但消耗动力网络更多应力
- 在最大设置下（256 RPM、×16），接收器产生10,240 AE/t——足以满足大多数ME系统
- 搭配机械动力的水车或风车，实现可再生的ME供电方案
- 接收器在AE2侧没有空闲能量消耗
