package com.elfmcys.yesstevemodel.fabric.mixin;

import com.elfmcys.yesstevemodel.client.event.MobEffectEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityEffectMixin {

    @Inject(method = "onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void ysm$onEffectAdded(MobEffectInstance instance, Entity source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectEvent.onEffectAdded(self, instance.getEffect(), instance.getAmplifier());
    }

    @Inject(method = "onEffectsRemoved(Ljava/util/Collection;)V", at = @At("HEAD"))
    private void ysm$onEffectsRemoved(Collection<MobEffectInstance> collection, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        for (MobEffectInstance mobEffectInstance : collection) {
            MobEffectEvent.onEffectRemoved(self, mobEffectInstance.getEffect());
        }
    }
}
