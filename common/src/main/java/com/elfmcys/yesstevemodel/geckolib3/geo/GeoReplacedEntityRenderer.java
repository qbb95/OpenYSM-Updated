package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.extended.LivingEntityRendererAccessor;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.elfmcys.yesstevemodel.mixin.client.LivingEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.api.client.RenderLivingBridge;

import java.util.List;
import java.util.Optional;

public abstract class GeoReplacedEntityRenderer<TEntity extends Player, T extends LivingAnimatable<TEntity>, S extends AvatarRenderState> extends LivingEntityRenderer<TEntity, S, PlayerModel> implements IGeoRenderer<T> {

    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();

    public Matrix4f dispatchedMat = new Matrix4f();

    public Matrix4f renderEarlyMat = new Matrix4f();

    public MultiBufferSource rtb;

    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        this.rtb = null;
    }

    public static int packOverlayCoords(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.currentModelRenderCycle = cycle;
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
        this.renderEarlyMat.set(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public void renderEntity(T t, S state, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderEntityWithTexture(t, state, null, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public void renderEntityWithTexture(T t, S state, @Nullable Identifier identifier, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        Direction bedOrientation;
        if (RenderLivingBridge.firePre(t.getEntity(), this, partialTick, poseStack, multiBufferSource, packedLight)) {
            return;
        }
        TEntity entity = t.getEntity();

        float savedYBodyRot = 0.0f, savedYBodyRotO = 0.0f;
        float savedYHeadRot = 0.0f, savedYHeadRotO = 0.0f;
        float savedYRot = 0.0f, savedYRotO = 0.0f;
        float savedXRot = 0.0f, savedXRotO = 0.0f;
        // 外部捕获（RealCamera 的模型绑定视图等）也会临时改写实体的 bodyYaw/headYaw/xRot，
        // 但不写对应的上一帧字段（lastBodyYaw/yHeadRotO/yRotO/xRotO）——它假定权威旋转全在
        // render state 里。若这里仍按 rotLerp(partialTick, 上一帧, 当前) 采样，每个 tick
        // partialTick 从 0 走到 1 就会让身体朝向在"真实朝向"和"被改写的朝向"之间来回摆一次，
        // 表现为模型不停旋转抽搐。因此外部捕获同样按 render state 锁定四个旋转。
        boolean syncRotationsForPreview = (com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer.isPreview()
                || com.elfmcys.yesstevemodel.client.renderer.RenderContext.isExternalCapture()) && entity != null;
        if (syncRotationsForPreview) {
            savedYBodyRot = entity.yBodyRot;
            savedYBodyRotO = entity.yBodyRotO;
            savedYHeadRot = entity.yHeadRot;
            savedYHeadRotO = entity.yHeadRotO;
            savedYRot = entity.getYRot();
            savedYRotO = entity.yRotO;
            savedXRot = entity.getXRot();
            savedXRotO = entity.xRotO;

            float bodyRot = state.bodyRot;
            float headYaw = state.bodyRot + state.yRot;
            entity.yBodyRot = bodyRot;
            entity.yBodyRotO = bodyRot;
            entity.yHeadRot = headYaw;
            entity.yHeadRotO = headYaw;
            entity.setYRot(headYaw);
            entity.yRotO = headYaw;
            entity.setXRot(state.xRot);
            entity.xRotO = state.xRot;
        }
        AnimationEvent<?> event;
        try {
            event = t.processAnimation(partialTick);
        } finally {
            if (syncRotationsForPreview) {
                entity.yBodyRot = savedYBodyRot;
                entity.yBodyRotO = savedYBodyRotO;
                entity.yHeadRot = savedYHeadRot;
                entity.yHeadRotO = savedYHeadRotO;
                entity.setYRot(savedYRot);
                entity.yRotO = savedYRotO;
                entity.setXRot(savedXRot);
                entity.xRotO = savedXRotO;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            EntityModelData modelData = event.getModelData();
            // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
            this.dispatchedMat.set(poseStack.last().pose());
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            if (entity.getPose() == Pose.SLEEPING && (bedOrientation = entity.getBedOrientation()) != null) {
                float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                poseStack.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
            }
            setupRotations(entity, state, poseStack, modelData.lerpedAge, modelData.lerpBodyRot, partialTick, 1.0f);
            if (t.getEntity().getVehicle() != null) {
                VehicleCapability.get(t.getEntity().getVehicle()).ifPresent(cap -> {
                    Vector3f vector3f = cap.getExpressionOffset();
                    if (vector3f != null) {
                        poseStack.mulPose(new Quaternionf().rotateZYX(vector3f.z, 0.0f, vector3f.x).invert());
                    }
                });
            }
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0.0f, 0.01f, 0.0f);
            AnimatedGeoModel animatedGeoModel = t.getCurrentModel();
            // 第一人称世界渲染时临时把头部骨整体缩到 0：CPU / native / GPU 三条路径都以
            // "scale 全为 0 即不可见" 为准，子骨（头发、头饰等）会跟着父骨一起消失。
            // 只在这一遍渲染里生效，结束后立刻还原，所以纸娃娃 / 模型预览 / RealCamera 外部捕获不受影响。
            List<IBone> hiddenHeads = List.of();
            float[] hiddenScales = null;
            if (shouldHideHeadInFirstPerson(entity, minecraft)) {
                hiddenHeads = collectHeadBonesToHide(animatedGeoModel);
                if (!hiddenHeads.isEmpty()) {
                    hiddenScales = new float[hiddenHeads.size() * 4];
                    for (int i = 0; i < hiddenHeads.size(); i++) {
                        IBone bone = hiddenHeads.get(i);
                        int o = i * 4;
                        hiddenScales[o] = bone.getScaleX();
                        hiddenScales[o + 1] = bone.getScaleY();
                        hiddenScales[o + 2] = bone.getScaleZ();
                        hiddenScales[o + 3] = bone.isHidden() ? 1.0f : 0.0f;
                        bone.setScaleX(0.0f);
                        bone.setScaleY(0.0f);
                        bone.setScaleZ(0.0f);
                        bone.setHidden(true);
                    }
                    ModelPreviewRenderer.setHeadHiddenInFirstPerson(true);
                }
            }
            try {
                int textureIndex = identifier == null ? t.getTextureIndex() : 0;
                RenderType renderType = getRenderType(identifier == null ? t.getTextureLocation() : identifier, isBodyVisible(state) && !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), t.getCurrentModel().getGeoModel().isTranslucentTexture(textureIndex));
                boolean useExtraPlayer = t.isRenderLayersFirst();
                Color color = getRenderColor(t, partialTick, poseStack, multiBufferSource, null, packedLight);
                renderWithBone(animatedGeoModel, t, partialTick, poseStack, multiBufferSource, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                if (useExtraPlayer && !entity.isSpectator()) {
                    render(t, state, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
                }
                if (renderType != null) {
                    renderWithBoneAndRenderType(animatedGeoModel, t, partialTick, renderType, poseStack, multiBufferSource, textureIndex, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                }
                if (!useExtraPlayer && !entity.isSpectator()) {
                    render(t, state, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
                }
                poseStack.popPose();
            } finally {
                if (hiddenScales != null) {
                    for (int i = 0; i < hiddenHeads.size(); i++) {
                        IBone bone = hiddenHeads.get(i);
                        int o = i * 4;
                        bone.setScaleX(hiddenScales[o]);
                        bone.setScaleY(hiddenScales[o + 1]);
                        bone.setScaleZ(hiddenScales[o + 2]);
                        bone.setHidden(hiddenScales[o + 3] != 0.0f);
                    }
                    ModelPreviewRenderer.setHeadHiddenInFirstPerson(false);
                }
            }
        }
        net.minecraft.client.renderer.SubmitNodeCollector activeCollector = com.elfmcys.yesstevemodel.client.renderer.RenderContext.collector();
        net.minecraft.client.renderer.state.CameraRenderState activeCameraState = com.elfmcys.yesstevemodel.client.renderer.RenderContext.camera();
        if (activeCollector != null && activeCameraState != null
                && entity != null && entity != minecraft.getCameraEntity()) {
            ((LivingEntityRendererAccessor) this).tlm$renderNameTag(state, poseStack, activeCollector, activeCameraState);
        }
        RenderLivingBridge.firePost(entity, this, partialTick, poseStack, multiBufferSource, packedLight);
    }

    /**
     * 要隐藏的头部根骨集合。优先取所有 {@code AllHead*} 骨（双骨架模型会同时存在 AllHead 和 AllHead3，
     * 只有其中一棵树真正带可见网格），没有 AllHead 时退回 headBones 链的最后一骨。
     */
    private static List<IBone> collectHeadBonesToHide(AnimatedGeoModel model) {
        if (!model.allHeadBones().isEmpty()) {
            return model.allHeadBones();
        }
        if (model.allHeadBone() != null) {
            return List.of(model.allHeadBone());
        }
        List<IBone> heads = model.headBones();
        if (!heads.isEmpty()) {
            return List.of(heads.get(heads.size() - 1));
        }
        return List.of();
    }

    /**
     * 只有"本地玩家 + 世界渲染 + 第一人称"这一种组合才隐藏头部。
     * 纸娃娃（extra player）、模型预览、RealCamera 的模型捕获视图都走各自的分支，这里必须返回 false，
     * 否则左上角的额外玩家模型也会掉头。
     */
    protected boolean shouldHideHeadInFirstPerson(TEntity entity, Minecraft minecraft) {
        return shouldHideHeadInFirstPerson0(entity, minecraft);
    }

    private boolean shouldHideHeadInFirstPerson0(TEntity entity, Minecraft minecraft) {
        if (!GeneralConfig.HIDE_HEAD_IN_FIRST_PERSON.get().booleanValue()) {
            return false;
        }
        if (entity == null || minecraft.player == null || entity != minecraft.player) {
            return false;
        }
        if (ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer()) {
            return false;
        }
        // 第一人称下本地玩家不是由世界渲染提交的：原版直接跳过相机实体，RealCamera 绑定模式则在
        // LevelRenderer#submitEntities 的 RETURN 处自建 RoutingSubmitCollector 再画一遍
        // （所以这里是 external capture，不能按 capture 排除）。RealCamera 的模型绑定视图是
        // 一个 Screen，用它把"游戏内第一人称"和"GUI 里的模型捕获"区分开。
        // 注意只排除 RealCamera 自己的采集/配置界面，而不是"任何 Screen"：`ESC` 暂停、背包、聊天
        // 都只是叠在世界上的 GUI，世界照常在画，此时头部必须继续隐藏 —— 否则一暂停脑袋就冒出来
        // （2026-11-12 报的问题）。
        if (isModelCaptureScreen(minecraft.screen)) {
            return false;
        }
        return minecraft.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON;
    }

    /**
     * 是否是 RealCamera 的模型采集 / 配置界面（按类名字符串判断，不 import 可选依赖的类型）。
     * 这类界面会用自己新建的 SubmitNodeCollector 抓模型几何，隐藏头部会让预览缺头，必须放行。
     */
    private static boolean isModelCaptureScreen(Object screen) {
        return screen != null && screen.getClass().getName().startsWith("com.xtracr.realcamera.");
    }

    public void render(T entity, S state, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, AnimationEvent<?> event, EntityModelData data) {        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.render(state, poseStack, bufferSource, packedLightIn, entity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick, data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch);
        }
    }

    public float getHurtOverlayProgress(TEntity entity, float partialTick) {
        return 0.0f;
    }

    public void preRenderCallback(TEntity entity, PoseStack poseStack, float partialTick) {
    }

    public void setupRotations(TEntity tentity, S state, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        int t = tentity.deathTime;
        boolean zIsAutoSpinAttack = tentity.isAutoSpinAttack();
        if (t > 0) {
            tentity.deathTime = 0;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, false);
        }
        if (tentity.onClimbable()) {
            Optional<BlockPos> lastClimbablePos = tentity.getLastClimbablePos();
            if (lastClimbablePos.isPresent()) {
                Optional<Direction> optionalValue = tentity.level().getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (optionalValue.isPresent()) {
                    rotationYaw = optionalValue.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }
        super.setupRotations(state, poseStack, rotationYaw, scale);
        if (t > 0) {
            tentity.deathTime = t;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, true);
        }
    }

    @Override
    public boolean shouldShowName(TEntity entity, double distance) {
        double d = entity.isDiscrete() ? 32.0d : 64.0d;
        return distance < d * d && entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && Minecraft.renderNames();
    }

    public final boolean addLayerRenderer(GeoLayerRenderer<T> layerRenderer) {
        return this.layerRenderers.add(layerRenderer);
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.rtb = bufferSource;
    }

    @Override
    public void extractRenderState(TEntity entity, S state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
    }
}
