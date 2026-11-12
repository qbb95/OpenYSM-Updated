package rip.ysm.compat.non;

import dev.architectury.platform.Platform;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.lang.reflect.Method;

/**
 * 读取 bridge-mod 为当前 NoN 动画算出的"模型整体平移"对齐量，供 RealCamera 绑定重建姿态时补齐。
 * <p>
 * 为什么需要它：NoN 动画期间 {@code ysm_non_bridge} 会在 openysm 的
 * {@code setupRotations} 尾部（TAIL）对整套模型做一次平移 —— 把 NoN 动画的原版锚点对齐到 YSM
 * 模型的锚点，量级可达零点几格。而 RealCamera 绑定函数跑在<b>相机更新阶段</b>，那时没有那条
 * 渲染注入，{@link com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer#computeBindPose}
 * 只能自己重建变换，于是锚点停在"未平移"的头部位置：动画里镜头看着比画面中的头远一截，
 * 站立时因为偏移为 0 所以看不出差别。
 * <p>
 * 纯反射、可失败：bridge 缺失 / 无活动 NoN 动画 / 类名变动时返回 {@code null}，调用方照旧不偏移。
 */
public final class NoNAlignmentProbe {

    private static final boolean BRIDGE_LOADED = Platform.isModLoaded("ysm_non_bridge");

    private static final String BRIDGE_CLASS = "org.ysmnon.bridge.offset.NoNAlignmentRenderBridge";

    private static Method resolve;

    private static boolean resolved;

    private NoNAlignmentProbe() {
    }

    /**
     * 该玩家当前这一帧要施加的模型平移（单位：格，玩家本地朝向坐标系）。
     *
     * @param player    目标玩家
     * @param baseScale 渲染状态里的 base scale（与 bridge 渲染路径同一个乘数）
     * @return 偏移量；没有活动 NoN 动画上下文或解析失败时返回 {@code null}
     */
    public static Vector3f offsetOf(Player player, double baseScale) {
        if (player == null) {
            return null;
        }
        Method method = resolve();
        if (method == null) {
            return null;
        }
        Object result;
        try {
            result = method.invoke(null, player, baseScale);
        } catch (Throwable t) {
            return null;
        }
        if (!(result instanceof double[] value) || value.length < 3) {
            return null;
        }
        if (!Double.isFinite(value[0]) || !Double.isFinite(value[1]) || !Double.isFinite(value[2])) {
            return null;
        }
        return new Vector3f((float) value[0], (float) value[1], (float) value[2]);
    }

    private static synchronized Method resolve() {
        if (resolved) {
            return resolve;
        }
        resolved = true;
        if (!BRIDGE_LOADED) {
            return null;
        }
        try {
            Class<?> type = Class.forName(BRIDGE_CLASS);
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals("resolveAlignmentOffset")) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                // resolveAlignmentOffset(PlayerEntity, double) -> double[]
                if (params.length == 2
                        && params[0].isAssignableFrom(Player.class)
                        && params[1] == double.class
                        && method.getReturnType() == double[].class) {
                    resolve = method;
                    break;
                }
            }
        } catch (Throwable ignored) {
            // bridge 不在或版本不符：保持 null，等价于"不偏移"。
        }
        return resolve;
    }
}
