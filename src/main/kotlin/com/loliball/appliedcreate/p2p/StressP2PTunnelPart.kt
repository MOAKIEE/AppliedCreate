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
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import org.apache.logging.log4j.LogManager

class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    private var registeredInputPos: BlockPos? = null
    private var registeredKineticPos: BlockPos? = null
    private var retryAttemptsRemaining = 0
    private var initialLoadComplete = false
    private var didInitialReconcile = false

    companion object {
        private val LOGGER = LogManager.getLogger("appliedcreate/StressP2P")
        private const val MAX_RETRY_ATTEMPTS = 5

        private val MODELS = P2PModels(
            ResourceLocation(AppliedCreate.MOD_ID, "part/p2p/p2p_tunnel_stress")
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

    // ── Lifecycle ──

    override fun addToWorld() {
        super.addToWorld()
        LOGGER.info("[StressP2P] addToWorld: pos={}, side={}, isOutput={}", blockEntity.blockPos, side, isOutput)
        initialLoadComplete = false
        didInitialReconcile = false
    }

    override fun removeFromWorld() {
        LOGGER.info("[StressP2P] removeFromWorld: pos={}, isOutput={}", blockEntity.blockPos, isOutput)
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        LOGGER.info("[StressP2P] onMainNodeStateChanged: pos={}, reason={}, isActive={}, isOutput={}, initialLoadComplete={}",
            blockEntity.blockPos, reason, isActive, isOutput, initialLoadComplete)

        if (!initialLoadComplete) {
            if (isActive) {
                initialLoadComplete = true
                scheduleDeferredInitialRegistration()
            }
            return
        }

        if (isActive) {
            registerKineticBridge()
        } else {
            unregisterKineticBridge()
        }
    }

    override fun onTunnelNetworkChange() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        LOGGER.info("[StressP2P] onTunnelNetworkChange: pos={}, isOutput={}, isActive={}, freq={}, initialLoadComplete={}",
            blockEntity.blockPos, isOutput, isActive, frequency, initialLoadComplete)

        if (!initialLoadComplete) {
            return
        }

        reRegisterBridge()

        for (output in getOutputs()) {
            LOGGER.info("[StressP2P] onTunnelNetworkChange: propagating to output at pos={}",
                output.blockEntity.blockPos)
            output.reRegisterBridge()
        }
    }

    // ── Bridge Registration ──

    private fun registerKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = findInputTunnelPos()
        if (inputPos == null) {
            LOGGER.info("[StressP2P] registerKineticBridge: pos={}, isOutput={} — inputPos is null, scheduling retry",
                blockEntity.blockPos, isOutput)
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            LOGGER.info("[StressP2P] registerKineticBridge: pos={}, kineticPos={} — no KineticBlockEntity adjacent, actual BE={}, blockState={}",
                blockEntity.blockPos, kineticPos, be?.javaClass?.name ?: "null", level.getBlockState(kineticPos))
            return
        }

        if (registeredInputPos == inputPos && registeredKineticPos == kineticPos) {
            LOGGER.info("[StressP2P] registerKineticBridge: pos={} — already registered (inputPos={}, kineticPos={})",
                blockEntity.blockPos, inputPos, kineticPos)
            return
        }

        LOGGER.info("[StressP2P] registerKineticBridge: pos={}, isOutput={}, inputPos={}, kineticPos={}",
            blockEntity.blockPos, isOutput, inputPos, kineticPos)
        KineticBridgeRegistry.register(inputPos, kineticPos)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        LOGGER.info("[StressP2P] unregisterKineticBridge: pos={}, isOutput={}, inputPos={}, kineticPos={}",
            blockEntity.blockPos, isOutput, inputPos, kineticPos)

        if (KineticBridgeRegistry.serverStopping) {
            KineticBridgeRegistry.unregister(inputPos, kineticPos)
            registeredInputPos = null
            registeredKineticPos = null
            return
        }

        val remainingEndpoints = KineticBridgeRegistry.getEndpoints(inputPos).filter { it != kineticPos }
        KineticBridgeRegistry.unregister(inputPos, kineticPos)

        val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
        if (be != null && be.getTheoreticalSpeed() != 0f) {
            RotationPropagator.handleRemoved(level, kineticPos, be)
            if (be.hasSource()) {
                be.removeSource()
                be.sendData()
            }
        }

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

        LOGGER.info("[StressP2P] reRegisterBridge: pos={}, isOutput={}, old=[{}, {}], new=[{}, {}]",
            blockEntity.blockPos, isOutput, oldInputPos, oldKineticPos, newInputPos, newKineticPos)

        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) {
            return
        }

        if (oldInputPos != null && oldKineticPos != null) {
            val oldEndpoints = KineticBridgeRegistry.getEndpoints(oldInputPos).toList()
            KineticBridgeRegistry.unregister(oldInputPos, oldKineticPos)
            val oldBE = level.getBlockEntity(oldKineticPos) as? KineticBlockEntity
            if (oldBE != null && oldBE.getTheoreticalSpeed() != 0f) {
                RotationPropagator.handleRemoved(level, oldKineticPos, oldBE)
            }
            for (partnerPos in oldEndpoints) {
                if (partnerPos == oldKineticPos) continue
                val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
                RotationPropagator.handleAdded(level, partnerPos, partnerBE)
            }
            if (oldBE != null) {
                RotationPropagator.handleAdded(level, oldKineticPos, oldBE)
            }
        }

        if (newInputPos != null) {
            val be = level.getBlockEntity(newKineticPos)
            if (be is KineticBlockEntity) {
                KineticBridgeRegistry.register(newInputPos, newKineticPos)
                registeredInputPos = newInputPos
                registeredKineticPos = newKineticPos
                val newEndpoints = KineticBridgeRegistry.getEndpoints(newInputPos)
                for (endpointPos in newEndpoints) {
                    val endpointBE = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    RotationPropagator.handleAdded(level, endpointPos, endpointBE)
                }
                return
            }
            LOGGER.info("[StressP2P] reRegisterBridge: pos={}, kineticPos={} — NOT KineticBlockEntity, actual BE={}, blockState={}",
                blockEntity.blockPos, newKineticPos, be?.javaClass?.name ?: "null", level.getBlockState(newKineticPos))
        } else if (isOutput) {
            LOGGER.info("[StressP2P] reRegisterBridge: pos={} — output tunnel cannot resolve input, scheduling retry",
                blockEntity.blockPos)
            registeredInputPos = null
            registeredKineticPos = null
            scheduleRetry()
            return
        }

        registeredInputPos = null
        registeredKineticPos = null
    }

    private fun findInputTunnelPos(): BlockPos? {
        return if (!this.isOutput) {
            blockEntity.blockPos
        } else {
            this.input?.blockEntity?.blockPos
        }
    }

    // ── Deferred scheduling ──

    private fun scheduleDeferredInitialRegistration() {
        val level = blockEntity.level as? ServerLevel ?: return
        if (KineticBridgeRegistry.serverStopping) return
        val server = level.server
        val scheduledAtTick = server.tickCount
        val isInput = !this.isOutput
        scheduleForNextTick(server, scheduledAtTick) {
            if (blockEntity.isRemoved) return@scheduleForNextTick
            registerKineticBridge()
            if (isInput) {
                scheduleKineticPropagation()
            }
        }
    }

    private fun scheduleKineticPropagation() {
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        val inputPos = registeredInputPos ?: return
        val isInitialLoad = !didInitialReconcile
        val scheduledAtTick = server.tickCount
        scheduleForNextTick(server, scheduledAtTick) {
            if (blockEntity.isRemoved) return@scheduleForNextTick
            if (registeredInputPos != inputPos) return@scheduleForNextTick
            val endpoints = KineticBridgeRegistry.getEndpoints(inputPos)

            if (isInitialLoad) {
                didInitialReconcile = true
                var inputEndpointPos: BlockPos? = null
                var inputEndpointBE: KineticBlockEntity? = null
                val outputEndpointEntries = mutableListOf<Pair<BlockPos, KineticBlockEntity>>()

                for (endpointPos in endpoints) {
                    val be = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    val dx = if (endpointPos.x > inputPos.x) endpointPos.x - inputPos.x else inputPos.x - endpointPos.x
                    val dy = if (endpointPos.y > inputPos.y) endpointPos.y - inputPos.y else inputPos.y - endpointPos.y
                    val dz = if (endpointPos.z > inputPos.z) endpointPos.z - inputPos.z else inputPos.z - endpointPos.z
                    val dist = dx + dy + dz
                    if (dist == 1 && inputEndpointPos == null) {
                        inputEndpointPos = endpointPos
                        inputEndpointBE = be
                    } else {
                        outputEndpointEntries.add(endpointPos to be)
                    }
                }

                // BFS teardown of output-side kinetic blocks
                val endpointPositions = endpoints.toSet()
                val tornDown = mutableSetOf<BlockPos>()
                if (inputEndpointPos != null) tornDown.add(inputEndpointPos)

                for ((startPos, _) in outputEndpointEntries) {
                    val frontier = ArrayDeque<BlockPos>()
                    frontier.add(startPos)
                    while (frontier.isNotEmpty()) {
                        val pos = frontier.removeFirst()
                        if (!tornDown.add(pos)) continue
                        val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: continue
                        be.detachKinetics()
                        be.removeSource()
                        be.updateSpeed = false
                        for (dir in Direction.values()) {
                            val neighborPos = pos.relative(dir)
                            if (neighborPos in tornDown) continue
                            if (neighborPos in endpointPositions && neighborPos != pos) {
                                continue
                            }
                            val neighborBE = level.getBlockEntity(neighborPos) as? KineticBlockEntity
                            if (neighborBE != null) {
                                frontier.add(neighborPos)
                            }
                        }
                    }
                }

                if (inputEndpointBE != null && inputEndpointPos != null) {
                    RotationPropagator.handleAdded(level, inputEndpointPos, inputEndpointBE)

                    val processedNetworkIds = mutableSetOf<Long>()
                    for (epPos in endpoints) {
                        val epBE = level.getBlockEntity(epPos) as? KineticBlockEntity ?: continue
                        if (epBE.hasNetwork()) {
                            val network = epBE.getOrCreateNetwork()
                            if (processedNetworkIds.add(network.id)) {
                                network.initFromTE(0f, 0f, 0)
                                network.updateNetwork()
                            }
                        }
                    }
                }
            } else {
                for (endpointPos in endpoints) {
                    val be = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    RotationPropagator.handleAdded(level, endpointPos, be)
                }
            }
        }
    }

    private fun scheduleRetry() {
        if (KineticBridgeRegistry.serverStopping) return
        if (retryAttemptsRemaining <= 0) {
            retryAttemptsRemaining = MAX_RETRY_ATTEMPTS
        }
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        retryAttemptsRemaining--
        val attemptsLeft = retryAttemptsRemaining
        val scheduledAtTick = server.tickCount
        scheduleForNextTick(server, scheduledAtTick) {
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

    private fun doRetryAttempt(attemptsLeft: Int) {
        if (registeredInputPos != null) {
            return
        }
        if (blockEntity.isRemoved) return
        val inputPos = findInputTunnelPos()
        if (inputPos != null) {
            registerKineticBridge()
        } else if (attemptsLeft > 0) {
            scheduleRetry()
        }
    }

}
