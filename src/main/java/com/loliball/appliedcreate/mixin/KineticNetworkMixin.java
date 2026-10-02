package com.loliball.appliedcreate.mixin;

import com.loliball.appliedcreate.energy.StressNetworkAccess;
import com.loliball.appliedcreate.energy.StressNetworkController;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin implements StressNetworkAccess {
    @Shadow private float currentCapacity;
    @Shadow private float currentStress;
    @Shadow public abstract void sync();

    @Inject(method = {"updateCapacity", "updateStress", "updateNetwork"}, at = @At("HEAD"),
            cancellable = true, require = 3)
    private void appliedcreate$balanceBeforeOverload(CallbackInfo ci) {
        if (StressNetworkController.update((KineticNetwork) (Object) this)) ci.cancel();
    }

    @Inject(method = "add", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/kinetics/KineticNetwork;updateFromNetwork(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V"), require = 1)
    private void appliedcreate$memberAdded(KineticBlockEntity member, CallbackInfo ci) {
        StressNetworkController.update((KineticNetwork) (Object) this);
    }

    @Inject(method = "addSilently", at = @At("RETURN"), require = 1)
    private void appliedcreate$memberLoaded(KineticBlockEntity member, float capacity, float stress, CallbackInfo ci) {
        StressNetworkController.update((KineticNetwork) (Object) this);
    }

    @Inject(method = "remove", at = @At("RETURN"), require = 1)
    private void appliedcreate$memberRemoved(KineticBlockEntity member, CallbackInfo ci) {
        StressNetworkController.update((KineticNetwork) (Object) this);
    }

    @Override
    public void appliedcreate$publishStress(float capacity, float stress) {
        if (currentCapacity == capacity && currentStress == stress) return;
        currentCapacity = capacity;
        currentStress = stress;
        sync();
    }
}
