---
navigation:
  title: Stress Storage Cells
  icon: appliedcreate:stress_storage_cell_1k
  parent: appliedcreate:ae2guide/index.md
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

# Stress Storage Cells

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_storage_cell_1k" scale="4" />

Stress Storage Cells allow you to store Create's rotational stress values in your ME network, just like item or fluid storage cells store items and fluids. Insert them into ME Drives to buffer stress for later use with the <ItemLink id="appliedcreate:me_gearbox" />.

</Column>
</Row>

## Cell Tiers

### Andesite Tier (1k – 256k)

Built with <ItemLink id="appliedcreate:andesite_stress_cell_housing" /> and Andesite-tier storage assemblies.

| Cell | Storage | Idle Drain |
|------|---------|------------|
| <ItemLink id="appliedcreate:stress_storage_cell_1k" /> | 1 KB | 0.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_4k" /> | 4 KB | 1.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_16k" /> | 16 KB | 1.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_64k" /> | 64 KB | 2.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_256k" /> | 256 KB | 2.5 AE/t |

### Brass Tier (1M – 256M)

Built with <ItemLink id="appliedcreate:brass_stress_cell_housing" /> and Brass-tier storage assemblies.

| Cell | Storage | Idle Drain |
|------|---------|------------|
| <ItemLink id="appliedcreate:stress_storage_cell_1m" /> | 1 MB | 3.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_4m" /> | 4 MB | 3.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_16m" /> | 16 MB | 4.0 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_64m" /> | 64 MB | 4.5 AE/t |
| <ItemLink id="appliedcreate:stress_storage_cell_256m" /> | 256 MB | 5.0 AE/t |

### Creative Stress Cell

<ItemImage id="appliedcreate:creative_stress_cell" scale="2" />

The <ItemLink id="appliedcreate:creative_stress_cell" /> provides infinite stress storage for creative mode testing.

## Crafting

Stress storage cells are crafted by combining a storage assembly with the appropriate cell housing:

<RecipeFor id="appliedcreate:stress_storage_cell_1k" />

<RecipeFor id="appliedcreate:stress_storage_cell_1m" />

## Usage

1. Craft a stress storage cell of the desired tier
2. Insert it into an AE2 ME Drive
3. Use the <ItemLink id="appliedcreate:me_gearbox" /> in Import mode to store stress
4. Use the <ItemLink id="appliedcreate:me_gearbox" /> in Export mode to retrieve stress and generate rotation

## Tips

- Stress cells store a single type: rotational stress (SU). The ME terminal displays stored stress amounts.
- Higher-tier cells hold more stress but consume more idle power from the ME network
- The LED indicator on each cell shows its fill status (empty, partially filled, full)
- Stress cells can be viewed in the ME terminal alongside regular items and fluids
