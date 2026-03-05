package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * Mixin into Create's RotationPropagator to support virtual kinetic edges
 * through AE2 P2P tunnels without requiring physical companion blocks.
 *
 * Three injection points:
 * 1. getPotentialNeighbourLocations — add remote kinetic positions as virtual neighbors
 * 2. isConnected — recognize virtual edges as valid connections
 * 3. getRotationSpeedModifier — return 1:1 ratio for virtual edges
 */
@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    /**
     * Inject at TAIL of getPotentialNeighbourLocations to add virtual bridge targets.
     * The method returns a LinkedList<BlockPos> that already contains 6 adjacent positions
     * plus any custom positions from addPropagationLocations.
     * We add our bridged remote positions so RotationPropagator's BFS can traverse them.
     */
    @Inject(
            method = "getPotentialNeighbourLocations",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void appliedcreate$addBridgedLocations(
            KineticBlockEntity be,
            CallbackInfoReturnable<List<BlockPos>> cir
    ) {
        Set<BlockPos> remotePositions = KineticBridgeRegistry.INSTANCE.getRemotePositions(be.getBlockPos());
        if (remotePositions != null && !remotePositions.isEmpty()) {
            List<BlockPos> result = cir.getReturnValue();
            for (BlockPos remote : remotePositions) {
                if (!result.contains(remote)) {
                    result.add(remote);
                }
            }
        }
    }

    /**
     * Inject at HEAD of isConnected to recognize virtual bridge edges.
     * If KineticBridgeRegistry says these two positions are bridged, return true immediately
     * without running Create's normal connection checks.
     */
    @Inject(
            method = "isConnected",
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
        }
    }

    /**
     * Inject at HEAD of getRotationSpeedModifier to return 1:1 ratio for virtual edges.
     * This ensures bridged kinetic blocks transmit rotation at the same speed.
     */
    @Inject(
            method = "getRotationSpeedModifier",
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
        }
    }
}
