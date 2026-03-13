---
navigation:
  title: Kinetic Energy Acceptor
  icon: appliedcreate:kinetic_energy_acceptor
  parent: appliedcreate:ae2guide/index.md
  position: 45
item_ids:
  - appliedcreate:kinetic_energy_acceptor
---

# Kinetic Energy Acceptor

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:kinetic_energy_acceptor" scale="4" />

The Kinetic Energy Acceptor converts Create's rotational kinetic energy into AE2 grid power (AE). Connect it to a kinetic network to power your ME system mechanically — no energy cells or generators required.

</Column>
</Row>

## Key Features

- **Kinetic to AE Conversion** — Converts rotational stress into AE2 grid power at a rate of 640 AE/t per 256 RPM (base multiplier)
- **Adjustable Stress Multiplier** — Scroll on the block to adjust the multiplier (1–16×), increasing both power output and stress consumption
- **Passive Power Generation** — Acts as an AE2 passive energy generator, continuously supplying power while receiving rotation
- **Goggle Tooltip** — View current AE/t output and stress impact with Engineer's Goggles
- **Side Connectivity** — AE2 cables connect to the sides perpendicular to the shaft axis

## Power Output

The conversion formula is:

**AE/t = 640 × Multiplier × (RPM / 256)**

| RPM | ×1 Multiplier | ×4 Multiplier | ×16 Multiplier |
|-----|---------------|---------------|----------------|
| 32  | 80 AE/t       | 320 AE/t      | 1,280 AE/t     |
| 64  | 160 AE/t      | 640 AE/t      | 2,560 AE/t     |
| 128 | 320 AE/t      | 1,280 AE/t    | 5,120 AE/t     |
| 256 | 640 AE/t      | 2,560 AE/t    | 10,240 AE/t    |

## Stress Impact

Base stress impact: 64 SU per RPM (at ×1 multiplier). Scales linearly with the multiplier setting.

## Setup

1. **Place** the Kinetic Energy Acceptor facing the direction of your shaft
2. **Connect** a Create kinetic source (shaft, cogwheel, etc.) to the shaft face
3. **Connect** AE2 cables to the side faces
4. **Scroll** on the block to adjust the stress multiplier if needed
5. The acceptor will immediately begin generating AE power

## Crafting

<RecipeFor id="appliedcreate:kinetic_energy_acceptor" />

## Tips

- Higher multiplier = more AE power but more stress consumed from the kinetic network
- At maximum settings (256 RPM, ×16), the acceptor generates 10,240 AE/t — enough for most ME systems
- Combine with a Create water wheel or windmill for a renewable ME power source
- The acceptor has no idle power consumption on the AE2 side
