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
 * It reads speed from the adjacent Stress P2P Tunnel (output side) and generates
 * rotation at that speed.
 *
 * Stress capacity is provided via BlockStressValues.CAPACITIES registration (2048 SU base).
 * The base KineticBlockEntity.calculateAddedStressCapacity() reads this registered value
 * automatically — no override needed.
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

    override fun getGeneratedSpeed(): Float {
        return providedSpeed
    }

    // calculateAddedStressCapacity() is NOT overridden — the base class reads
    // from BlockStressValues.getCapacity(block) which returns our registered 2048.0

    /**
     * Called on first tick by SmartBlockEntity.tick() -> initialize().
     * Mirrors CreativeMotorBlockEntity.initialize() pattern:
     * ensures rotation propagation happens on world load / block placement.
     */
    override fun initialize() {
        super.initialize()
        // Try to sync from tunnel immediately on initialization
        if (level != null && !level!!.isClientSide) {
            val tunnel = findAdjacentTunnel()
            if (tunnel != null) {
                val newSpeed = tunnel.getTransferSpeed()
                if (newSpeed != 0f && newSpeed != providedSpeed) {
                    providedSpeed = newSpeed
                }
            }
            // Following Creative Motor pattern: if not sourced or generating faster
            // than theoretical, trigger rotation propagation
            if (!hasSource() || getGeneratedSpeed() > theoreticalSpeed) {
                updateGeneratedRotation()
            }
        }
    }

    /**
     * Called by StressP2PTunnelPart when new values arrive from the input side.
     */
    fun updateFromTunnel() {
        val tunnel = findAdjacentTunnel() ?: return

        val newSpeed = tunnel.getTransferSpeed()

        if (newSpeed != providedSpeed) {
            providedSpeed = newSpeed
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
        super.write(compound, registries, clientPacket)
    }

    override fun read(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        providedSpeed = compound.getFloat("ProvidedSpeed")
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
