package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

data class ConfigureMECannonPayload(val option: Option, val set: Boolean) : CustomPacketPayload {

    enum class Option {
        DONT_REPLACE, REPLACE_SOLID, REPLACE_ANY, REPLACE_EMPTY, SKIP_MISSING, SKIP_BLOCK_ENTITIES, PLAY, PAUSE, STOP
    }

    override fun type(): CustomPacketPayload.Type<ConfigureMECannonPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ConfigureMECannonPayload>(ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "configure_me_cannon"))

        val STREAM_CODEC: StreamCodec<ByteBuf, ConfigureMECannonPayload> = StreamCodec.composite(
            StreamCodec.of({ buf: ByteBuf, value: Option -> buf.writeByte(value.ordinal) }, { buf: ByteBuf -> Option.entries[buf.readByte().toInt()] }) as StreamCodec<ByteBuf, Option>, { it.option },
            net.minecraft.network.codec.ByteBufCodecs.BOOL, { it.set },
            ::ConfigureMECannonPayload
        )
    }

    fun handle(context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player()
            if (player.containerMenu is MEBlueprintCannonMenu) {
                val be = (player.containerMenu as MEBlueprintCannonMenu).contentHolder
                when (option) {
                    Option.DONT_REPLACE, Option.REPLACE_ANY, Option.REPLACE_EMPTY, Option.REPLACE_SOLID ->
                        be.replaceMode = option.ordinal
                    Option.SKIP_MISSING ->
                        be.skipMissing = set
                    Option.SKIP_BLOCK_ENTITIES ->
                        be.replaceBlockEntities = set
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
    }
}
