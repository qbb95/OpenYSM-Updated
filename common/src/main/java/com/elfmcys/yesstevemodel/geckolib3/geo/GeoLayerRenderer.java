package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public abstract class GeoLayerRenderer<T extends AnimatableEntity<?>> {
    public abstract void render(AvatarRenderState state, PoseStack poseStack, DeferredBufferSource multiBufferSource, int packedLightIn, T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch);
}