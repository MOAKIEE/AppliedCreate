---
navigation:
  title: Brass Pattern Provider
  icon: appliedcreate:brass_pattern_provider
  parent: appliedcreate:ae2guide/index.md
  position: 30
item_ids:
  - appliedcreate:brass_pattern_provider
  - appliedcreate:brass_pattern_provider_part
  - appliedcreate:brass_pattern_provider_upgrade
---

# Brass Pattern Provider

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:brass_pattern_provider" scale="4" />

The Brass Pattern Provider is the upgraded version of the <ItemLink id="appliedcreate:andesite_pattern_provider" />. It functions identically but can hold up to **36 processing patterns**, making it ideal for complex automation setups with many different mechanical crafting recipes.

</Column>
</Row>

## Key Features

- **36 Pattern Slots** — Four times the capacity of the Andesite variant
- **Smart Distribution** — Same intelligent ingredient distribution as the Andesite version
- **AE2 Integration** — Full ME network device with material requesting
- **Cable Subpart** — Available as a cable-attached part
- **In-Place Upgrade** — Upgrade from Andesite variant preserving all patterns

## Comparison

| Feature | Andesite | Brass |
|---------|----------|-------|
| Pattern Slots | 9 | 36 |
| Cable Part | ✓ | ✓ |
| ME Network | ✓ | ✓ |
| Smart Distribution | ✓ | ✓ |

## Setup

Setup is identical to the <ItemLink id="appliedcreate:andesite_pattern_provider" />:

1. **Build** your Mechanical Crafter array and connect them with a wrench
2. **Place** the Brass Pattern Provider adjacent to the Mechanical Crafter array
3. **Connect** to your ME network with AE2 cables
4. **Insert** up to 36 processing patterns

### Cable Subpart Form

<ItemImage id="appliedcreate:brass_pattern_provider_part" scale="2" />

Also available as a cable subpart for compact designs.

## Obtaining

### Direct Crafting

<RecipeFor id="appliedcreate:brass_pattern_provider" />

### Upgrading from Andesite

Use a <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> on an existing <ItemLink id="appliedcreate:andesite_pattern_provider" /> to upgrade it in-place. All patterns stored in the Andesite variant will be preserved.

<RecipeFor id="appliedcreate:brass_pattern_provider_upgrade" />

## Tips

- Use the Brass Pattern Provider when you have many different mechanical crafting recipes to automate
- The cable subpart form is great for dense ME network builds
- Refer to JEI/REI for correct ingredient layouts when creating processing patterns
- Multiple providers can be placed adjacent to the same Mechanical Crafter array for even more pattern capacity
