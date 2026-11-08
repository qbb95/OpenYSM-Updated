package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.CustomEntityTranslucentRenderType;
import com.elfmcys.yesstevemodel.client.renderer.RenderContext;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGeoRenderer<T extends AnimatableEntity<?>> {
    MultiBufferSource getCurrentRTB();

    default void setCurrentRTB(MultiBufferSource bufferSource) {
    }

    default void renderWithBone(AnimatedGeoModel model, T animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer vertexConsumer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
        renderLate(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    default void renderWithBoneAndRenderType(AnimatedGeoModel model, T animatable, float partialTick, RenderType renderType, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, int i, @Nullable VertexConsumer vertexConsumer, int i2, int i3, float f2, float f3, float f4, float f5) {
        SubmitNodeCollector collector = RenderContext.collector();
        if (vertexConsumer == null && collector != null && RenderContext.isExternalCapture()) {
            submitMeshToCollector(collector, model, animatable, poseStack, renderType, i, i2, i3, f2, f3, f4, f5);
            return;
        }
        if (vertexConsumer == null) {
            vertexConsumer = bufferSource.getBuffer(renderType);
        }
        animatable.resetAnimationState();
        Identifier tex = animatable.getTextureLocation();
        NativeModelRenderer.renderMesh(vertexConsumer, poseStack.last(), model.getGeoModel(), model.getMatrixData(), model.getAbsPivotData(), i, 0, i2, i3, f2, f3, f4, f5, tex);
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    /**
     * 把模型网格作为 custom geometry 提交给 collector。
     * <p>
     * RealCamera 的模型绑定视图 / 绑定计算用的是自己新建的 {@code SubmitNodeCollector}，
     * 只认通过 collector 提交的几何；直接写 {@code Minecraft.renderBuffers().bufferSource()} 或者走 GPU 直写
     * 都不会被捕获，模型在视图里就是空的（看不到，也绑定不上）。
     * <p>
     * 因此这里改为 submitCustomGeometry + 纯 Java 顶点路径，保证几何一定经由 VertexConsumer 输出。
     */
    default void submitMeshToCollector(SubmitNodeCollector collector, AnimatedGeoModel model, T animatable, PoseStack poseStack, RenderType renderType, int textureIndex, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        final GeoModel geoModel = model.getGeoModel();
        // 捕获方可能在稍后才真正渲染（延迟 flush），先把骨矩阵快照下来，避免被同一帧内后续的动画采样覆盖
        final float[] boneParams = model.getMatrixData().clone();
        animatable.resetAnimationState();
        // 绝对 pivot 缓冲（stateBuffer）只是给 native 路径输出用的，这里传 null：避免在延迟渲染时
        // 回写共享数组、污染该模型后续帧的数据
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                NativeModelRenderer.renderVertices(buffer, pose, geoModel, boneParams, null, textureIndex, 0, packedLight, packedOverlay, red, green, blue, alpha));
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }

    default void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                             @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = animatable.getHeightScale();
            float height = animatable.getWidthScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            @Nullable VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue,
                            float alpha) {
    }

    @Nullable
    default RenderType getRenderType(Identifier identifier, boolean z, boolean z2, boolean z3) {
        if (z) {
            if (z3) {
                return CustomEntityTranslucentRenderType.get(identifier);
            }
            return RenderTypes.entityCutoutNoCull(identifier);
        }
        if (z2) {
            return RenderTypes.outline(identifier);
        }
        return null;
    }

    default Color getRenderColor(T animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }

    @NotNull
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }
}