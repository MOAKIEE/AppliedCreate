---
navigation:
  title: Stress Crafting Materials
  icon: appliedcreate:stress_processor
  parent: appliedcreate:index.md
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

# Stress Crafting Materials

These items form the crafting progression for building stress storage cells. The system mirrors AE2's standard storage crafting chain but uses Create-themed materials.

## Circuit Boards

Circuit boards are the foundation of stress processors. They can be created using the AE2 Inscriber or a circuit cutter.

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_circuit_board" scale="4" />

### <ItemLink id="appliedcreate:stress_circuit_board" />

The base circuit board for Andesite-tier stress processing.

<RecipeFor id="appliedcreate:stress_circuit_board" />

</Column>
<Column>

<ItemImage id="appliedcreate:advanced_stress_circuit_board" scale="4" />

### <ItemLink id="appliedcreate:advanced_stress_circuit_board" />

The advanced circuit board for Brass-tier stress processing.

<RecipeFor id="appliedcreate:advanced_stress_circuit_board" />

</Column>
</Row>

## Processors

Processors are assembled from circuit boards using the AE2 Inscriber.

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_processor" scale="4" />

### <ItemLink id="appliedcreate:stress_processor" />

Used in Andesite-tier (1k–256k) storage assembly recipes.

<RecipeFor id="appliedcreate:stress_processor" />

</Column>
<Column>

<ItemImage id="appliedcreate:advanced_stress_processor" scale="4" />

### <ItemLink id="appliedcreate:advanced_stress_processor" />

Used in Brass-tier (1M–256M) storage assembly recipes.

<RecipeFor id="appliedcreate:advanced_stress_processor" />

</Column>
</Row>

## Storage Assemblies

Storage assemblies are the core component that determines cell capacity. They follow a tiered crafting chain — each tier builds upon the previous one.

### Andesite Tier (1k – 256k)

<RecipeFor id="appliedcreate:stress_storage_component_1k" />

### Brass Tier (1M – 256M)

<RecipeFor id="appliedcreate:stress_storage_component_1m" />

## Cell Housings

Cell housings are combined with storage assemblies to create finished storage cells.

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_stress_cell_housing" scale="4" />

### <ItemLink id="appliedcreate:andesite_stress_cell_housing" />

Used with Andesite-tier (1k–256k) storage assemblies.

<RecipeFor id="appliedcreate:andesite_stress_cell_housing" />

</Column>
<Column>

<ItemImage id="appliedcreate:brass_stress_cell_housing" scale="4" />

### <ItemLink id="appliedcreate:brass_stress_cell_housing" />

Used with Brass-tier (1M–256M) storage assemblies.

<RecipeFor id="appliedcreate:brass_stress_cell_housing" />

</Column>
</Row>

## Crafting Progression

The full crafting chain for stress storage:

1. **Circuit Boards** — Create via Inscriber or circuit cutter
2. **Processors** — Assemble circuit board + silicon + redstone in Inscriber
3. **Storage Assemblies** — Craft with processors (each tier requires the previous tier's assembly)
4. **Cell Housings** — Craft the appropriate housing type
5. **Storage Cells** — Combine housing + assembly = finished cell
