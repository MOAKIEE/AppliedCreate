package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class ConfigureMECannonPacket(val option: Option, val set: Boolean) {

    enum class Option {
        DONT_REPLACE, REPLACE_SOLID, REPLACE_ANY, REPLACE_EMPTY, SKIP_MISSING, SKIP_BLOCK_ENTITIES, PLAY, PAUSE, STOP
    }

    companion object {
        val ID = ResourceLocation(AppliedCreate.MOD_ID, "configure_me_cannon")

        fun encode(msg: ConfigureMECannonPacket, buf: FriendlyByteBuf) {
            buf.writeByte(msg.option.ordinal)
            buf.writeBoolean(msg.set)
        }

        fun decode(buf: FriendlyByteBuf): ConfigureMECannonPacket {
            val option = Option.entries[buf.readByte().toInt()]
            val set = buf.readBoolean()
            return ConfigureMECannonPacket(option, set)
        }

        fun handle(msg: ConfigureMECannonPacket, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                val player = ctx.get().sender ?: return@enqueueWork
                if (player.containerMenu is MEBlueprintCannonMenu) {
                    val be = (player.containerMenu as MEBlueprintCannonMenu).contentHolder
                    when (msg.option) {
                        Option.DONT_REPLACE, Option.REPLACE_ANY, Option.REPLACE_EMPTY, Option.REPLACE_SOLID ->
                            be.replaceMode = msg.option.ordinal
                        Option.SKIP_MISSING ->
                            be.skipMissing = msg.set
                        Option.SKIP_BLOCK_ENTITIES ->
                            be.replaceBlockEntities = msg.set
                        Option.PLAY -> {
                            be.state = SchematicannonBlockEntity.State.RUNNING
                            be.statusMsg = "running"
                        }
                        Option.PAUSE -> {
                            be.state = SchematicannonBlockEntity.State.PAUSED
                            be.statusMsg = "paused"
                        }
                        Option.STOP -> {
                            be.state = SchematicannonBlockEntity.State.STOPPED
                            be.statusMsg = "stopped"
                        }
                    }
                    be.sendUpdate = true
                }
            }
            ctx.get().packetHandled = true
        }
    }
}
