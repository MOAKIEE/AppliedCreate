---
navigation:
  title: Mechanical Craft Encoder
  icon: appliedcreate:mechanical_craft_encoder
  parent: appliedcreate:ae2guide/index.md
  position: 10
item_ids:
  - appliedcreate:mechanical_craft_encoder
---

# Mechanical Craft Encoder

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:mechanical_craft_encoder" scale="4" />

The Mechanical Craft Encoder is a utility block that helps you look up Create mechanical crafting recipes and understand the ingredient layout required for automation.

</Column>
</Row>

## How It Works

The Mechanical Craft Encoder provides a GUI where you can browse all registered Create mechanical crafting recipes. For each recipe, it displays:

- The **recipe result** — what the recipe produces
- The **ingredient grid** — the exact layout of ingredients in the correct positions, including empty slots
- The **grid dimensions** — the shape of the crafting grid required

This information is essential for setting up AE2 processing patterns that work with the <ItemLink id="appliedcreate:andesite_pattern_provider" /> or <ItemLink id="appliedcreate:brass_pattern_provider" />.

## Usage

1. **Place** the Mechanical Craft Encoder in the world
2. **Right-click** to open its GUI
3. **Browse** through available mechanical crafting recipes
4. **Note** the ingredient layout and grid shape
5. **Create** AE2 processing patterns matching the recipe inputs and outputs

## Crafting

<RecipeFor id="appliedcreate:mechanical_craft_encoder" />

## Tips

- The encoder is a reference tool — it does not consume or produce items
- Use it alongside the AE2 Pattern Terminal to create matching processing patterns
- Pay attention to the grid layout — Mechanical Crafters require ingredients in specific positions
- Empty slots in the grid are important and must be left empty in the Mechanical Crafter array
