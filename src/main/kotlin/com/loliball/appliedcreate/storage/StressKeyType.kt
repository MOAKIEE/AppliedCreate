package com.loliball.appliedcreate.storage

import appeng.api.stacks.AEKey
import appeng.api.stacks.AEKeyType
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.config.ACConfig
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

    override fun getAmountPerOperation(): Int = ACConfig.SERVER.stressAmountPerOperation.get()

    override fun getAmountPerByte(): Int = ACConfig.SERVER.stressAmountPerByte.get()

    override fun getUnitSymbol(): String = "SU"
}
