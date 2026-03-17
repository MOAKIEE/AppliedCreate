package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    /**
     * When Create's BFS queries potential neighbour locations for a kinetic block,
     * append any virtual bridge endpoints registered through P2P tunnels.
     *
     * We mutate the returned list in-place rather than calling setReturnValue(),
     * so that subsequent @Inject(RETURN) handlers from other mods (e.g. Create Connected)
     * can still see and process our additions without the callback chain being cancelled.
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

    /**
     * Intercept isConnected to report virtual bridge edges as connected.
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
        }
    }

    /**
     * Return speed modifier of 1.0 for virtual bridge edges (1:1 ratio).
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
        }
    }
}
