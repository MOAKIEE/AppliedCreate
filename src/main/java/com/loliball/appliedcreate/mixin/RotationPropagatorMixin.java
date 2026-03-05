package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mixin into Create's RotationPropagator to support virtual kinetic edges
 * through AE2 P2P tunnels without requiring physical companion blocks.
 *
 * Six injection points:
 * 1. getPotentialNeighbourLocations — add remote kinetic positions as virtual neighbors
 * 2. isConnected — recognize virtual edges as valid connections
 * 3. getRotationSpeedModifier — return 1:1 ratio for virtual edges
 * 4. findConnectedNeighbour — resolve cross-dimension block entities via registry
 * 5. handleRemoved — propagate source removal to cross-dimension bridge partners
 * 6. propagateMissingSource — redirect getBlockEntity to resolve cross-dimension positions
 */
@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    // ── 1. Virtual neighbor discovery ──

    /**
     * Inject at TAIL of getPotentialNeighbourLocations to add virtual bridge targets.
     * The method returns a LinkedList<BlockPos> that already contains 6 adjacent positions
     * plus any custom positions from addPropagationLocations.
     * We add our bridged remote positions so RotationPropagator's BFS can traverse them.
     */
    @Inject(
            method = "getPotentialNeighbourLocations(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Ljava/util/List;",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void appliedcreate$addBridgedLocations(
            KineticBlockEntity be,
            CallbackInfoReturnable<List<BlockPos>> cir
    ) {
        Set<BlockPos> remotePositions = KineticBridgeRegistry.INSTANCE.getRemotePositions(be.getBlockPos());
        if (remotePositions != null && !remotePositions.isEmpty()) {
            // Defensively copy the list in case Create returns an unmodifiable list
            List<BlockPos> result = new ArrayList<>(cir.getReturnValue());
            for (BlockPos remote : remotePositions) {
                if (!result.contains(remote)) {
                    result.add(remote);
                }
            }
            cir.setReturnValue(result);
        }
    }

    // ── 2. Virtual edge recognition ──

    /**
     * Inject at HEAD of isConnected to recognize virtual bridge edges.
     * If KineticBridgeRegistry says these two positions are bridged, return true immediately
     * without running Create's normal connection checks.
     */
    @Inject(
            method = "isConnected(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void appliedcreate$checkBridgeConnection(
            KineticBlockEntity from,
            KineticBlockEntity to,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos())) {
            cir.setReturnValue(true);
            return;
        }
    }

    // ── 3. Virtual edge speed ratio ──

    /**
     * Inject at HEAD of getRotationSpeedModifier to return 1:1 ratio for virtual edges.
     * This ensures bridged kinetic blocks transmit rotation at the same speed.
     */
    @Inject(
            method = "getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void appliedcreate$bridgeSpeedModifier(
            KineticBlockEntity from,
            KineticBlockEntity to,
            CallbackInfoReturnable<Float> cir
    ) {
        if (KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos())) {
            cir.setReturnValue(1.0f);
            return;
        }
    }

    // ── 4. Cross-dimension neighbor resolution ──

    /**
     * Inject at HEAD of findConnectedNeighbour to resolve cross-dimension block entities.
     *
     * Problem: Create's findConnectedNeighbour uses currentTE.getLevel() to look up
     * the block at neighbourPos. When a virtual bridge edge connects blocks in different
     * dimensions, the neighbourPos is in a different level, so the lookup returns the wrong
     * block (usually air → silently skipped, breaking the kinetic bridge).
     *
     * Fix: If the positions are bridged, resolve the block entity from the correct level
     * via KineticBridgeRegistry and return it directly, bypassing Create's level-local lookup.
     *
     * This fix makes propagateNewSource naturally cross-dimension safe, since it uses
     * recursive calls passing actual BE references (each recursion level resolves from
     * the correct currentTE.getLevel() locally).
     */
    @Inject(
            method = "findConnectedNeighbour(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/BlockPos;)Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void appliedcreate$crossDimFindNeighbour(
            KineticBlockEntity currentTE,
            BlockPos neighbourPos,
            CallbackInfoReturnable<KineticBlockEntity> cir
    ) {
        if (KineticBridgeRegistry.INSTANCE.areBridged(currentTE.getBlockPos(), neighbourPos)) {
            KineticBlockEntity remoteBE = KineticBridgeRegistry.INSTANCE.resolveBlockEntity(neighbourPos);
            if (remoteBE != null) {
                cir.setReturnValue(remoteBE);
                return;
            }
        }
    }

    // ── 5. Cross-dimension removal propagation ──

    /**
     * Inject at TAIL of handleRemoved to propagate source removal to cross-dimension
     * bridge partners that Create's normal loop misses.
     *
     * Problem: handleRemoved's loop (lines 317-329) uses worldIn.getBlockState/getBlockEntity
     * directly — NOT through findConnectedNeighbour. So cross-dimension bridge partners
     * are silently skipped because worldIn is the removed block's level, but the partner
     * is in a different dimension.
     *
     * Fix: After Create's normal handling, manually check all bridge partners. If a partner
     * had source == pos (the removed block's position), call propagateMissingSource on it
     * via the RotationPropagatorAccessor invoker interface.
     */
    @Inject(
            method = "handleRemoved(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At("TAIL")
    )
    private static void appliedcreate$crossDimHandleRemoved(
            Level worldIn, BlockPos pos, KineticBlockEntity removedBE,
            CallbackInfo ci
    ) {
        if (worldIn.isClientSide) return;
        if (removedBE == null) return;
        if (removedBE.getTheoreticalSpeed() == 0) return;

        Set<BlockPos> remotePositions = KineticBridgeRegistry.INSTANCE.getRemotePositions(pos);
        if (remotePositions == null || remotePositions.isEmpty()) return;

        for (BlockPos remotePos : remotePositions) {
            // Only process cross-dimension partners (same-dimension ones are already handled)
            KineticBlockEntity remoteBE = KineticBridgeRegistry.INSTANCE.resolveBlockEntity(remotePos);
            if (remoteBE == null) continue;

            // Skip if same level — Create's normal loop already handled these
            if (remoteBE.getLevel() == worldIn) continue;

            // If this remote BE was sourced from the removed block, propagate the missing source
            if (remoteBE.hasSource() && remoteBE.source.equals(pos)) {
                RotationPropagatorAccessor.appliedcreate$invokePropMissingSource(remoteBE);
            }
        }
    }

    // ── 6. Cross-dimension BFS in propagateMissingSource ──

    /**
     * Redirect world.getBlockEntity(pos) inside propagateMissingSource to resolve
     * cross-dimension positions from the correct level.
     *
     * Problem: propagateMissingSource captures world = updateTE.getLevel() once and uses
     * it for the entire BFS frontier. If a cross-dimension position enters the frontier
     * (because its source was set to a position in the other dimension), the lookup
     * resolves from the wrong level.
     *
     * Fix: If the position is a registered bridge endpoint, resolve it from the correct
     * level via KineticBridgeRegistry. Otherwise fall back to the normal level lookup.
     *
     * Note: This targets the single world.getBlockEntity(pos) call at line 349 of
     * RotationPropagator. The getConnectedNeighbours call at line 356 goes through
     * findConnectedNeighbour which is already fixed by injection point #4.
     */
    @Redirect(
            method = "propagateMissingSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private static BlockEntity appliedcreate$crossDimGetBlockEntity(Level level, BlockPos pos) {
        // Check if this position is a registered bridge endpoint in a different dimension
        KineticBlockEntity bridgedBE = KineticBridgeRegistry.INSTANCE.resolveBlockEntity(pos);
        if (bridgedBE != null) {
            return bridgedBE;
        }
        // Fallback: normal same-dimension lookup
        return level.getBlockEntity(pos);
    }
}
