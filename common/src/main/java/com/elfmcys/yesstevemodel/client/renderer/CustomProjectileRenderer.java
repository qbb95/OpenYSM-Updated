package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.projectile.Projectile;

public class CustomProjectileRenderer {
    public static boolean renderProjectile(Projectile projectile, EntityRenderState state, float partialTick, PoseStack poseStack, DeferredBufferSource multiBufferSource, int packedLight) {
        return ProjectileCapability.get(projectile).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                RendererManager.getProjectileRenderer().render(cap.getEntity(), state, partialTick, poseStack, multiBufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }
}
