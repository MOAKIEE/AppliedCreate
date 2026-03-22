package com.loliball.appliedcreate.storage

import appeng.api.stacks.AEKey
import appeng.api.stacks.AEKeyType
import com.loliball.appliedcreate.AppliedCreate
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class StressKeyType private constructor() : AEKeyType(
    ID,
    StressKey::class.java,
    Component.translatable("key_type.${AppliedCreate.MOD_ID}.stress")
) {
    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "stress")

        @JvmStatic
        val TYPE = StressKeyType()
    }

    override fun codec(): MapCodec<out AEKey> = StressKey.MAP_CODEC

    override fun readFromPacket(input: RegistryFriendlyByteBuf): AEKey {
        return StressKey.INSTANCE
    }

    override fun getAmountPerOperation(): Int = 1024 * 16

    override fun getAmountPerByte(): Int = 1024 * 1024

    override fun getUnitSymbol(): String = "SU"
}
