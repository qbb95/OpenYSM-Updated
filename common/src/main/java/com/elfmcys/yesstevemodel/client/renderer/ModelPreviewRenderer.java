package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.oculus.OculusCompat;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import com.elfmcys.yesstevemodel.client.animation.AnimationTracker;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.util.AnimatableCacheUtil;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.render.DeferredBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.ExecutionException;
import com.mojang.math.Axis;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;

public final class ModelPreviewRenderer {

    private static boolean isPreviewMode = false;

    private static boolean isExtraPlayerMode = false;

    private static boolean isFirstPersonMode = false;

    public static void setPreviewMode(boolean previewMode) {
        isPreviewMode = previewMode;
    }

    public static boolean isPreview() {
        return isPreviewMode;
    }

    public static void setExtraPlayerMode(boolean extraPlayerMode) {
        isExtraPlayerMode = extraPlayerMode;
    }

    public static boolean isExtraPlayer() {
        return isExtraPlayerMode;
    }

    public static void setFirstPersonMode(boolean firstPersonMode) {
        isFirstPersonMode = firstPersonMode;
    }

    public static boolean isFirstPerson() {
        return isFirstPersonMode || OculusCompat.isPBRActive() || FirstPersonCompat.isFirstPersonActive();
    }

    public static boolean isFirstPersonOnRenderThread() {
        RenderSystem.assertOnRenderThread();
        return isFirstPersonMode && !FirstPersonCompat.isFirstPersonActive();
    }

