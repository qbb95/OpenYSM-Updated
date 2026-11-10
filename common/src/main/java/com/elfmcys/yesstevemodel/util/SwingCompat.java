package com.elfmcys.yesstevemodel.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Adapt the legacy animation bindings to Minecraft 26.3's swing descriptions. */
public final class SwingCompat {
    private SwingCompat() {}

    public static InteractionHand hand(LivingEntity entity) {
        var swing = entity.getCurrentSwing();
        return swing == null ? InteractionHand.MAIN_HAND : swing.hand();
    }

    public static int ticks(LivingEntity entity) {
        var swing = entity.getCurrentSwing();
        return swing == null ? 0 : Math.round(entity.getSwingAnimation(1.0f) * swing.durationTicks());
    }
}
