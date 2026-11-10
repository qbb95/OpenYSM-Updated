package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import com.mojang.math.Axis;

public class CustomPlayerParrotLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private static final String TAG_ID = "id";
    private static final String TAG_VARIANT = "Variant";

    private final ParrotModel parrotModel;
    private final ParrotRenderState parrotState = new ParrotRenderState();

    public CustomPlayerParrotLayer(EntityRendererProvider.Context context) {
        this.parrotModel = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
        this.parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
    }

    @Override
    public void render(AvatarRenderState state, PoseStack poseStack, DeferredBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model == null) {
            return;
        }
        if (!model.leftShoulderBones().isEmpty()) {
            renderParrot(poseStack, state, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!model.rightShoulderBones().isEmpty()) {
            renderParrot(poseStack, state, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }

    private void renderParrot(PoseStack poseStack, AvatarRenderState state, DeferredBufferSource bufferSource, AnimatedGeoModel model, int packedLightIn, Player player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean isLeftShoulder) {
        Parrot.Variant variant = isLeftShoulder ? state.parrotOnLeftShoulder : state.parrotOnRightShoulder;

        if (variant == null)
            return;

        poseStack.pushPose();
        applyParrotTransform(poseStack, model, isLeftShoulder);
        poseStack.rotate(Axis.ZP.rotationDegrees(180.0f));
        this.renderOnShoulder(poseStack, bufferSource, packedLightIn, state, variant, headPitch, netHeadYaw, isLeftShoulder);
        poseStack.popPose();
    }

    private void renderOnShoulder(
            PoseStack poseStack,
            DeferredBufferSource bufferSource,
            int packedLight,
            AvatarRenderState state,
            Parrot.Variant variant,
            float pitch,
            float yaw,
            boolean leftShoulder
    ) {
        poseStack.pushPose();
        poseStack.translate(/*leftShoulder ? 0.2F : -0.2F*/0, state.isCrouching ? -1.3F : -1.5F, 0.0F);
        this.parrotState.ageInTicks = state.ageInTicks;
        this.parrotState.walkAnimationPos = state.walkAnimationPos;
        this.parrotState.walkAnimationSpeed = state.walkAnimationSpeed;
        this.parrotState.yRot = yaw;
        this.parrotState.xRot = pitch;
        this.parrotModel.setupAnim(this.parrotState);
        this.parrotModel
                .renderToBuffer(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(variant))), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }

    public void applyParrotTransform(PoseStack poseStack, AnimatedGeoModel model, boolean isLeftShoulder) {
        if (isLeftShoulder) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftShoulderBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, model.rightShoulderBones());
        }
    }
}
