package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerArmorLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerParrotLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import rip.ysm.compat.realcamera.CameraPassthrough;
import rip.ysm.compat.realcamera.ScrollDolly;
import rip.ysm.compat.realcamera.FirstPersonFaceAnchor;
import rip.ysm.compat.non.NoNAlignmentProbe;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<Player, CustomPlayerEntity, AvatarRenderState> {

    private Identifier currentTexture;
    private Player currentPlayer;

    public CustomPlayerRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayerRenderer(new CustomPlayerItemInHandLayer(Minecraft.getInstance().gameRenderer.itemInHandRenderer));
        addLayerRenderer(new CustomPlayerElytraLayer(context));
        addLayerRenderer(new CustomPlayerParrotLayer(context));
        addLayerRenderer(new CustomPlayerArmorLayer(context));
    }

    public void render(Player player, AvatarRenderState renderState, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerCapability capability;
        if (SWarfareCompat.isPlayerAiming(player) || (capability = PlayerCapability.get(player).orElse(null)) == null) {
            return;
        }
        this.currentPlayer = player;
        capability.tickModel();
        SpecialPlayerRenderEvent renderEvent = new SpecialPlayerRenderEvent(player, capability, capability.getModelId());
        this.currentTexture = renderEvent.getTextureLocation();
        if (SpecialPlayerRenderEvent.post(renderEvent).isFalse()) {
            return;
        }
        renderEntityWithTexture(capability, renderState, renderEvent.getTextureLocation(), entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldShowName(Player entity, double distance) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        double dDistanceToSqr = distance;
        float nameRenderDistance = entity.isDiscrete() ? 32.0f : 64.0f;
        if (dDistanceToSqr >= nameRenderDistance * nameRenderDistance || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null) {
            return false;
        }
        boolean isVisible = !entity.isInvisibleTo(localPlayer);
        if (entity != localPlayer) {
            Team team = entity.getTeam();
            Team team2 = localPlayer.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case ALWAYS:
                        return isVisible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team2 == null ? isVisible : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || isVisible);
                    case HIDE_FOR_OWN_TEAM:
                        return team2 == null ? isVisible : !team.isAlliedTo(team2) && isVisible;
                    default:
                        throw new IncompatibleClassChangeError();
                }
            }
        }
        return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && isVisible && !entity.isVehicle();
    }

    @Override
    public @NotNull AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @NotNull
    public Identifier getTextureLocation(Player player) {
        return this.currentTexture == null ? PlayerCapability.get(player).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation()) : this.currentTexture;
    }

    @Override
    public net.minecraft.resources.Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        if (this.currentTexture != null) {
            return this.currentTexture;
        }

        Player player = this.currentPlayer;
        if (player == null) {
            player = Minecraft.getInstance().player;
        }
        if (player != null) {
            return PlayerCapability.get(player)
                    .map(PlayerCapability::getTextureLocation)
                    .orElse(MissingTextureAtlasSprite.getLocation());
        }
        return MissingTextureAtlasSprite.getLocation();
    }

    // TODO: port to 1.21.4
