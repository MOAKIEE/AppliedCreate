---
navigation:
  title: ME Gearbox
  icon: appliedcreate:me_gearbox
  parent: appliedcreate:index.md
  position: 40
item_ids:
  - appliedcreate:me_gearbox
---

# ME Gearbox

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:me_gearbox" scale="4" />

The ME Gearbox is a bidirectional converter between AE2 ME network stress storage and Create's rotational kinetic networks. It can either extract stored stress from the ME network to generate rotation, or consume rotation from the kinetic network and store it as stress in the ME network.

</Column>
</Row>

## Key Features

- **Bidirectional Conversion** — Switch between Export (ME → Kinetic) and Import (Kinetic → ME) modes
- **Configurable Speed** — Set the output rotation speed from 1 to 256 RPM
- **Configurable Stress Multiplier** — Set the stress multiplier from 0 to 65536 to control capacity/impact
- **AE2-Style GUI** — Right-click to open a clean AE2-style configuration interface
- **Goggle Tooltip** — View current mode, stress capacity/impact, and transfer rate with Engineer's Goggles
- **Requires Channel** — Consumes one ME network channel

## Modes

### Export Mode (ME → Kinetic)

The gearbox extracts stress values from ME storage and generates Create rotation:

- Acts as a **stress source** in the kinetic network
- Output speed is set via the GUI (1–256 RPM)
- Stress capacity provided equals the configured stress multiplier
- Stops generating if ME storage runs out of stress

### Import Mode (Kinetic → ME)

The gearbox consumes Create rotation and inserts stress values into ME storage:

- Acts as a **stress consumer** in the kinetic network
- Stress impact equals the configured stress multiplier
- Transfer rate scales with actual RPM of the connected kinetic network
- Requires an external kinetic source to function

## Setup

1. **Place** the ME Gearbox and connect it to your ME network via AE2 cables on the sides
2. **Connect** a Create shaft or kinetic component to the front/back faces
3. **Right-click** to open the GUI and configure mode, speed, and stress multiplier
4. For Export mode, ensure your ME network has stress stored in stress storage cells
5. For Import mode, ensure a kinetic source is driving the gearbox

## Crafting

<RecipeFor id="appliedcreate:me_gearbox" />

## Tips

- The transfer rate displayed in the goggle tooltip shows how much stress (SU/t) is being moved between ME and kinetic networks
- In Export mode, the gearbox will stop outputting rotation if the ME network runs out of stored stress
- In Import mode, the gearbox provides a convenient way to buffer kinetic energy into your ME network for later use
- Stress multiplier affects both the stress capacity/impact in the kinetic network AND the transfer rate to/from ME storage
