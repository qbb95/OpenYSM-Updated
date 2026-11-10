package rip.ysm.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

public final class RenderLivingBridge {

    private RenderLivingBridge() {
    }

    @ExpectPlatform
    public static boolean firePre(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void firePost(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, float partialTick, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight) {
        throw new AssertionError();
    }
}
