package com.loliball.appliedcreate.patternprovider

import appeng.api.parts.IPart
import appeng.api.parts.IPartItem
import appeng.api.parts.PartHelper
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

class MechanicalCraftingPartItem<T : IPart>(
    properties: Properties,
    private val partClass: Class<T>,
    private val factory: (IPartItem<T>) -> T
) : Item(properties), IPartItem<T> {

    override fun useOn(context: UseOnContext): InteractionResult {
        return PartHelper.usePartItem(context)
    }

    override fun getPartClass(): Class<T> = partClass

    override fun createPart(): T = factory(this)
}
