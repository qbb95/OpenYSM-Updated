package rip.ysm.compat.realcamera;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import rip.ysm.compat.non.NoNActorProbe;

/**
 * 绑定模式第一人称下、触发动画期间的滚轮推拉距离。
 * <p>
 * 距离以方块为单位累积，正值 = 镜头沿脸朝前探，负值 = 向后退。真正的平移在
 * {@code CustomPlayerRenderer#computeBindPose} 里沿最终 forward 施加 —— 只移动位置，
 * 不改 forward/up，所以"脸平面不变"。
 * <p>
 * 门控是"当前确实在播触发出来的动画"：要么 {@code player.cap} 控制器非 IDLE
 * （openysm 自己的动画轮盘 / 同步过来的触发动画），要么 {@code player.main} 在播模型
 * 自带的 runtime 动画（needsofnature 之类由 bridge 注入的动画走的是这条）。普通的
 * idle/walk/run 是 openysm 内建状态，只在轮盘播放期间叠加在 cap 上，所以不会误触发。
 */
public final class ScrollDolly {

    private static final float NONE = 0.0f;

    private static final String CAP_CONTROLLER_KEY = "player.cap";

    private static final String MAIN_CONTROLLER_KEY = "player.main";

    /**
     * 桥接（ysm-non-bridge）把 NoN 动画注入 {@code mainAnimations} 时用的键前缀：
     * {@code "non_<resource>_<key>"}。这是这里唯一能区分"NoN 触发动画"和"模型自带状态动画"
     * （idle / walk / run / fly / swim …）的信号。
     */
    private static final String NON_ANIMATION_MARKER = "non_";

    private static float distance;

    private ScrollDolly() {
    }

    /**
     * 每客户端 tick 调一次。只要推拉窗口（绑定第一人称 + 动画播放）还开着，就保留当前
     * 距离；窗口一关（动画结束 / 退出第一人称 / 开关被关掉）立即回中 —— 否则动画播完后
     * 镜头会一直停在推拉后的位置不回去，或下次进入第一人称镜头凭空偏出去。
     * <p>
     * 注意：这里<b>不能</b>因为"这一 tick 没有新的滚轮输入"就回中。滚轮是离散事件
     * （20 tick/s 远慢于渲染帧率），若按 tick 无输入即清零，滚一格后约 1 tick 距离就被
     * 抹掉，表现为镜头前探一下又被拉回原地。距离应由 {@link #isDollyAllowed()} 决定去留。
     */
    public static void tickEnd() {
        if (distance == 0.0f) {
            return;
        }
        if (!isDollyWindowOpen()) {
            distance = NONE;
        }
    }

    /**
     * @return true 表示这次滚轮已被推拉消费，调用方应取消原版滚轮（否则会切换快捷栏）
     */
    public static boolean onScroll(double yOffset) {
        if (yOffset == 0.0d || !isDollyWindowOpen()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        // 只在"没有 GUI 打开"时接管滚轮：否则在背包 / 暂停菜单里滚列表也会被吃掉。
        // 注意这条只影响"能不能滚出新距离"，不能拿去判"要不要清空已有距离"（见 tickEnd）。
        if (client.screen != null || client.getOverlay() != null) {
            return false;
        }
        double max = GeneralConfig.REALCAMERA_SCROLL_DOLLY_MAX.get();
        double step = GeneralConfig.REALCAMERA_SCROLL_DOLLY_STEP.get();
        // 滚轮上滚给正 yOffset —— 上滚 = 向前探
        distance = Mth.clamp((float) (distance + yOffset * step), (float) -max, (float) max);
        return true;
    }

    /** 当前推拉距离（方块），未参与时返回 0。 */
    public static float getDistance() {
        return distance;
    }

    /** 动画结束 / 退出第一人称时回中，避免下次进入第一人称镜头凭空偏出去。 */
    public static void reset() {
        distance = NONE;
    }

    /**
     * 推拉窗口是否还开着：绑定模式开关打开 + 模型在用 + 触发动画在播。
     * <p>
     * <b>故意不看 GUI / 暂停</b>：`ESC` 暂停会开 Screen、`isPaused()` 期间 RealCamera 自己也会把自己
     * 置为 inactive，但那是"暂停"，不是"推拉该结束"。若在这里把窗口判死，一开暂停菜单距离就被清零，
     * 用户看到"滚出来的镜头突然弹回原地"（2026-11-12 报的问题）。
     * 是否接管滚轮输入另由 {@link #onScroll} 的 screen 判定负责。
     */
    private static boolean isDollyWindowOpen() {
        if (!GeneralConfig.REALCAMERA_SCROLL_DOLLY.get().booleanValue() || !RealCameraBinding.isLoaded()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        CustomPlayerEntity animatable = PlayerCapability.get(player).orElse(null);
        return animatable != null && animatable.isModelActive() && isTriggeredAnimationPlaying(animatable);
    }

    private static boolean isTriggeredAnimationPlaying(CustomPlayerEntity animatable) {
        if (animatable.isModelSwitching()
                && animatable.getAnimationState(CAP_CONTROLLER_KEY) != AnimationState.IDLE) {
            return true;
        }
        AnimationData data = animatable.getAnimationData();
        if (data == null) {
            return false;
        }
        IAnimationController<?> main = data.getAnimationControllerByName(MAIN_CONTROLLER_KEY);
        if (main == null) {
            return false;
        }
        String current = main.getCurrentAnimation();
        if (current == null || current.isEmpty()) {
            return false;
        }
        // 注意：不能用"名字不是 idle / Coded"来代理"触发动画"。模型自带状态机在 fly / swim / ride /
        // 部分模型的 walk 下同样返回具体名字（PredicateBasedController 返回 "Coded -> <名>"），
        // 那样运动状态里滚轮会被推拉吃掉、换不了物品栏（2026-11-12）。
        //
        // 桥接注入的 NoN 动画在 controller 上暴露的是**原始 key**（Animation.animationName，
        // 如 "p1_player"），名字里并没有 "non_" 前缀；而名字又可能被包成 "Coded -> xxx"。
        // 所以这里不靠名字判断，直接问 NoN 运行时"这个玩家是不是正在演动画"。
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null && NoNActorProbe.isActorActive(localPlayer.getUUID())) {
            return true;
        }
        // 兜底：若某些路径上报的是命名空间键（"non_<resource>_<key>"），也认。
        return current.contains(NON_ANIMATION_MARKER);
    }
}
