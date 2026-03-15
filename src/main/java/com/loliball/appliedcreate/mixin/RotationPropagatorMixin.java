package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    @Unique
    private static final Logger appliedcreate$LOGGER = LogManager.getLogger("appliedcreate/Mixin");

    @Inject(
            method = "getPotentialNeighbourLocations(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Ljava/util/List;",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void appliedcreate$addBridgedLocations(
            KineticBlockEntity be,
            CallbackInfoReturnable<List<BlockPos>> cir
    ) {
        BlockPos pos = be.getBlockPos();
        Set<BlockPos> remotePositions = KineticBridgeRegistry.INSTANCE.getRemotePositions(pos);
        if (remotePositions != null && !remotePositions.isEmpty()) {
            List<BlockPos> result = new ArrayList<>(cir.getReturnValue());
            for (BlockPos remote : remotePositions) {
                if (!result.contains(remote)) {
                    result.add(remote);
                }
            }
            appliedcreate$LOGGER.info("[Mixin] getPotentialNeighbourLocations: be={}, added {} bridged positions: {}, total={}",
                    pos, remotePositions.size(), remotePositions, result.size());
            cir.setReturnValue(result);
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
        boolean bridged = KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos());
        if (bridged) {
            appliedcreate$LOGGER.info("[Mixin] isConnected: from={}, to={} -> BRIDGED (returning true)",
                    from.getBlockPos(), to.getBlockPos());
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
        boolean bridged = KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos());
        if (bridged) {
            appliedcreate$LOGGER.info("[Mixin] getRotationSpeedModifier: from={}, to={} -> BRIDGED (returning 1.0f), fromSpeed={}, toSpeed={}",
                    from.getBlockPos(), to.getBlockPos(), from.getTheoreticalSpeed(), to.getTheoreticalSpeed());
            cir.setReturnValue(1.0f);
        }
    }
}
