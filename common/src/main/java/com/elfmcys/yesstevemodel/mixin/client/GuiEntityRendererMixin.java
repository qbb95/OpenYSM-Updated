package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.PreviewEntityRegistry;
import com.elfmcys.yesstevemodel.client.renderer.RenderContext;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEntityRenderer.class)
public class GuiEntityRendererMixin {

    @Inject(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
    private void ysm$enterPreviewMode(GuiEntityRenderState state, PoseStack poseStack, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("RETURN"))
    private void ysm$exitPreviewMode(GuiEntityRenderState state, PoseStack poseStack, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }

    @Redirect(
            method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
            )
    )
    private void ysm$redirectPreviewDispatch(
            EntityRenderDispatcher dispatcher,
            EntityRenderState state,
            CameraRenderState cameraState,
            double x, double y, double z,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        PreviewEntityRegistry.Entry entry = PreviewEntityRegistry.getEntry(state);
        if (entry != null && entry.animatable() != null && state instanceof AvatarRenderState playerState) {
            try {
                RenderContext.enter(collector, cameraState);
                DeferredBufferSource bufferSource = RenderContext.bufferSource();
                try {
                    if (entry.beforeEntity() != null) {
                        entry.beforeEntity().render(poseStack, bufferSource, state.lightCoords);
                    }
                    CustomPlayerRenderer renderer = RendererManager.getPlayerRenderer();
                    float framePartialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    // Animation offsets belong to the character's model space,
                    // after the GUI camera rotation. Scenery keeps its own origin.
                    poseStack.pushPose();
                    try {
                        if (entry.modelPoseTransform() != null) {
                            entry.modelPoseTransform().apply(poseStack);
                        }
                        renderer.renderEntity(entry.animatable(), playerState, 0.0f, framePartialTick, poseStack, bufferSource, state.lightCoords);
                    } finally {
                        poseStack.popPose();
                    }
                    if (entry.afterEntity() != null) {
                        entry.afterEntity().render(poseStack, bufferSource, state.lightCoords);
                    }
                } finally {
                    RenderContext.exit();
                }
            } finally {
                PreviewEntityRegistry.remove(state);
            }
            return;
        }
        dispatcher.submit(state, cameraState, x, y, z, poseStack, collector);
    }
}
