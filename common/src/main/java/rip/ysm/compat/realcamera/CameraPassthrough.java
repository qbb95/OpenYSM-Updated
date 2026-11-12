package rip.ysm.compat.realcamera;

/**
 * 相机穿透请求开关：由外部 mod（{@code non_animation_perspective}）在"绑定模型第一人称 +
 * NoN 动画中"时每帧上报，本模组据此跳过滚轮推拉的两道方块裁剪，让镜头可以穿进地面 / 栅栏。
 * <p>
 * 为什么需要跨 mod 开关：两道裁剪都在本模组（{@code clampDollyAgainstBlocks} /
 * {@code clampDollyAgainstEyeRay}），而"什么时候允许穿透"是那个 mod 的产品决定。这里只提供
 * 一个无类型耦合的静态入口（调用方走反射），不引入任何编译期依赖。
 * <p>
 * {@link #EXPIRE_MILLIS} 是防呆：调用方一旦停止上报（mod 被移除、mixin 没应用、线程卡死），
 * 超过这个时间就自动回到"禁止穿透"，不会永久留下一个可以钻墙的相机。
 */
public final class CameraPassthrough {

    private static final long EXPIRE_MILLIS = 250L;

    private static volatile boolean requested;

    private static volatile long requestedAt;

    private CameraPassthrough() {
    }

    /** 由外部 mod 每帧调用；{@code false} 表示立即撤销。 */
    public static void setRequested(boolean value) {
        requested = value;
        requestedAt = System.currentTimeMillis();
    }

    /** 当前这一帧是否允许镜头穿透方块。 */
    public static boolean isRequested() {
        return requested && System.currentTimeMillis() - requestedAt < EXPIRE_MILLIS;
    }
}