//    @Override
//    protected void renderNameTag(AvatarRenderState entityRenderState, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
//        Scoreboard scoreboard;
//        Objective displayObjective;
//        if (PlayerPreviewEntity.isPreviewPlayer(player)) {
//            return;
//        }
//        double dDistanceToSqr = this.entityRenderDispatcher.distanceToSqr(player);
//        poseStack.pushPose();
//        if (dDistanceToSqr < 100.0d && (displayObjective = (scoreboard = player.getScoreboard()).getDisplayObjective(DisplaySlot.BELOW_NAME)) != null) {
//            super.renderNameTag(entityRenderState, Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player, displayObjective).get())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i);
//            poseStack.translate(0.0d, 0.25875d, 0.0d);
//        }
//        super.renderNameTag(entityRenderState, component, poseStack, multiBufferSource, i);
//        poseStack.popPose();
//    }

    @Override
    public void setupRotations(Player player, AvatarRenderState state, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        super.setupRotations(player, state, poseStack, ageInTicks, rotationYaw, partialTicks, scale);
        Entity vehicle = player.getVehicle();
        if (TouhouLittleMaidCompat.isSimplePlanesEntity(vehicle) || TouhouLittleMaidCompat.isImmersiveAircraftEntity(vehicle)) {
            poseStack.translate(0.0d, 0.5d, 0.0d);
        }
    }

    /**
     * 在当前帧不渲染的情况下复算出 YSM 模型头部定位器的世界姿态，供 RealCamera 绑定模式取
     * 位置 / forward / upward。绑定函数在相机更新阶段调用，拿不到渲染中的 PoseStack，
     * 只能按 {@code renderEntityWithTexture} 的同一套变换重建一遍。
     *
     * @return 成功算出可用的绑定姿态返回 true
     */
    public boolean computeBindPose(Player player, AvatarRenderState state, float partialTicks,
                                   Vector3f forwardOut, Vector3f upwardOut, Vector3f positionOut) {
        PlayerCapability capability = PlayerCapability.get(player).orElse(null);
        if (capability == null) {
            return false;
        }
        if (!capability.isModelActive()) {
            return false;
        }
        AnimatedGeoModel model = capability.getCurrentModel();
        if (model == null) {
            return false;
        }
        List<IBone> locator = model.headBones();
        boolean headTrackingApplied = false;
        float restoreRotX = 0.0f;
        float restoreRotY = 0.0f;
        IBone trackedHead = null;
        if (locator.isEmpty()) {
            IBone fallback = model.allHeadBone() != null ? model.allHeadBone() : model.viewLocatorBone();
            if (fallback == null) {
                return false;
            }
            locator = List.of(fallback);
        } else {
            // applyHeadTracking 把鼠标 pitch/yaw 直接加到了最后一根头部骨骼的 rotationX/Y 上。
            // 这里要的是"纯动画"的脸朝向，交出相机后再把鼠标作为增量叠上去，否则会重复计算。
            trackedHead = locator.get(locator.size() - 1);
            float rawHeadPitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
            float headPitch = -rawHeadPitch;
            float lerpHead = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot);
            float lerpBody = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
            float netHeadYaw = -Mth.clamp(Mth.wrapDegrees(lerpHead - lerpBody), -85.0f, 85.0f);
            restoreRotX = trackedHead.getRotationX();
            restoreRotY = trackedHead.getRotationY();
            trackedHead.setRotationX(restoreRotX - (float) Math.toRadians(headPitch));
            trackedHead.setRotationY(restoreRotY - (float) Math.toRadians(netHeadYaw));
            headTrackingApplied = true;
        }
        PoseStack pose = new PoseStack();
        Vec3 renderOffset = getRenderOffset(state);
        pose.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        Direction bed = player.getPose() == Pose.SLEEPING ? player.getBedOrientation() : null;
        if (bed != null) {
            float eyeHeight = player.getEyeHeight(Pose.STANDING) - 0.1f;
            pose.translate(-bed.getStepX() * eyeHeight, 0.0f, -bed.getStepZ() * eyeHeight);
        }
        setupRotations(player, state, pose, state.ageInTicks, state.bodyRot, partialTicks, 1.0f);
        // NoN 动画期间 bridge-mod（ysm_non_bridge）会在 setupRotations 的尾部把整套模型平移一段
        // 对齐量：把 NoN 动画自己的锚点对齐到 YSM 模型的锚点，量级可达零点几格。相机姿态是在
        // 相机更新阶段独立重建的，拿不到那条渲染注入，必须在这里补上同一段平移 —— 否则锚点会
        // 停在"未平移"的头部，动画里看着比画面中的头远一截（站立时偏移为 0，看不出差别）。
        Vector3f alignmentOffset = NoNAlignmentProbe.offsetOf(player, player.getScale());
        if (alignmentOffset != null) {
            pose.translate(alignmentOffset.x, alignmentOffset.y, alignmentOffset.z);
        }

        pose.translate(0.0f, 0.01f, 0.0f);
        // 与 IGeoRenderer#renderEarly 的 INITIAL 分支保持一致（那里的局部变量名是反的）
        pose.scale(capability.getHeightScale(), capability.getWidthScale(), capability.getHeightScale());
        RenderUtils.prepMatrixForLocator(pose, locator);
        // 定位器原点在颈 / 头骨枢轴（YSM 空间 Y 向上、面朝 -Z）。取眼睛位置：上移 4 像素、
        // 略贴前额，而不是脑袋正中心 —— 前者低头时能看到身体，后者会被头部网格糊住。
        Vector4f anchor = pose.last().pose().transform(new Vector4f(0.0f, 0.25f, -0.15f, 1.0f));
        positionOut.set(anchor.x, anchor.y, anchor.z);
        // 动画驱动的脸部基（此时 pose 里已经剔除了鼠标分量）。YSM 骨骼空间 Y 向上、正面朝 -Z，
        // 因此 forward 取 -第 3 列、up 取第 2 列。
        Matrix3f normal = pose.last().normal();
        Vector3f animForward = normal.getColumn(2, new Vector3f()).negate();
        Vector3f animUp = normal.getColumn(1, new Vector3f());
        // 头骨可能被第一人称隐藏缩到 0，此时列向量退化；先判长度再归一化，避免 NaN
        if (animForward.lengthSquared() <= 1.0e-8f || animUp.lengthSquared() <= 1.0e-8f) {
            return false;
        }
        animForward.normalize();
        // normal 是逆转置矩阵，非等比缩放下两列未必严格正交，先把 up 正交于 forward
        animUp.fma(-animUp.dot(animForward), animForward);
        if (animUp.lengthSquared() <= 1.0e-8f) {
            return false;
        }
        animUp.normalize();
        if (headTrackingApplied) {
            // 还原被临时剔除的鼠标分量，绝不能改动真实渲染姿势
            trackedHead.setRotationX(restoreRotX);
            trackedHead.setRotationY(restoreRotY);
        }
        // 鼠标增量必须落在"世界"轴系里叠加。旧实现是把 rotationY/rotationX 直接左乘进头部
        // 局部坐标系（pose.mulPose），头一旦低头 / 侧躺，头骨自身的"上轴"就不再是世界的竖直轴，
        // 水平甩鼠标变成了绕头骨上轴（≈ 水平方向）的旋转，也就是侧滚 + 意外改俯仰，镜头不跟手。
        // NoN 动画里人物贴地（爬 / 躺 / 翻滚）时头骨几乎水平，这个偏差最大 —— 正是"镜头贴近
        // 地面时方向转动有问题"的来源。
        //
        // 鼠标分量单独在最后叠加，有两种模式（GeneralConfig.REALCAMERA_VIEW_MODE）：
        //   MOUSE_ABSOLUTE：镜头朝向完全由鼠标决定，动画只贡献身体位置与侧滚 roll；
        //     任何姿势下上下都能看满 ±90°（脸朝下时抬头也能到正上方）。
        //   FACE_RELATIVE：以动画脸朝向为基准，鼠标俯仰相对脸叠加；侧躺 / 翻滚时镜头跟着脸倾斜，
        //     但脸朝下 45° 时向上最多只到 +45°（几何上被脸本身吃掉一部分）。
        float rawHeadPitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float lerpHead = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot);
        float lerpBody = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        float netHeadYaw = Mth.clamp(Mth.wrapDegrees(lerpHead - lerpBody), -85.0f, 85.0f);
        GeneralConfig.RealCameraViewMode viewMode = GeneralConfig.REALCAMERA_VIEW_MODE.get();
        if (viewMode == GeneralConfig.RealCameraViewMode.MOUSE_ABSOLUTE) {
            composeMouseAbsolute(player, animForward, animUp, lerpBody, netHeadYaw, rawHeadPitch,
                    forwardOut, upwardOut);
        } else {
            composeMouseFaceRelative(animForward, animUp, netHeadYaw, rawHeadPitch, forwardOut, upwardOut);
        }
        if (forwardOut.lengthSquared() <= 1.0e-8f) {
            return false;
        }
        if (upwardOut.lengthSquared() <= 1.0e-8f) {
            return false;
        }
        // 滚轮推拉：只沿最终 forward 平移位置，forward/up 原样交出去，
        // 所以镜头朝向与"脸平面"完全不变，只是远近变化。
        float dolly = ScrollDolly.getDistance();
        float dollyClamped = dolly;
        if (dolly != 0.0f) {
            if (CameraPassthrough.isRequested()) {
                // 外部 mod（non_animation_perspective）判定"绑定第一人称 + NoN 动画中"时，
                // 要求镜头可以穿透任何方块：两道裁剪都跳过，距离原样交出。
                dollyClamped = dolly;
            } else {
                // 推拉距离要按方块碰撞裁剪：否则在贴墙 / 贴栅栏的姿势里向前推，会把镜头直接顶穿
                // 栅栏钻到身体里。这里从"未推拉的头部锚点"做起点的绝对裁剪。
                dollyClamped = clampDollyAgainstBlocks(player, partialTicks,
                        new Vector3f(positionOut), forwardOut, dolly);
                // 第二道：按 RealCamera 自己那条"眼点 → 相机"的射线再收一次。见方法注释 ——
                // 少了这一道，推拉会把目标点推到这条线段穿过栅栏的位置，RealCamera 的
                // clipToSpace 就会把镜头从头部一侧猛地拽回身体一侧（"滚一段突然到另一边"）。
                dollyClamped = clampDollyAgainstEyeRay(player, partialTicks,
                        new Vector3f(positionOut), forwardOut, dollyClamped);
            }
            positionOut.fma(dollyClamped, forwardOut);
        }
        return true;
    }

    /**
     * 把推拉位移按方块碰撞裁剪，返回裁剪后仍可用的推拉距离（保留符号）。
     * <p>
     * 世界坐标起点 = 实体插值位置 + 头部锚点（{@code positionOut} 是实体相对量，RealCamera
     * 用 {@code smoothedCamera.position + entityPos} 还原世界坐标，这里做同样的还原）。
     * 从起点沿 forward 打到"推拉后的目标点"，命中方块就把距离收到碰撞面之前留一点余量，
     * 表现等价于原版第三人称相机的贴墙收缩，只是这里裁的是滚轮推拉的增量。
     * <p>
     * 单条中心射线会从栅栏 / 栏杆这类"碰撞体有缝隙"的方块中间穿过去（眼睛正对缝隙时），
     * 所以沿镜头小孔径打一圈偏移射线（世界轴 ±r 的 8 个角 + 中心），取最近命中，
     * 与 RealCamera / 原版 {@code clipToSpace} 的多点采样一致。
     */
    private static float clampDollyAgainstBlocks(Player player, float partialTicks,
                                                 Vector3f anchorLocal, Vector3f forward, float dolly) {
        if (Math.abs(dolly) < 1.0e-4f) {
            return dolly;
        }
        Level level = player.level();
        if (level == null) {
            return dolly;
        }
        Vec3 worldForward = new Vec3(forward.x, forward.y, forward.z);
        if (worldForward.length() < 1.0e-4) {
            return dolly;
        }
        Vec3 anchorWorld = player.getPosition(partialTicks)
                .add(anchorLocal.x, anchorLocal.y, anchorLocal.z);
        Vec3 targetWorld = anchorWorld.add(worldForward.scale(dolly));
        double total = Math.abs(dolly);

        double nearest = total;
        float r = 0.15f;
        // 中心 + 8 个世界轴角，共 9 条；中心保证正对面命中，角射线堵住缝隙漏光。
        int[] signs = {-1, 0, 1};
        for (int sx : signs) {
            for (int sy : signs) {
                for (int sz : signs) {
                    double ox = sx * r;
                    double oy = sy * r;
                    double oz = sz * r;
                    Vec3 from = anchorWorld.add(ox, oy, oz);
                    Vec3 to = targetWorld.add(ox, oy, oz);
                    BlockHitResult hit = level.clip(new ClipContext(
                            from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                    if (hit.getType() == HitResult.Type.MISS) {
                        continue;
                    }
                    double allowed = hit.getLocation().subtract(from).length();
                    if (allowed < nearest) {
                        nearest = allowed;
                    }
                }
            }
        }
        if (nearest >= total) {
            return dolly;
        }
        double margin = 0.05;
        double clamped = Math.max(0.0, nearest - margin);
        return (float) Math.copySign(clamped, dolly);
    }

    /**
     * 第二道裁剪：按 <b>RealCamera 自己的</b> {@code clipToSpace} 射线把推拉距离收住。
     * <p>
     * RealCamera 0.7.9 绑定模式（{@code MixinCamera#setupCamera} RETURN）在设完相机位置后 **总会**
     * 调一次 {@code clipToSpace}：起点是<b>玩家眼点</b>（{@code x/z} 取眼点、{@code y} 取目标位置
     * 再夹进实体包围盒），终点是绑定结果给的位置；8 条 VISUAL 射线，**任何一条命中就把相机**
     * 按 {@code sqrt(dist²/total²)} 拉回到起点那一侧**。
     * <p>
     * 我们上一道裁剪是从<b>头部锚点</b>往前的，看不到"眼点 → 相机"这条线段上的遮挡。而趴 / 俯身类
     * 动画里头部锚点会离开眼点一两格（甚至越过栅栏），推拉又让目标点沿 forward 偏移，于是滚到
     * 某个距离后这条线段开始穿过栅栏，RealCamera 把镜头从"头部这侧"一下拽到"身体那侧"。
     * <p>
     * 这里用与 RealCamera 相同的起点、孔径 / 口径预判：只要线段会被裁，就把推拉距离二分收到
     * "线段干净"的最大值。收到的结果保证 RealCamera 那一步恒为 MISS，于是不会再跳。
     * 位置沿 {@code anchor + d·forward} 单射线变化，小距离干净则更小距离也干净，二分成立。
     *
     * @return 裁剪后仍可用的推拉距离（保留符号）
     */
    private static float clampDollyAgainstEyeRay(Player player, float partialTicks,
                                                 Vector3f anchorLocal, Vector3f forward, float dolly) {
        if (Math.abs(dolly) < 1.0e-4f) {
            return dolly;
        }
        Level level = player.level();
        if (level == null) {
            return dolly;
        }
        Vec3 worldForward = new Vec3(forward.x, forward.y, forward.z);
        if (worldForward.length() < 1.0e-4) {
            return dolly;
        }
        Vec3 anchorWorld = player.getPosition(partialTicks)
                .add(anchorLocal.x, anchorLocal.y, anchorLocal.z);
        Vec3 eye = player.getEyePosition(partialTicks);
        double sign = Math.signum(dolly);
        double hi = Math.abs(dolly);
        if (isEyeSegmentClear(player, level, eye, anchorWorld, worldForward, hi)) {
            return dolly;
        }
        double lo = 0.0;
        for (int i = 0; i < 8; i++) {
            double mid = 0.5 * (lo + hi);
            if (isEyeSegmentClear(player, level, eye, anchorWorld, worldForward, mid)) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (float) (sign * lo);
    }

    /**
     * 复刻 RealCamera {@code clipToSpace} 的 8 条射线（起点取眼点 x/z + 目标 y 夹进包围盒），
     * 返回是否全部 MISS。孔径随 FOV 变化，与 RealCamera 用同一公式。
     */
    private static boolean isEyeSegmentClear(Player player, Level level, Vec3 eye,
                                             Vec3 anchorWorld, Vec3 forward, double distance) {
        Vec3 desired = anchorWorld.add(forward.scale(distance));
        AABB box = player.getBoundingBox();
        double startY = Mth.clamp(desired.y, box.minY + 0.1, box.maxY - 0.1);
        Vec3 start = new Vec3(eye.x, startY, eye.z);
        if (desired.distanceToSqr(start) < 1.0e-8) {
            return true;
        }
        double fov = Minecraft.getInstance().options.fov().get();
        float depth = (float) (0.05 + fov * (1.0e-4 + 5.0e-6 * fov));
        for (int i = 0; i < 8; i++) {
            float ox = depth * ((i & 1) * 2 - 1);
            float oy = depth * (((i >> 1) & 1) * 2 - 1);
            float oz = depth * (((i >> 2) & 1) * 2 - 1);
            BlockHitResult hit = level.clip(new ClipContext(
                    start.add(ox, oy, oz), desired.add(ox, oy, oz),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    /**
     * 模式 A：镜头朝向完全由鼠标决定，动画只贡献身体位置与侧滚 roll。
     * animForward/animUp 只读（不改动），结果写入 forwardOut/upwardOut。
     * <p>
     * 鼠标走"画面方向"：水平绕当前画面竖轴(up)、俯仰绕画面横轴(up × forward)。竖直站立时
     * up 就是世界 Y，与旧实现完全等价；侧躺 / 翻身时画面竖轴是横的，于是"鼠标往下"等于往
     * 画面下方（身体一侧）转，而不是往地面扎。
     */
    private static void composeMouseAbsolute(Player player, Vector3f animForward, Vector3f animUp,
                                             float bodyYawDeg, float netHeadYaw, float rawHeadPitch,
                                             Vector3f forwardOut, Vector3f upwardOut) {
        // 动画脸的侧滚：绕"动画 forward"把动画 up 从竖直参考转过的角度
        float roll = rollAbout(animForward, animUp);
        // 水平基准取玩家真实身体朝向（lerpBody 的 MC 视线向量），而不是动画头骨的"脸面轴水平投影"：
        // 头骨在蹲 / 摔 / 翻滚这类姿势里几乎竖直向下，其脸面轴的水平投影又短又会随挣扎摆动，
        // 而滚轮推拉是沿 forwardOut 平移位置的，forward 一旦跟着头骨抖，向前滚就会把镜头横拖到身体一侧。
        float bodyYawRad = (float) Math.toRadians(bodyYawDeg);
        Vector3f heading = new Vector3f((float) -Math.sin(bodyYawRad), 0.0f,
                (float) Math.cos(bodyYawRad));
        float bodyYaw = (float) Math.atan2(-heading.x, -heading.z);
        Quaternionf bodyYawQuat = new Quaternionf().rotationY(bodyYaw);
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(bodyYawQuat);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(bodyYawQuat);
        // 动画侧滚先叠在基准朝向上：之后鼠标的上 / 下、左 / 右才都是画面方向
        forward.rotateAxis(roll, forward.x, forward.y, forward.z);
        up.rotateAxis(roll, forward.x, forward.y, forward.z);
        // 进 / 出 NoN 动画时的一次性帧重定向（见 FirstPersonFaceAnchor）。它加在"基准朝向"上、
        // 位于鼠标之前：这样锚定之后鼠标输入仍以画面方向为参照，不会被这层偏移带歪。
        Quaternionf anchor = FirstPersonFaceAnchor.anchor(player, animForward, animUp, forward, up);
        if (anchor != null) {
            forward.rotate(anchor);
            up.rotate(anchor);
        }
        // 鼠标水平：绕画面竖轴 up（竖直站立时即世界 Y，与旧实现一致）
        Quaternionf yawQuat = new Quaternionf().rotateAxis((float) Math.toRadians(-netHeadYaw),
                up.x, up.y, up.z);
        forward.rotate(yawQuat);
        up.rotate(yawQuat);
        // 鼠标俯仰：绕画面横轴 up × forward
        Vector3f right = up.cross(forward, new Vector3f());
        if (right.lengthSquared() < 1.0e-6f) {
            right.set(1.0f, 0.0f, 0.0f);
        }
        right.normalize();
        Quaternionf pitchQuat = new Quaternionf().rotateAxis((float) Math.toRadians(rawHeadPitch), right);
        forward.rotate(pitchQuat);
        up.rotate(pitchQuat);
        up.fma(-up.dot(forward), forward);
        if (up.lengthSquared() < 1.0e-8f) {
            up.set(0.0f, 1.0f, 0.0f);
        }
        up.normalize();
        forwardOut.set(forward);
        upwardOut.set(up);
    }

    /**
     * 模式 B：镜头以动画脸朝向为基准，鼠标 yaw 绕世界 Y、pitch 绕头骨右轴叠加。
     */
    private static void composeMouseFaceRelative(Vector3f animForward, Vector3f animUp,
                                                 float netHeadYaw, float rawHeadPitch,
                                                 Vector3f forwardOut, Vector3f upwardOut) {
        Quaternionf yawQuat = new Quaternionf().rotationY((float) Math.toRadians(-netHeadYaw));
        animForward.rotate(yawQuat);
        animUp.rotate(yawQuat);
        Vector3f pitchAxis = animUp.cross(animForward, new Vector3f());
        if (pitchAxis.lengthSquared() < 1.0e-6f) {
            pitchAxis.set(1.0f, 0.0f, 0.0f);
        }
        pitchAxis.normalize();
        Quaternionf pitchQuat = new Quaternionf()
                .rotateAxis((float) Math.toRadians(rawHeadPitch), pitchAxis);
        forwardOut.set(animForward).rotate(pitchQuat);
        upwardOut.set(animUp).rotate(pitchQuat);
    }

    /**
     * 求绕 forward 轴、把 up 从"世界竖直投影参考"转过的侧滚角（弧度）。
     * forward 接近竖直时参考退化，返回 0。
     */
    private static float rollAbout(Vector3f forward, Vector3f up) {
        Vector3f ref = new Vector3f(0.0f, 1.0f, 0.0f);
        ref.fma(-ref.dot(forward), forward);
        if (ref.lengthSquared() < 1.0e-8f) {
            return 0.0f;
        }
        ref.normalize();
        float c = Math.max(-1.0f, Math.min(1.0f, ref.dot(up)));
        Vector3f cross = ref.cross(up, new Vector3f());
        float s = cross.dot(forward);
        return (float) Math.atan2(s, c);
    }

}
