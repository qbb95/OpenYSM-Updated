package rip.ysm.compat.realcamera;

import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.compat.non.NoNActorProbe;

/**
 * {@code MOUSE_ABSOLUTE} 视角模式下的"进 / 出 NoN 动画重新锚定"。
 * <p>
 * 背景：{@code composeMouseAbsolute} 的基准朝向取玩家真实身体 yaw 加动画侧滚（不用动画头骨的
 * 脸面轴水平投影——蹲 / 翻 / 躺时它短且抖，会把滚轮推拉横拖到身体另一侧）。代价是：动画自己把
 * 角色转向时，镜头还停在站立时那个方位。
 * <p>
 * 本类补这一段：检测 NoN 动画的"进入 / 退出"边沿，
 * <ul>
 *   <li><b>进入</b>时用 250 ms 采集"基准朝向 → 动画脸部朝向"的旋转（指数滑动平均抑抖），采集完
 *       <b>冻结</b>，整段动画期间作为常量世界旋转交给 {@code composeMouseAbsolute} 加在<b>基准朝向</b>上
 *       ——之后鼠标的上下左右仍然以画面方向为参照，不会被这个偏移带歪；</li>
 *   <li><b>退出</b>时把该常量按时间常数衰减回单位旋转，镜头平滑回到站立朝向。</li>
 * </ul>
 * 语义即"只在进 / 出瞬间纠正一次"，与 {@code FACE_RELATIVE} 的"全程跟随脸"不同。
 * <p>
 * 调用频率不敏感：时间量都用 {@code System.nanoTime()} 的真实时间差，一次渲染里被调用几次也不会
 * 改变行为；长时间没被调用（切第三人称）时超时自动复位。
 */
public final class FirstPersonFaceAnchor {

    /** 采集窗口长度：NoN 的过渡混入大约在这个尺度上完成。 */
    private static final long MEASURE_NANOS = 250_000_000L;

    /** 采集期滑动平均的时间常数（秒）。越小越跟手、越大越抑抖。 */
    private static final double MEASURE_TAU = 0.06d;

    /** 退出时衰减回零的时间常数（秒）。 */
    private static final double RELEASE_TAU = 0.09d;

    /** 单次 dt 上限，防止卡顿后一步跳到位。 */
    private static final double MAX_DT = 0.1d;

    /** 超过这个间隔视为"链路断过"，直接复位，避免陈旧偏移在重新进入第一人称时突然生效。 */
    private static final double STALE_SECONDS = 1.0d;

    private static final Quaternionf IDENTITY = new Quaternionf();

    /** 当前冻结的世界旋转（相对"基准朝向"）。 */
    private static final Quaternionf HOLD = new Quaternionf();

    private static final Matrix3f MAT_ANIM = new Matrix3f();
    private static final Matrix3f MAT_BASE = new Matrix3f();
    private static final Matrix3f MAT_DELTA = new Matrix3f();
    private static final Vector3f TMP_RIGHT = new Vector3f();
    private static final Vector3f TMP_UP = new Vector3f();
    private static final Vector3f TMP_BACK = new Vector3f();
    private static final Vector3f TMP_BASE_FORWARD = new Vector3f();
    private static final Vector3f TMP_BASE_UP = new Vector3f();
    private static final Vector3f TMP_ANIM_FORWARD = new Vector3f();
    private static final Vector3f TMP_ANIM_UP = new Vector3f();

    /** HOLD 是否已写入过（未写入时第一帧直接取瞬时值，而不是从单位旋转慢慢爬）。 */
    private static boolean holdValid;

    private static boolean animating;

    private static boolean measuring;

    private static long enterNanos;

    private static long lastNanos;

    private FirstPersonFaceAnchor() {
    }

