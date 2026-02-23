---
navigation:
  title: Andesite Pattern Provider
  icon: appliedcreate:andesite_pattern_provider
  parent: appliedcreate:ae2guide/index.md
  position: 20
item_ids:
  - appliedcreate:andesite_pattern_provider
  - appliedcreate:andesite_pattern_provider_part
---

# Andesite Pattern Provider

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_pattern_provider" scale="4" />

The Andesite Pattern Provider is a specialized AE2 pattern provider designed specifically for Create Mechanical Crafters. It can hold up to **9 processing patterns** and automatically distributes ingredients to adjacent Mechanical Crafters in the correct grid positions.

</Column>
</Row>

## Key Features

- **9 Pattern Slots** — Store up to 9 AE2 processing patterns
- **Smart Distribution** — Automatically places ingredients into the correct Mechanical Crafter slots based on the recipe grid layout
- **AE2 Integration** — Functions as a full AE2 network device, requesting materials from your ME network
- **Cable Subpart** — Available as a cable-attached part for compact builds
- **Upgradeable** — Can be upgraded to a <ItemLink id="appliedcreate:brass_pattern_provider" /> using the <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" />

## Setup

### Block Form

1. **Build** your Mechanical Crafter array and connect them with a wrench
2. **Place** the Andesite Pattern Provider adjacent to the Mechanical Crafter array
3. **Connect** the Andesite Pattern Provider to your ME network with AE2 cables
4. **Insert** processing patterns into the provider's GUI

### Cable Subpart Form

<ItemImage id="appliedcreate:andesite_pattern_provider_part" scale="2" />

The Andesite Pattern Provider is also available as a cable subpart. Place it on an AE2 cable face adjacent to your Mechanical Crafter array for more compact setups.

## Processing Patterns

The Andesite Pattern Provider works with **AE2 processing patterns**. When creating patterns:

1. Open the AE2 Pattern Terminal
2. Switch to **Processing** mode
3. Set the **inputs** to match the mechanical crafting recipe ingredients
4. Set the **output** to the recipe result
5. Use the <ItemLink id="appliedcreate:mechanical_craft_encoder" /> as a reference for correct ingredient layouts

When the ME system receives a crafting request, the provider will:
1. Request all required materials from the ME network
2. Identify the matching mechanical crafting recipe based on the pattern's output
3. Distribute ingredients to the correct Mechanical Crafter slots
4. The Mechanical Crafters complete the recipe automatically

## Crafting

<RecipeFor id="appliedcreate:andesite_pattern_provider" />

## Upgrading

Use a <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> on the Andesite Pattern Provider to upgrade it to a <ItemLink id="appliedcreate:brass_pattern_provider" /> with 36 pattern slots. All existing patterns are preserved during the upgrade.
