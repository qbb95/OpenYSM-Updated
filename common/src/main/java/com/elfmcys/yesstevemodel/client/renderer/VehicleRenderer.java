package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class VehicleRenderer extends GeoEntityRenderer<Entity, GeckoVehicleEntity> {
    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(Entity entity, EntityRenderState state, float entityYaw, float partialTick, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || entity.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        VehicleCapability.get(entity).ifPresent(cap -> {
            cap.tickModel();
            renderEntity(cap, state, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }

    @NotNull
    public Identifier getTextureLocation(Entity entity) {
        return VehicleCapability.get(entity).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
