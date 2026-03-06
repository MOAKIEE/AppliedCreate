package com.loliball.appliedcreate.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DEBUG MIXIN: Captures the exact moment and call stack when a KineticBlockEntity
 * transitions to overStressed state. This is temporary — remove after debugging.
 *
 * Target: KineticBlockEntity.updateFromNetwork(float, float, int)
 * This method is called by KineticNetwork.sync() and is the ONLY place where
 * overStressed is computed from network stress values at runtime (not NBT read).
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityDebugMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("AppliedCreate/KineticDebug");

    @Shadow
    protected boolean overStressed;


    @Shadow
    public abstract float getTheoreticalSpeed();

    @Shadow
    public abstract boolean hasSource();

    @Shadow
    public abstract boolean hasNetwork();

    /**
     * Inject at HEAD of updateFromNetwork to detect when overStressed transitions to true.
     * Logs full stack trace so we can see exactly who triggered the stress recalculation.
     */
    @Inject(
            method = "updateFromNetwork(FFI)V",
            at = @At("HEAD")
    )
    private void appliedcreate$debugUpdateFromNetwork(float maxStress, float currentStress, int networkSize, CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        boolean wouldBeOverStressed = maxStress < currentStress;
        if (wouldBeOverStressed && !this.overStressed) {
            // This is the transition point: block is about to become overStressed
            BlockPos pos = self.getBlockPos();
            LOGGER.error("[OVERSTRESS TRANSITION] Block at {} will become overStressed! " +
                            "maxStress={}, currentStress={}, networkSize={}, " +
                            "currentSpeed={}, hasSource={}, hasNetwork={}",
                    pos, maxStress, currentStress, networkSize,
                    getTheoreticalSpeed(), hasSource(), hasNetwork());
            LOGGER.error("[OVERSTRESS TRANSITION] Full stack trace for block at {}:", pos,
                    new Throwable("Overstress trigger trace"));
        }
        // Also log when stress state changes in general (non-transition cases)
        if (wouldBeOverStressed) {
            BlockPos pos = self.getBlockPos();
            LOGGER.warn("[STRESS UPDATE] Block at {} updateFromNetwork: maxStress={}, currentStress={}, networkSize={}, " +
                            "alreadyOverStressed={}, speed={}",
                    pos, maxStress, currentStress, networkSize, this.overStressed, getTheoreticalSpeed());
        }
    }

    /**
     * Inject at HEAD of attachKinetics to log when kinetic networks are being rebuilt.
     * attachKinetics() calls RotationPropagator.handleAdded which triggers BFS propagation.
     */
    @Inject(
            method = "attachKinetics()V",
            at = @At("HEAD")
    )
    private void appliedcreate$debugAttachKinetics(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            BlockPos pos = self.getBlockPos();
            LOGGER.info("[ATTACH KINETICS] Block at {} type={}, speed={}, hasSource={}, hasNetwork={}, overStressed={}",
                    pos, self.getBlockState().getBlock().getClass().getSimpleName(),
                    getTheoreticalSpeed(), hasSource(), hasNetwork(), this.overStressed);
            LOGGER.info("[ATTACH KINETICS] Stack trace for block at {}:", pos,
                    new Throwable("attachKinetics trace"));
        }
    }

    /**
     * Inject at HEAD of detachKinetics to log when kinetic networks are being torn down.
     */
    @Inject(
            method = "detachKinetics()V",
            at = @At("HEAD")
    )
    private void appliedcreate$debugDetachKinetics(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            BlockPos pos = self.getBlockPos();
            LOGGER.info("[DETACH KINETICS] Block at {} type={}, speed={}, hasSource={}, hasNetwork={}, overStressed={}",
                    pos, self.getBlockState().getBlock().getClass().getSimpleName(),
                    getTheoreticalSpeed(), hasSource(), hasNetwork(), this.overStressed);
        }
    }
}