    public static void renderVehicleModel(Entity entity, PoseStack poseStack, float partialTick) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            VehicleCapability.get(vehicle).ifPresent(cap -> {
                int index;
                AnimatedGeoModel model;
                List<IBone> list;
                if (!cap.isModelInitialized() || !cap.isModelReady() || (index = vehicle.getPassengers().indexOf(entity)) < 0 || (model = cap.getCurrentModel()) == null || model.passengerGroupChains().isEmpty() || index >= model.passengerGroupChains().size() || (list = model.passengerGroupChains().get(index)) == null) {
                    return;
                }
                float bodyRotation = CustomVehicleRenderer.getBodyRotation(vehicle, Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()), partialTick);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRotation));
                RenderUtils.prepMatrixForLocator(poseStack, list);
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0f - bodyRotation));
                double myRidingOffset = -(vehicle.getPassengerRidingPosition(entity).y - vehicle.getY());
                if (((entity instanceof Player) && PlayerCapability.get(entity).isPresent()) || TouhouLittleMaidCompat.isMaidRideable(entity)) {
                    myRidingOffset -= 0.5d;
                }
                poseStack.translate(0.0d, myRidingOffset, 0.0d);
            });
        }
    }

    public static void submitLivingEntityPreview(
            GuiGraphicsExtractor guiGraphics,
            int x0, int y0, int x1, int y1,
            int displaySize,
            float partialTick,
            PlayerPreviewEntity animatable,
            boolean disablePreviewRotation,
            boolean hideEquipment
    ) {
        LivingEntity entity = animatable.getEntity();
        if (entity == null) {
            return;
        }

        ItemStack[] savedEquipment = null;
        if (hideEquipment && entity instanceof Player player) {
            EquipmentSlot[] slots = EquipmentSlot.values();
            savedEquipment = new ItemStack[slots.length];
            for (int i = 0; i < slots.length; i++) {
                savedEquipment[i] = player.getItemBySlot(slots[i]).copy();
                player.setItemSlot(slots[i], ItemStack.EMPTY);
            }
        }

        CustomPlayerRenderer renderer = RendererManager.getPlayerRenderer();
        AvatarRenderState state = new AvatarRenderState();
        renderer.extractRenderState((Player) entity, state, partialTick);
        state.lightCoords = 15728880;

        float previewYaw = disablePreviewRotation ? 180.0F : 200.0F;
        state.bodyRot = previewYaw;
        state.yRot = 0.0F;
        state.xRot = 0.0F;

        PreviewEntityRegistry.register(state, animatable);

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraTilt = null;
        if (!disablePreviewRotation) {
            cameraTilt = new Quaternionf().rotateX((float) (-10.0 * Math.PI / 180.0));
            rotation.mul(cameraTilt);
        }

        float entityScale = entity.getScale();
        float yOffsetPx = disablePreviewRotation ? 5.5F : 0.0F;
        float yOffsetModel = yOffsetPx * entityScale / (float) displaySize;
        Vector3f translation = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffsetModel, 0.0F);
        float submitScale = (float) displaySize / entityScale;

        guiGraphics.enableScissor(x0, y0, x1, y1);
        guiGraphics.entity(state, submitScale, translation, rotation, cameraTilt, x0, y0, x1, y1);
        guiGraphics.disableScissor();

        if (savedEquipment != null) {
            Player player = (Player) entity;
            EquipmentSlot[] slots = EquipmentSlot.values();
            for (int i = 0; i < slots.length; i++) {
                player.setItemSlot(slots[i], savedEquipment[i]);
            }
        }
    }

    public static void submitPlayerOverlay(
            GuiGraphicsExtractor guiGraphics,
            LocalPlayer localPlayer,
            double x, double y,
            float scale,
            float yawOffset,
            float partialTick
    ) {
        PlayerCapability cap = PlayerCapability.get(localPlayer).orElse(null);
        if (cap == null) {
            return;
        }
        cap.tickModel();

        float cx = (float) (x + scale * 0.6F);
        float cy = (float) (y + scale * 1.0F);
        int halfW = (int) (scale * 2.0F);
        int halfH = (int) (scale * 2.5F);
        int x0 = (int) cx - halfW;
        int x1 = (int) cx + halfW;
        int y0 = (int) cy - halfH;
        int y1 = (int) cy + halfH;

        CustomPlayerRenderer renderer = RendererManager.getPlayerRenderer();
        AvatarRenderState state = new AvatarRenderState();
        renderer.extractRenderState(localPlayer, state, partialTick);
        state.lightCoords = 15728880;
        PreviewEntityRegistry.register(state, cap);

        state.bodyRot = 180.0F;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        float entityScale = localPlayer.getScale();
        Vector3f translation = new Vector3f(0.0F, localPlayer.getBbHeight() / 2.0F, 0.0F);
        float submitScale = scale / entityScale;

        guiGraphics.entity(state, submitScale, translation, rotation, null, x0, y0, x1, y1);
    }

    public static void submitTexturePreview(
            GuiGraphicsExtractor guiGraphics,
            int x0, int y0, int x1, int y1,
            float anchorX, float anchorY,
            float zoom,
            float pitch,
            float yaw,
            PlayerPreviewEntity animatable,
            boolean renderGround,
            float partialTick
    ) {
        LivingEntity entity = animatable.getEntity();
        if (entity == null) {
            return;
        }

        AnimationTracker tracker = animatable.getAnimationStateMachine();
        Pose oldPose = entity.getPose();
        Pose newPose = oldPose;
        float poseYOffset = 0.0F;
        if (tracker.isCurrentAnimation("sleep")) {
            newPose = Pose.SLEEPING;
        } else if (tracker.isCurrentAnimation("swim") || tracker.isCurrentAnimation("swim_stand")) {
            newPose = Pose.SWIMMING;
        } else if (tracker.isCurrentAnimation("sneak") || tracker.isCurrentAnimation("sneaking")) {
            newPose = Pose.CROUCHING;
        } else if (tracker.isCurrentAnimation("sit")) {
            poseYOffset = -0.5F;
        } else if (tracker.isCurrentAnimation("ride")) {
            poseYOffset = 0.85F;
        } else if (tracker.isCurrentAnimation("ride_pig")) {
            poseYOffset = 0.3125F;
        } else if (tracker.isCurrentAnimation("boat")) {
            poseYOffset = -0.45F;
        }
        boolean poseChanged = newPose != oldPose;
        if (poseChanged) {
            entity.setPose(newPose);
        }

        CustomPlayerRenderer renderer = RendererManager.getPlayerRenderer();
        AvatarRenderState state = new AvatarRenderState();
        renderer.extractRenderState((Player) entity, state, partialTick);
        state.lightCoords = 15728880;

        state.bodyRot = -yaw;
        state.yRot = Mth.wrapDegrees(180.0F + yaw);
        state.xRot = 0.0F;

        final float capturedYaw = yaw;
        final boolean wantGround = renderGround;
        final boolean wantBed = tracker.isCurrentAnimation("sleep");
        final boolean wantHorse = tracker.isCurrentAnimation("ride");
        final boolean wantPig = tracker.isCurrentAnimation("ride_pig");
        final boolean wantBoat = tracker.isCurrentAnimation("boat");
        PreviewEntityRegistry.SceneryRenderer scenery = null;
        if (wantGround || wantBed || wantHorse || wantPig || wantBoat) {
            scenery = (poseStack, bufferSource, packedLight) -> {
                if (wantHorse) {
                    renderVehicleScenery(poseStack, bufferSource, packedLight, capturedYaw, partialTick, entity, EntityType.HORSE);
                } else if (wantPig) {
                    renderVehicleScenery(poseStack, bufferSource, packedLight, capturedYaw, partialTick, entity, EntityType.PIG);
                } else if (wantBoat) {
                    renderVehicleScenery(poseStack, bufferSource, packedLight, capturedYaw, partialTick, entity, EntityType.OAK_BOAT);
                }
                if (wantBed) {
                    renderBedScenery(poseStack, bufferSource, packedLight, capturedYaw);
                }
                if (wantGround) {
                    renderGroundScenery(poseStack, bufferSource, packedLight, capturedYaw);
                }
            };
        }

        final float characterYOffset = poseYOffset;
        PreviewEntityRegistry.ModelPoseTransform characterPose = null;
        if (wantBed) {
            // A bed is 9/16 blocks high. Move the sleeper onto its mattress;
            // the half-block shift uses the same yaw convention as the bed.
            double angle = Math.toRadians(capturedYaw);
            double sleeperX = 0.5 * Math.sin(angle);
            double sleeperZ = 0.5 * Math.cos(angle);
            characterPose = poseStack -> poseStack.translate(sleeperX, 9.0 / 16.0, sleeperZ);
        } else if (characterYOffset != 0.0f) {
            characterPose = poseStack -> poseStack.translate(0.0f, characterYOffset, 0.0f);
        }
        PreviewEntityRegistry.register(state, animatable, scenery, null, characterPose);

        Quaternionf cameraTilt = new Quaternionf().rotateX((float) Math.toRadians(-10.0 + pitch));
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraTilt);

        float entityScale = entity.getScale();
        float submitScale = zoom / entityScale;
        float rectCenterX = (x0 + x1) / 2.0F;
        float rectCenterY = (y0 + y1) / 2.0F;
        float translationX = (anchorX - rectCenterX) / submitScale;
        // GUI translation precedes Rz(pi)*Rx(tilt). Pose offsets instead run
        // after that rotation, on the character only, via the registry above.
        float translationY = (anchorY - rectCenterY) / submitScale + 0.8F;
        if (wantBed) {
            state.bodyRot = yaw - 90;
        }

        Vector3f translation = new Vector3f(translationX, translationY, 0.0F);

        guiGraphics.enableScissor(x0, y0, x1, y1);
        guiGraphics.entity(state, submitScale, translation, rotation, cameraTilt, x0, y0, x1, y1);
        guiGraphics.disableScissor();

        if (poseChanged) {
            entity.setPose(oldPose);
        }
    }

    private static void submitBlockScenery(net.minecraft.world.level.block.state.BlockState block,
                                           PoseStack poseStack, DeferredBufferSource buffers,
                                           int light, int overlay) {
        net.minecraft.client.renderer.block.BlockModelRenderState state = new net.minecraft.client.renderer.block.BlockModelRenderState();
        RendererManager.getBlockModelResolver().update(state, block,
                net.minecraft.client.renderer.block.model.BlockDisplayContext.create());
        state.submit(poseStack, buffers.collector(), light, overlay, 0);
    }

    public static void submitSettingsPreview(GuiGraphicsExtractor graphics, LivingAnimatable<?> animatable,
                                            int x0, int y0, int x1, int y1,
                                            float anchorX, float anchorY, float zoom,
                                            float pitch, float yaw, float partialTick) {
        if (!(animatable instanceof CustomPlayerEntity playerModel)) {
            throw new IllegalArgumentException("Settings preview requires a player model");
        }
        Player entity = playerModel.getEntity();
        if (entity == null) {
            return;
        }
        AvatarRenderState state = new AvatarRenderState();
        RendererManager.getPlayerRenderer().extractRenderState(entity, state, partialTick);
        state.lightCoords = net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
        state.bodyRot = -yaw;
        state.yRot = Mth.wrapDegrees(180.0f + yaw);
        state.xRot = 0;
        PreviewEntityRegistry.register(state, playerModel);
        Quaternionf cameraTilt = new Quaternionf().rotateX((float) Math.toRadians(-10.0f + pitch));
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraTilt);
        float submitScale = zoom / entity.getScale();
        Vector3f translation = new Vector3f((anchorX - (x0 + x1) / 2.0f) / submitScale,
                (anchorY - (y0 + y1) / 2.0f) / submitScale + 0.8f, 0);
        graphics.enableScissor(x0, y0, x1, y1);
        graphics.entity(state, submitScale, translation, rotation, cameraTilt, x0, y0, x1, y1);
        graphics.disableScissor();
    }

    private static void renderGroundScenery(PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5d, -1.0d, -2.5d);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0.0f, 0.0f, 1.0f);
                submitBlockScenery(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.translate(1.0f, 0.0f, -3.0f);
        }
        poseStack.translate(-1.0f, 1.0f, 1.0f);
        submitBlockScenery(Blocks.SHORT_GRASS.defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.translate(0.0f, 0.0f, 1.0f);
        submitBlockScenery(Blocks.RED_TULIP.defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderBedScenery(PoseStack poseStack, DeferredBufferSource bufferSource, int packedLight, float yaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0f));
        poseStack.translate(-0.5d, 0.0d, 0.5d);
        submitBlockScenery(Blocks.RED_BED.defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderVehicleScenery(
            PoseStack poseStack,
            DeferredBufferSource bufferSource,
            int packedLight,
            float yaw,
            float partialTick,
            LivingEntity rider,
            EntityType<? extends Entity> vehicleType
    ) {
        if (rider.level() == null) {
            return;
        }
        Entity vehicle;
        try {
            vehicle = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(vehicleType), () -> {
                Entity preview = vehicleType.create(rider.level(), EntitySpawnReason.LOAD);
                if (preview != null) {
                    preview.setId(com.elfmcys.yesstevemodel.util.PreviewEntityIds.next());
                }
                return preview;
            });
        } catch (java.util.concurrent.ExecutionException e) {
            return;
        }
        if (vehicle == null) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // The vanilla renderer's origin is the vehicle's feet/keel. This scene
        // has its ground plane at y=0; passenger attachment offsets apply to
        // the rider, never to the independently displayed vehicle.
        net.minecraft.client.renderer.entity.state.EntityRenderState state = dispatcher.extractEntity(vehicle, partialTick);
        state.lightCoords = packedLight;
        dispatcher.submit(state, RenderContext.camera(), 0, 0, 0, poseStack, bufferSource.collector());
        poseStack.popPose();
    }
}
