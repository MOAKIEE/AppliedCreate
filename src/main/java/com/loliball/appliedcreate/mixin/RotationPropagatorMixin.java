package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.p2p.KineticBridgeRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    @Unique
    private static final Logger appliedcreate$LOGGER = LogManager.getLogger("appliedcreate/RotationPropagatorMixin");

    static {
        LogManager.getLogger("appliedcreate/RotationPropagatorMixin")
                .info("[DIAG] RotationPropagatorMixin static initializer executed — mixin class loaded into RotationPropagator");
    }

    @Inject(
            method = "handleAdded(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At("HEAD")
    )
    private static void appliedcreate$onHandleAdded(
            Level level,
            BlockPos pos,
            KineticBlockEntity addedTE,
            CallbackInfo ci
    ) {
        appliedcreate$LOGGER.info("[DIAG] handleAdded fired: pos={}, be={}", pos, addedTE.getClass().getSimpleName());
    }

    @Inject(
            method = "getPotentialNeighbourLocations(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void appliedcreate$addBridgedLocations(
            KineticBlockEntity be,
            CallbackInfoReturnable<List<BlockPos>> cir
    ) {
        appliedcreate$LOGGER.info("[DIAG] getPotentialNeighbourLocations called: pos={}, originalSize={}",
                be.getBlockPos(), cir.getReturnValue().size());
        Set<BlockPos> remotePositions = KineticBridgeRegistry.INSTANCE.getRemotePositions(be.getBlockPos());
        if (remotePositions != null && !remotePositions.isEmpty()) {
            appliedcreate$LOGGER.info("[DIAG] getPotentialNeighbourLocations RETURN: pos={}, found {} bridge remotes: {}",
                    be.getBlockPos(), remotePositions.size(), remotePositions);
            List<BlockPos> result = new ArrayList<>(cir.getReturnValue());
            for (BlockPos remote : remotePositions) {
                if (!result.contains(remote)) {
                    result.add(remote);
                }
            }
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
        if (KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos())) {
            appliedcreate$LOGGER.info("[DIAG] isConnected: bridge match from={} to={}, returning true",
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
        if (KineticBridgeRegistry.INSTANCE.areBridged(from.getBlockPos(), to.getBlockPos())) {
            appliedcreate$LOGGER.info("[DIAG] getRotationSpeedModifier: bridge match from={} to={}, returning 1.0f",
                    from.getBlockPos(), to.getBlockPos());
            cir.setReturnValue(1.0f);
        }
    }
}