    /**
     * 本帧要叠加在"基准朝向"上的世界旋转；没有锚定时返回 {@code null}（调用方跳过）。
     * <p>
     * 返回的是内部实例，调用方<b>只可读</b>（拿去做 {@code Vector3f.rotate}），不得修改。
     *
     * @param player      本地玩家
     * @param animForward 动画脸的前向（"基准朝向"要收敛到的目标）
     * @param animUp      动画脸的上向
     * @param baseForward 基准朝向的前向（身体 yaw + 动画侧滚，未叠加鼠标）
     * @param baseUp      基准朝向的上向
     */
    public static Quaternionf anchor(Player player, Vector3f animForward, Vector3f animUp,
                                     Vector3f baseForward, Vector3f baseUp) {
        long now = System.nanoTime();
        double dt = 0.0d;
        if (lastNanos != 0L) {
            double raw = (now - lastNanos) / 1.0e9d;
            if (raw > STALE_SECONDS) {
                // 中间整段时间没被调用过（切第三人称 / 绑定没跑）：旧偏移已经没有意义
                reset();
            }
            dt = Math.min(Math.max(raw, 0.0d), MAX_DT);
        }
        lastNanos = now;

        TMP_ANIM_FORWARD.set(animForward);
        TMP_ANIM_UP.set(animUp);

        boolean active = player != null && NoNActorProbe.isActorActive(player.getUUID());
        if (active) {
            if (!animating) {
                animating = true;
                measuring = true;
                enterNanos = now;
                holdValid = false;
                HOLD.identity();

            }
            if (measuring) {
                Quaternionf delta = deltaTo(baseForward, baseUp);
                if (delta != null) {
                    if (!holdValid) {
                        HOLD.set(delta);
                        holdValid = true;
                    } else {
                        HOLD.slerp(delta, (float) (1.0d - Math.exp(-dt / MEASURE_TAU)));
                    }
                }
                if (now - enterNanos >= MEASURE_NANOS) {
                    measuring = false;

                }
            }
        } else {
            if (animating) {
                animating = false;
                measuring = false;

            }
            release(dt);
        }

        if (!holdValid || isIdentity()) {
            return null;
        }

        return HOLD;
    }

    /** 退出动画后把偏移平滑衰减回单位旋转。 */
    private static void release(double dt) {
        if (!holdValid) {
            return;
        }
        if (isIdentity()) {
            HOLD.identity();
            holdValid = false;
            return;
        }
        float alpha = (float) (1.0d - Math.exp(-dt / RELEASE_TAU));
        HOLD.slerp(IDENTITY, alpha);
        if (isIdentity()) {
            HOLD.identity();
            holdValid = false;
        }
    }

    private static boolean isIdentity() {
        return Math.abs(HOLD.w) >= 0.99995f;
    }

    private static void reset() {
        HOLD.identity();
        holdValid = false;
        measuring = false;
        animating = false;
    }

    /**
     * 求把"基准朝向基"转到"动画脸部基"的旋转：{@code R_anim * R_base^T}。
     * 任一基退化（前向 / 上向共线）时返回 {@code null}，调用方保持上一次的值。
     */
    private static Quaternionf deltaTo(Vector3f baseForward, Vector3f baseUp) {
        TMP_BASE_FORWARD.set(baseForward);
        TMP_BASE_UP.set(baseUp);
        if (!basis(TMP_BASE_FORWARD, TMP_BASE_UP, MAT_BASE) || !basis(TMP_ANIM_FORWARD, TMP_ANIM_UP, MAT_ANIM)) {
            return null;
        }
        MAT_DELTA.set(MAT_ANIM).mul(MAT_BASE.transpose());
        return MAT_DELTA.getNormalizedRotation(new Quaternionf());
    }

    /** 用 (forward, up) 构造右手正交基，列向量依次为 right / up / back(= -forward)。 */
    private static boolean basis(Vector3f forward, Vector3f up, Matrix3f dest) {
        if (forward.lengthSquared() < 1.0e-8f || up.lengthSquared() < 1.0e-8f) {
            return false;
        }
        forward.cross(up, TMP_RIGHT);
        if (TMP_RIGHT.lengthSquared() < 1.0e-6f) {
            // forward 与 up 共线：坐标系退化
            return false;
        }
        TMP_RIGHT.normalize();
        TMP_RIGHT.cross(forward, TMP_UP).normalize();
        forward.negate(TMP_BACK);
        dest.set(TMP_RIGHT, TMP_UP, TMP_BACK);
        return true;
    }
}
