package rip.ysm.compat.non;

import dev.architectury.platform.Platform;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 判断"本地玩家当前是否处于 needs of nature 动画中"，供滚轮推拉的窗口判定使用。
 * <p>
 * 为什么需要它：{@code player.main} 上的动画名<b>不能</b>用来分辨触发动画 —— 模型自带状态机
 * （fly / swim / ride / 部分模型的 walk）在运动状态下同样返回具体名字（{@code "Coded -> fly"}），
 * 用"名字不像 idle 就算触发动画"会让飞行、走路时滚轮被镜头推拉吃掉。而桥接注入的 NoN 动画在
 * controller 上暴露的是<b>原始 key</b>（如 {@code p1_player}），名字里也没有 {@code non_}。
 * <p>
 * 所以这里直接问 NoN 自己的运行时：{@code ClientAnimationRuntime.isActorActive(UUID)}。
 * 与 {@code non-animation-perspective} 的 X 光/穿透用的是同一个信号，语义就是"这个玩家正在演动画"。
 * <p>
 * 纯反射、可失败：NoN 缺失或类名变动时 {@link #isActive()} 返回 {@code false}，退化成"不接管滚轮"
 * （安全方向：滚轮继续切物品栏）。
 */
public final class NoNActorProbe {

    private static final boolean NON_LOADED = Platform.isModLoaded("needsofnature");

    private static final String RUNTIME_CLASS =
            "com.nonid.internal.animation.client.runtime.ClientAnimationRuntime";

    private static Method isActorActive;

    private static boolean resolved;

    private NoNActorProbe() {
    }

    /** NoN 是否可用（类与目标方法都解析成功）。 */
    public static boolean isActive() {
        return NON_LOADED && resolve() != null;
    }

    /** 该玩家是否是 NoN 动画的当前演员。解析失败一律 {@code false}。 */
    public static boolean isActorActive(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Method method = resolve();
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, uuid));
        } catch (Throwable t) {
            return false;
        }
    }

    private static synchronized Method resolve() {
        if (resolved) {
            return isActorActive;
        }
        resolved = true;
        if (!NON_LOADED) {
            return null;
        }
        try {
            Class<?> type = Class.forName(RUNTIME_CLASS);
            for (Method method : type.getDeclaredMethods()) {
                // isActorActive(UUID) / isActorPendingOrActive(UUID) 一类单参静态布尔查询
                if (!method.getName().equals("isActorActive") || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0] == UUID.class
                        && method.getReturnType() == boolean.class) {
                    isActorActive = method;
                    break;
                }
            }
        } catch (Throwable ignored) {
            // NoN 不在或版本不符：保持 null，调用方按"不在动画中"处理。
        }
        return isActorActive;
    }
}
