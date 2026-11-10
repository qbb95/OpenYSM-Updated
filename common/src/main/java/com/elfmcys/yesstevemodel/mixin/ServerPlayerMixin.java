package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, remap = false)
public abstract class ServerPlayerMixin {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean bl, boolean bl2, CallbackInfoReturnable<Boolean> cir) {
        Entity entity2;
        if (YesSteveModel.isAvailable() && entity.getFirstPassenger() == (entity2 = (ServerPlayer) (Object) this)) {
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
}