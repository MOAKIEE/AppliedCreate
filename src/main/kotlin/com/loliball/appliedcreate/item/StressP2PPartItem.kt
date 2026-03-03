package com.loliball.appliedcreate.item

import appeng.items.parts.PartItem
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart

/**
 * Part item for the Stress P2P Tunnel that extends AE2's PartItem (not plain Item).
 *
 * This is required because AE2's P2PTunnelAttunement.validateTunnelPartItem() checks
 * `item instanceof PartItem<?>` — our old MechanicalCraftingPartItem extends plain Item
 * and only implements IPartItem, which fails that check.
 *
 * By extending PartItem directly, we get full P2P attunement support (tag-based
 * auto-attunement via memory card + items like gears, shafts, etc.).
 */
class StressP2PPartItem(
    properties: Properties,
    partClass: Class<StressP2PTunnelPart>,
    factory: java.util.function.Function<appeng.api.parts.IPartItem<StressP2PTunnelPart>, StressP2PTunnelPart>
) : PartItem<StressP2PTunnelPart>(properties, partClass, factory)
