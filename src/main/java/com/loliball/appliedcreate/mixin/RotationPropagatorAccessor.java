package com.loliball.appliedcreate.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor interface for RotationPropagator's private static methods.
 * Used by RotationPropagatorMixin to call propagateMissingSource on
 * cross-dimension bridge partners during handleRemoved.
 */
@Mixin(value = RotationPropagator.class, remap = false)
public interface RotationPropagatorAccessor {

    @Invoker("propagateMissingSource")
    static void appliedcreate$invokePropMissingSource(KineticBlockEntity updateTE) {
        throw new AssertionError("Mixin invoker not applied");
    }
}
