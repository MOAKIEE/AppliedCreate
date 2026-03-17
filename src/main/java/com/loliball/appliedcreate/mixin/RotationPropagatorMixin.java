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

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    /**
     * Append virtual bridge endpoints to the neighbour list in-place.
     * We avoid setReturnValue() so other mods' @Inject(RETURN) handlers
     * (e.g. Create Connected) can still run without the callback chain being cancelled.
     */
    @Inject(
            method = "getPotentialNeighbourLocations(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Ljava/util/List;",
            at = @At("RETURN")
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
        }
    }

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
        }
    }
}
