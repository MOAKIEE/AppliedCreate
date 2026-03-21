package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.networking.IGridNodeListener
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.BlockGetter
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import org.slf4j.LoggerFactory

class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    private var registeredInputPos: BlockPos? = null
    private var registeredKineticPos: BlockPos? = null
    private var retryAttemptsRemaining = 0
    private var initialLoadComplete = false

    companion object {
        private val LOGGER = LoggerFactory.getLogger("AppliedCreate/StressP2P")
        private const val MAX_RETRY_ATTEMPTS = 20
        private const val RETRY_INTERVAL_TICKS = 5

        private val MODELS = P2PModels(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "part/p2p/p2p_tunnel_stress")
        )

        @JvmStatic
        @PartModels
        fun getModels(): List<IPartModel> {
            return MODELS.models
        }
    }

    override fun getStaticModels(): IPartModel {
        return MODELS.getModel(this.isPowered, this.isActive)
    }

    override fun addToWorld() {
        super.addToWorld()
        initialLoadComplete = false
    }

    override fun removeFromWorld() {
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        if (!initialLoadComplete) {
            if (isActive) {
                initialLoadComplete = true
                scheduleDeferredInitialRegistration()
            }
            return
        }

        if (isActive) {
            retryAttemptsRemaining = MAX_RETRY_ATTEMPTS
            reRegisterBridge()
        } else {
            unregisterKineticBridge()
        }
    }

    override fun onTunnelNetworkChange() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        if (!initialLoadComplete) {
            return
        }

        retryAttemptsRemaining = MAX_RETRY_ATTEMPTS
        reRegisterBridge()

        for (output in getOutputs()) {
            output.reRegisterBridge()
        }
    }

    override fun onNeighborChanged(level: BlockGetter, pos: BlockPos, neighbor: BlockPos) {
        super.onNeighborChanged(level, pos, neighbor)
        if (level is ServerLevel && initialLoadComplete && !KineticBridgeRegistry.serverStopping) {
            val kineticPos = blockEntity.blockPos.relative(this.side)
            if (neighbor == kineticPos) {
                retryAttemptsRemaining = MAX_RETRY_ATTEMPTS
                reRegisterBridge()
            }
        }
    }

    private fun registerKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = findInputTunnelPos()
        if (inputPos == null) {
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            scheduleRetry()
            return
        }

        if (registeredInputPos == inputPos && registeredKineticPos == kineticPos) {
            return
        }

        KineticBridgeRegistry.register(inputPos, kineticPos)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0
        
        // Notify Create that a new connection exists
        RotationPropagator.handleAdded(level, kineticPos, be)
        val endpoints = KineticBridgeRegistry.getEndpoints(inputPos)
        for (ep in endpoints) {
            val epBE = level.getBlockEntity(ep) as? KineticBlockEntity ?: continue
            RotationPropagator.handleAdded(level, ep, epBE)
        }
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        if (KineticBridgeRegistry.serverStopping) {
            KineticBridgeRegistry.unregister(inputPos, kineticPos)
            registeredInputPos = null
            registeredKineticPos = null
            return
        }

        // CRITICAL FIX: Call handleRemoved BEFORE unregistering the edge from KineticBridgeRegistry.
        // RotationPropagator.handleRemoved uses getPotentialNeighbourLocations to find
        // downstream blocks and clear their sources. If we remove the virtual edge first,
        // it cannot cross the P2P connection, leaving downstream blocks as "ghost sources"
        // that never stop spinning.
        val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
        if (be != null && be.getTheoreticalSpeed() != 0f) {
            RotationPropagator.handleRemoved(level, kineticPos, be)
            if (be.hasSource()) {
                be.removeSource()
                be.sendData()
            }
        }

        KineticBridgeRegistry.unregister(inputPos, kineticPos)

        val remainingEndpoints = KineticBridgeRegistry.getEndpoints(inputPos)
        for (partnerPos in remainingEndpoints) {
            val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
            RotationPropagator.handleAdded(level, partnerPos, partnerBE)
        }

        if (be != null) {
            RotationPropagator.handleAdded(level, kineticPos, be)
        }

        registeredInputPos = null
        registeredKineticPos = null
    }

    internal fun reRegisterBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        val oldInputPos = registeredInputPos
        val oldKineticPos = registeredKineticPos
        val newInputPos = findInputTunnelPos()
        val newKineticPos = blockEntity.blockPos.relative(this.side)

        if (!isActive) {
            if (oldInputPos != null && oldKineticPos != null) {
                unregisterKineticBridge()
            }
            return
        }

        if (!isOutput && oldInputPos != null && oldKineticPos != null) {
            val hasOutputs = getOutputs().isNotEmpty()
            if (!hasOutputs) {
                val endpoints = KineticBridgeRegistry.getEndpoints(oldInputPos)
                val hasRemoteEndpoints = endpoints.any { it != oldKineticPos }
                if (hasRemoteEndpoints) {
                    unregisterKineticBridge()
                    return
                }
            }
        }

        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) {
            return
        }

        if (oldInputPos != null && oldKineticPos != null) {
            unregisterKineticBridge()
        }

        if (newInputPos != null) {
            registerKineticBridge()
        } else if (isOutput) {
            scheduleRetry()
        }
    }

    private fun findInputTunnelPos(): BlockPos? {
        return if (!this.isOutput) {
            blockEntity.blockPos
        } else {
            this.input?.blockEntity?.blockPos
        }
    }

    private fun scheduleDeferredInitialRegistration() {
        val level = blockEntity.level as? ServerLevel ?: return
        if (KineticBridgeRegistry.serverStopping) return
        val server = level.server
        val scheduledAtTick = server.tickCount
        scheduleForNextTick(server, scheduledAtTick) {
            if (blockEntity.isRemoved) return@scheduleForNextTick
            registerKineticBridge()
        }
    }

    private fun scheduleRetry() {
        if (KineticBridgeRegistry.serverStopping) return
        if (retryAttemptsRemaining <= 0) return
        
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        retryAttemptsRemaining--
        val attemptsLeft = retryAttemptsRemaining
        val delayTicks = (MAX_RETRY_ATTEMPTS - attemptsLeft) * RETRY_INTERVAL_TICKS
        
        val scheduledAtTick = server.tickCount
        val targetTick = scheduledAtTick + delayTicks
        scheduleAtTick(server, targetTick) {
            doRetryAttempt(attemptsLeft)
        }
    }

    private fun scheduleForNextTick(server: net.minecraft.server.MinecraftServer, scheduledAtTick: Int, callback: () -> Unit) {
        server.tell(TickTask(scheduledAtTick + 1) {
            if (server.tickCount <= scheduledAtTick) {
                scheduleForNextTick(server, scheduledAtTick, callback)
                return@TickTask
            }
            callback()
        })
    }

    private fun scheduleAtTick(server: net.minecraft.server.MinecraftServer, targetTick: Int, callback: () -> Unit) {
        server.tell(TickTask(targetTick) {
            if (server.tickCount < targetTick) {
                scheduleAtTick(server, targetTick, callback)
                return@TickTask
            }
            callback()
        })
    }

    private fun doRetryAttempt(attemptsLeft: Int) {
        if (registeredInputPos != null) return
        if (blockEntity.isRemoved) return
        
        val inputPos = findInputTunnelPos()
        if (inputPos != null) {
            registerKineticBridge()
        } else if (attemptsLeft > 0) {
            scheduleRetry()
        }
    }
}
