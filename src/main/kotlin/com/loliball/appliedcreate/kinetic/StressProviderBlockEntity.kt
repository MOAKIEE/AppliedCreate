package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Stress Provider Block Entity — the output companion for Stress P2P.
 *
 * This GeneratingKineticBlockEntity generates rotation into the Create kinetic network.
 * It reads its speed and stress capacity from the adjacent Stress P2P Tunnel (output side).
 *
 * When the tunnel notifies us of updated values, we call updateGeneratedRotation()
 * to propagate the changes through the kinetic network.
 */
class StressProviderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : GeneratingKineticBlockEntity(type, pos, state) {

    private var providedSpeed: Float = 0f
    private var providedCapacity: Float = 0f

    override fun getGeneratedSpeed(): Float {
        return providedSpeed
    }

    override fun calculateAddedStressCapacity(): Float {
        this.lastCapacityProvided = providedCapacity
        return providedCapacity
    }

    /**
     * Called by StressP2PTunnelPart when new values arrive from the input side.
     */
    fun updateFromTunnel() {
        val tunnel = findAdjacentTunnel() ?: return

        val newSpeed = tunnel.getTransferSpeed()
        val newCapacity = tunnel.getTransferStressCapacity()

        if (newSpeed != providedSpeed || newCapacity != providedCapacity) {
            providedSpeed = newSpeed
            providedCapacity = newCapacity
            updateGeneratedRotation()
        }
    }

    /**
     * Find the adjacent Stress P2P tunnel output part.
     */
    private fun findAdjacentTunnel(): StressP2PTunnelPart? {
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val tunnelPos = worldPosition.relative(facing)
        val level = this.level ?: return null

        val be = level.getBlockEntity(tunnelPos) ?: return null
        val cableBus = be as? appeng.api.parts.IPartHost ?: return null
        val part = cableBus.getPart(facing.opposite)
        if (part is StressP2PTunnelPart && part.isOutput) {
            return part
        }
        return null
    }

    override fun write(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        compound.putFloat("ProvidedSpeed", providedSpeed)
        compound.putFloat("ProvidedCapacity", providedCapacity)
        super.write(compound, registries, clientPacket)
    }

    override fun read(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        providedSpeed = compound.getFloat("ProvidedSpeed")
        providedCapacity = compound.getFloat("ProvidedCapacity")
        super.read(compound, registries, clientPacket)
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        // Periodically re-sync from tunnel in case of chunk loads, etc.
        if (level!!.gameTime % 20 == 0L) {
            updateFromTunnel()
        }
    }
}
