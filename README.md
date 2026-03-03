# Applied Create

A bridge between [Create](https://modrinth.com/mod/create) and [Applied Energistics 2](https://modrinth.com/mod/ae2) for Minecraft 1.20.1 (Forge).

Store rotational stress in ME networks, transmit it through P2P tunnels, and automate mechanical crafting with pattern providers.

[中文说明](README_CN.md)

---

## Features

### Andesite Pattern Provider / Brass Pattern Provider

Pattern providers designed for Create's Mechanical Crafters. Place one adjacent to a connected Mechanical Crafter array, insert AE2 processing patterns, and the provider will automatically push ingredients into the correct crafter grid positions based on the recipe layout.

- **Andesite Pattern Provider** — 9 pattern slots (standard)
- **Brass Pattern Provider** — 36 pattern slots (extended), upgradeable from Andesite variant
- Available as both full blocks and cable-bus parts

### Stress P2P Tunnel

Transmit Create's rotational stress (speed + torque) through AE2 P2P tunnels. Consists of two components:

- **Stress P2P Tunnel** — an AE2 P2P tunnel part that attaches to cables
- **Stress P2P Companion** — a full block that auto-places behind the tunnel to interface with Create's kinetic network

Supports attunement via Create's mechanical items (cogwheels, shafts, etc.).

### ME Gearbox

A bidirectional converter between ME network energy storage and Create's rotational stress.

- **Export Mode** (ME → Kinetic): Reads stored stress from the ME network and outputs it as rotation
- **Import Mode** (Kinetic → ME): Accepts rotational stress from the kinetic network and stores it in ME storage

Toggle mode by right-clicking with an empty hand. Scroll to adjust the stress multiplier. Supports Jade tooltip display for mode and transfer rate.

### Kinetic Energy Acceptor

Accepts Create's rotational stress and converts it into AE2 grid power (AE). Connect it to a kinetic network to power your ME system mechanically.

### ME Blueprint Cannon

An AE2-connected device that reads Create schematic blueprints and fires required building materials from the ME network. Features an AE2-style GUI with material checklist, speed card upgrades, and crafting card support.

### Stress Storage

A full tier system for storing rotational stress values in ME networks, mirroring AE2's item/fluid cell architecture:

| Tier | Component | Cell |
|------|-----------|------|
| 1k–256k | Andesite-style assemblies | Andesite housing cells |
| 1M–256M | Brass-style assemblies | Brass housing cells |

Includes custom processors (Andesite/Brass Stress Processor) and circuit boards craftable via AE2 inscriber or circuit cutter.

---

## Dependencies

| Mod | Version | Required |
|-----|---------|----------|
| [Minecraft Forge](https://files.minecraftforge.net/) | 47.x (1.20.1) | Yes |
| [Create](https://modrinth.com/mod/create) | 6.0+ | Yes |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 15.0+ | Yes |
| [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) | 4.0+ | Yes |

## Installation

1. Install Minecraft Forge for 1.20.1
2. Install all required dependencies listed above
3. Place `appliedcreate-x.x.x.jar` into your `mods/` folder
4. Launch the game

## License

[MIT](LICENSE)
