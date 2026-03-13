---
navigation:
  title: Stress P2P Tunnel
  icon: appliedcreate:stress_p2p_tunnel
  parent: appliedcreate:index.md
  position: 50
item_ids:
  - appliedcreate:stress_p2p_tunnel
---

# Stress P2P Tunnel

<Row gap="20">
<Column>

<ItemImage id="appliedcreate:stress_p2p_tunnel" scale="4" />

The Stress P2P Tunnel is an AE2 P2P tunnel part that transmits Create's rotational stress (speed and torque) through ME networks. It bridges kinetic networks without requiring physical shaft connections between them.

</Column>
</Row>

## Key Features

- **Wireless Kinetic Bridging** — Connect distant Create kinetic networks through your ME cable system
- **Full Rotation Transfer** — Transfers both speed (RPM) and stress faithfully between input and output
- **Multiple Outputs** — One input tunnel can drive multiple output tunnels, just like standard P2P tunnels
- **Standard P2P Behavior** — Uses memory cards for linking, supports AE2's P2P tunnel attunement system
- **Create Item Attunement** — Can be attuned by holding Create mechanical items (cogwheels, shafts, etc.) near a standard ME P2P Tunnel

## How It Works

1. **Place** two Stress P2P Tunnel parts on AE2 cables
2. **Link** them using a memory card (same as any AE2 P2P tunnel)
3. **Connect** a Create kinetic source (shaft, cogwheel, etc.) adjacent to the input tunnel's cable bus face
4. **Connect** a Create kinetic consumer adjacent to the output tunnel's cable bus face
5. The rotation propagates through the P2P link as if the blocks were physically adjacent

## Setup Tips

- The tunnel parts must face outward from the cable bus toward the adjacent kinetic blocks
- Ensure both tunnels have active ME network connections (channels required)
- The input tunnel is the one you first link with the memory card; outputs are the destinations
- There is no energy loss through the tunnel — speed and stress are transferred 1:1

## Obtaining

Stress P2P Tunnels can be obtained by **attuning** a standard <ItemLink id="ae2:me_p2p_tunnel" /> with a Create mechanical item (such as a shaft or cogwheel). Hold the mechanical item and right-click the P2P tunnel to attune it.
