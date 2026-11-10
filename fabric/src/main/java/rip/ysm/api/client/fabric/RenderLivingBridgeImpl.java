package rip.ysm.api.client.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

public final class RenderLivingBridgeImpl {

    private RenderLivingBridgeImpl() {
    }

    public static boolean firePre(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight) {
        return false;
    }

    public static void firePost(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight) {
    }
}
