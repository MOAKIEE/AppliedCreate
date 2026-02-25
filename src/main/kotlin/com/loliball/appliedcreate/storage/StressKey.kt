package com.loliball.appliedcreate.storage

import appeng.api.stacks.AEKey
import appeng.api.stacks.AEKeyType
import com.loliball.appliedcreate.AppliedCreate
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class StressKey private constructor() : AEKey() {

    companion object {
        @JvmStatic
        val INSTANCE = StressKey()

        private val KEY_ID = ResourceLocation(AppliedCreate.MOD_ID, "stress")
    }

    override fun getType(): AEKeyType = StressKeyType.TYPE

    override fun dropSecondary(): AEKey = this

    override fun toTag(): CompoundTag = CompoundTag()

    override fun getPrimaryKey(): Any = KEY_ID

    override fun getId(): ResourceLocation = KEY_ID

    override fun writeToPacket(data: FriendlyByteBuf) {
        // No additional data needed — StressKey is a singleton
    }

    override fun computeDisplayName(): Component =
        Component.translatable("key.${AppliedCreate.MOD_ID}.stress")

    override fun addDrops(amount: Long, drops: MutableList<ItemStack>, level: Level, pos: BlockPos) {
        // Stress units are virtual — nothing to drop
    }

    override fun hashCode(): Int = KEY_ID.hashCode()

    override fun equals(other: Any?): Boolean = other is StressKey
}
