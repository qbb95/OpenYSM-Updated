package com.elfmcys.yesstevemodel.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ItemRenderCompat {
    private ItemRenderCompat() {}

    public static void submit(LivingEntity entity, ItemStack item, ItemDisplayContext context,
            PoseStack pose, SubmitNodeCollector collector, int light) {
        var state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(state, item, context, entity);
        state.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }
}
