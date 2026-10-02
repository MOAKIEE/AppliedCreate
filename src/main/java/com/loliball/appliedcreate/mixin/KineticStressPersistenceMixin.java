package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.energy.StressNetworkController;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticStressPersistenceMixin {
    @Inject(method = "write", at = @At("TAIL"), require = 1)
    private void appliedcreate$doNotPersistPaidCapacity(CompoundTag tag, HolderLookup.Provider registries,
                                                      boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket) {
            StressNetworkController.writePersistentNetwork((KineticBlockEntity) (Object) this, tag);
        }
    }
}
