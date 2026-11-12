package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.RenderContext;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import rip.ysm.compat.simplehats.SimpleHatsHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public class CustomPlayerArmorLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemInHandRenderer itemRenderer;

    public CustomPlayerArmorLayer(EntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;
    }

    @Override
    public void render(AvatarRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        if (com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer.isHeadHiddenInFirstPerson()) {
            // 头骨已被第一人称隐藏，头部挂件跟着一起跳过，否则会浮在半空
            return;
        }
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model != null && !model.headBones().isEmpty()) {
            ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemBySlot.isEmpty() && !isArmorItem(itemBySlot)) {
                renderArmorPiece(poseStack, bufferSource, packedLightIn, model, player, itemBySlot);
            }
            ItemStack stack = SimpleHatsHelper.getHatItem(player);
            if (stack != null && !stack.isEmpty()) {
                renderArmorPiece(poseStack, bufferSource, packedLightIn, model, player, stack);
            }
        }
    }

    private boolean isArmorItem(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.HEAD;
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, int i, AnimatedGeoModel model, Player player, ItemStack stack) {
        SubmitNodeCollector collector = RenderContext.collector();
        if (collector == null) {
            return;
        }
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, model.headBones());
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.translate(0.0f, 0.25f, 0.0f);
        this.itemRenderer.renderItem(player, stack, ItemDisplayContext.HEAD, poseStack, collector, i);
        poseStack.popPose();
    }
}