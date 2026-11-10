package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.alchemy.PotionContents;

public class EffectLevel extends ContextFunction<Entity> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }

    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int effects = 0;

        for (int i = 0; i < arguments.size(); i++) {
            Identifier effectId = arguments.getIdentifier(context, i);
            if (effectId != null) {
                Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT
                        .get(ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, effectId))
                        .map(h -> (Holder<MobEffect>) h)
                        .orElse(null);
                if (mobEffectHolder != null) {
                    if (context.entity().geoInstance() instanceof PlayerCapability cap
                            && !cap.isLocalPlayerModel()) {
                        effects += cap.getPositionTracker().getEffectAmplifier(mobEffectHolder);
                    } else if (((IContext<?>)context.entity()).entity() instanceof LivingEntity) {
                        MobEffectInstance mobEffectInstance = ((LivingEntity)((IContext<?>)context.entity()).entity())
                                .getEffect(mobEffectHolder);
                        if (mobEffectInstance != null) {
                            effects += mobEffectInstance.getAmplifier() + 1;
                        }
                    } else {
                        if (!(((IContext<?>)context.entity()).entity() instanceof Arrow)) {
                            return null;
                        }

                        PotionContents potionContents = ((Arrow) context.entity()).getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

                        for (MobEffectInstance mobEffectInstance : potionContents.getAllEffects()) {
                            if (mobEffectInstance.getEffect() == mobEffectHolder) {
                                effects += mobEffectInstance.getAmplifier() + 1;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return effects;
    }
}
