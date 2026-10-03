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

## Usage

- **Export (ME → Kinetic)**: configure only output RPM. A negative value reverses rotation; the speed limit follows Create's server configuration.
- **Import (Kinetic → ME)**: select the mode to collect automatically. No speed or multiplier setting is required.
- Both modes require a powered ME network and an available channel. Right-click to switch modes in the GUI; Engineer's Goggles show allocated stress and actual transfer rate.

## Network-wide allocation

All ME Gearboxes on the same kinetic network share one allocation, accounting for each machine's actual RPM:

- Native generators supply ordinary machines first. Importers collect only the remaining capacity.
- Exporters withdraw the remaining deficit from ME storage before providing capacity.
- Multiple gearboxes share the demand or surplus. A shared ME inventory cannot be counted twice. Each gearbox remains subject to the server transfer limit.
- Machine additions, speed changes, merges and splits trigger allocation before Create publishes the overload result.
- ME exports are excluded from harvestable native generation, so connecting an exporter to an importer cannot create stored stress.

## Idle rotation and shortages

An unloaded network can rotate with zero SU capacity and consume no stored stress. ME power and a channel are still required. With a load attached, insufficient storage to pay the deficit causes an overload; refilling storage restores operation automatically.

Providing 1 SU for one tick consumes one stored unit. Repeated updates in the same tick charge only increases beyond each gearbox's already paid allowance. Removing load does not refund capacity already used. Reservations from an unsuccessful joint payment are returned to storage; rejected refunds remain saved with the block.

## Compatibility

Export capacity fills only the exact deficit. Fractional demand is charged in whole stored units, without exposing the rounding difference as extra capacity. Collection cannot exceed its registered stress load; settlement updates that load to the amount actually stored. Uncollected native surplus, such as when storage is full, never becomes ME inventory.

Existing saves and memory cards retain mode and output RPM; their old multiplier is ignored. Only loaded native generators that still belong to the kinetic network count as available capacity, preventing collection from stale unloaded summaries. Rotation direction, gear ratios and conflicts between generator speeds continue to follow Create's rules.
