---
navigation:
  title: Applied Create
  icon: appliedcreate:andesite_pattern_provider
  position: 0
---

# Applied Create

Applied Create bridges [Create](https://www.curseforge.com/minecraft/mc-mods/create) mechanical crafting with [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) automation.

It introduces specialized pattern providers that understand Create's Mechanical Crafter grid layout, automatically distributing ingredients to the correct slots — enabling fully automated mechanical crafting driven by your ME network.

## Items & Blocks

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:andesite_pattern_provider" scale="4" />

### <ItemLink id="appliedcreate:andesite_pattern_provider" />

Compact pattern provider with 9 processing pattern slots. A great starting point for automating mechanical crafting recipes.

</Column>
<Column>

<ItemImage id="appliedcreate:brass_pattern_provider" scale="4" />

### <ItemLink id="appliedcreate:brass_pattern_provider" />

Upgraded pattern provider with 36 processing pattern slots. Ideal for complex automation setups with many different recipes.

</Column>
</Row>

## How It Works

1. Create AE2 **processing patterns** whose inputs match the mechanical crafting recipe ingredients and whose output matches the recipe result
2. Place a pattern provider **adjacent** to your Mechanical Crafter array
3. Connect the provider to your ME network and insert the processing patterns
4. When a crafting request arrives, the provider automatically identifies the matching recipe and distributes ingredients into the correct Mechanical Crafter slots
5. The Mechanical Crafters then complete the recipe — the finished item can be returned to the ME network via any method (e.g. funnel, chute, or import bus)

## Upgrade Path

Use the <ItemLink id="appliedcreate:brass_pattern_provider_upgrade" /> on an Andesite Pattern Provider to upgrade it to a Brass Pattern Provider in-place, preserving all stored patterns.
