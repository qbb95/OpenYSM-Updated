package rip.ysm.compat.realcamera;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RenderContext;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 把 YSM 模型头部的实时朝向喂给 RealCamera 的绑定模式。
 * <p>
 * RealCamera 自己的 YSM 兼容（{@code YSMCompat}）靠"渲染模型 + 按 UV 匹配用户保存的绑定目标"取位置/朝向，
 * 默认没有保存过的目标时永远匹配不上；于是退回到默认目标 {@code minecraft_head}（优先级 5），
 * 而它的 {@code bindConfig.bindRotation() == false}，{@code RealCameraCore.getEulerAngle} 会
 * 直接返回玩家自己的 pitch/yaw ⇒ 表现为"位置跟随、朝向不跟"（人物侧躺时镜头仍然竖直）。
 * <p>
 * 这里注册一个优先级更高（1000）的绑定函数：直接按模型头部定位器的实时矩阵给出
 * forward / upward / position，绕开纹理 UV 匹配，任何模型都能用；因为绑定目标的
 * {@code bindRotation} 为 true，RealCamera 会改用相机矩阵反解欧拉角，roll 也能带出来。
 * <p>
 * RealCamera 是可选依赖：所有 {@code com.xtracr.realcamera.*} 都走反射，且只在检测到模组时加载。
 */
public final class RealCameraBinding {

    private static final boolean REALCAMERA_LOADED = Platform.isModLoaded("realcamera");

    private static boolean registered;

    private RealCameraBinding() {
    }

    public static boolean isLoaded() {
        return REALCAMERA_LOADED && RealCameraHolder.isLoaded();
    }

    public static boolean isActive() {
        return REALCAMERA_LOADED && RealCameraHolder.isActive();
    }

    public static void register() {
        if (!REALCAMERA_LOADED || registered) {
            return;
        }
        registered = true;
        try {
            RealCameraHolder.register();
            YesSteveModel.LOGGER.info("[ysm-realcamera] 已注册头部朝向绑定函数 (priority 1000)");
        } catch (Throwable t) {
            YesSteveModel.LOGGER.warn("[ysm-realcamera] 注册绑定函数失败，RealCamera 兼容不可用", t);
        }
    }

    private static Object computeBindResult(Object clientObject, Object partialTicksObject) {
        try {
            return computeBindResultUnsafe(clientObject, partialTicksObject);
        } catch (Throwable t) {
            // 绝不能把异常抛回 RealCamera 的绑定循环，否则整帧相机计算失败
            YesSteveModel.LOGGER.warn("[ysm-realcamera] 绑定函数异常", t);
            return RealCameraHolder.emptyResult();
        }
    }

    private static Object computeBindResultUnsafe(Object clientObject, Object partialTicksObject) {
        if (!(clientObject instanceof Minecraft client) || !(partialTicksObject instanceof Float partialTicks)) {
            return RealCameraHolder.emptyResult();
        }
        LocalPlayer player = client.player;
        // 绑定不生效时把推拉距离回中，避免积攒的距离在重新进入第一人称时突然生效。
        // 但"RealCamera 自己 inactive"不等于"推拉该结束"：`ESC` 暂停时 RealCamera 会把 active 置 false
        // （GameRenderer 传下来的 advanceGameTime=false），若照这句话清空，暂停一次就把滚出来的距离
        // 抹掉了。所以这里只按"模组/玩家层面确实没了"来清，窗口本身交给 ScrollDolly.tickEnd 判。
        if (player == null || !YesSteveModel.isAvailable()) {
            ScrollDolly.reset();
            return RealCameraHolder.emptyResult();
        }
        // 模型绑定视图 / RealCamera 外部捕获期间不要把自己的结果反过来喂给自己
        if (ModelPreviewRenderer.isPreview() || RenderContext.isExternalCapture()) {
            ScrollDolly.reset();
            return RealCameraHolder.emptyResult();
        }
        if (!GeneralConfig.REALCAMERA_BIND_ROTATION.get().booleanValue()) {
            ScrollDolly.reset();
            return RealCameraHolder.emptyResult();
        }
        PlayerCapability capability = PlayerCapability.get((Player) player).orElse(null);
        if (capability == null || !capability.isModelActive()) {
            ScrollDolly.reset();
            return RealCameraHolder.emptyResult();
        }
        AvatarRenderState renderState = RendererManager.getPlayerRenderer().createRenderState(player, partialTicks);
        Vector3f forward = new Vector3f();
        Vector3f upward = new Vector3f();
        Vector3f position = new Vector3f();
        if (!RendererManager.getPlayerRenderer().computeBindPose(player, renderState, partialTicks, forward, upward, position)) {
            return RealCameraHolder.emptyResult();
        }
        return RealCameraHolder.createResult(forward, upward, position);
    }

    private static final class RealCameraHolder {

        private static boolean initialized;

        private static Method registerFunction;
        private static Method isActiveMethod;
        private static Constructor<?> bindTargetConstructor;
        private static Constructor<?> bindResultConstructor;
        private static Method setForward;
        private static Method setUpward;
        private static Method setPosition;
        private static Object bindTarget;
        private static Object emptyResult;

        private RealCameraHolder() {
        }

        static boolean isLoaded() {
            try {
                ensureInitialized();
                return initialized;
            } catch (Throwable t) {
                return false;
            }
        }

        static boolean isActive() {
            if (!isLoaded()) {
                return false;
            }
            try {
                return (Boolean) isActiveMethod.invoke(null);
            } catch (Throwable t) {
                return false;
            }
        }

        static void register() throws Exception {
            ensureInitialized();
            if (!initialized) {
                throw new IllegalStateException("RealCamera API 反射初始化失败");
            }
            BiFunction<Object, Object, Object> function = RealCameraBinding::computeBindResult;
            registerFunction.invoke(null, 1000, function);
        }

        static Object emptyResult() {
            return emptyResult;
        }

        static Object createResult(Vector3f forward, Vector3f upward, Vector3f position) {
            try {
                Object result = bindResultConstructor.newInstance(bindTarget);
                setForward.invoke(result, new Vec3(forward.x, forward.y, forward.z));
                setUpward.invoke(result, new Vec3(upward.x, upward.y, upward.z));
                setPosition.invoke(result, new Vec3(position.x, position.y, position.z));
                return result;
            } catch (Throwable t) {
                return emptyResult;
            }
        }

        private static synchronized void ensureInitialized() throws Exception {
            if (initialized) {
                return;
            }
            Class<?> apiClass = Class.forName("com.xtracr.realcamera.api.RealCameraAPI");
            Class<?> coreClass = Class.forName("com.xtracr.realcamera.RealCameraCore");
            Class<?> bindTargetClass = Class.forName("com.xtracr.realcamera.config.BindTarget");
            Class<?> targetConfigClass = Class.forName("com.xtracr.realcamera.config.BindTarget$TargetConfig");
            Class<?> bindConfigClass = Class.forName("com.xtracr.realcamera.config.BindTarget$BindConfig");
            Class<?> offsetConfigClass = Class.forName("com.xtracr.realcamera.config.OffsetConfig");
            Class<?> bindResultClass = Class.forName("com.xtracr.realcamera.api.BindResult");

            registerFunction = apiClass.getMethod("registerFunction", int.class, BiFunction.class);
            isActiveMethod = coreClass.getMethod("isActive");

            Constructor<?> targetConfigConstructor = targetConfigClass.getDeclaredConstructor(
                    float.class, float.class, float.class, float.class, float.class, float.class);
            Constructor<?> bindConfigConstructor = bindConfigClass.getDeclaredConstructor(
                    boolean.class, boolean.class, boolean.class, boolean.class);
            Constructor<?> offsetConfigConstructor = offsetConfigClass.getDeclaredConstructor();
            targetConfigConstructor.setAccessible(true);
            bindConfigConstructor.setAccessible(true);
            offsetConfigConstructor.setAccessible(true);

            // 位置/朝向全部由我们自己给，UV 目标只是占位；bindRotation 必须为 true，
            // 否则 RealCameraCore.getEulerAngle 会退回玩家自己的 pitch/yaw，roll 永远为 0。
            Object targetConfig = targetConfigConstructor.newInstance(0f, 0f, 0f, 0f, 0f, 0f);
            Object bindConfig = bindConfigConstructor.newInstance(true, true, true, true);
            Object offsetConfig = offsetConfigConstructor.newInstance();

            bindTargetConstructor = bindTargetClass.getDeclaredConstructor(
                    String.class, String.class, int.class, float.class,
                    targetConfigClass, bindConfigClass, offsetConfigClass, List.class);
            bindTargetConstructor.setAccessible(true);
            bindTarget = bindTargetConstructor.newInstance(
                    "ysm_nonanimation_bind", "yes_steve_model:realcamera_bind", 0, 0.2f,
                    targetConfig, bindConfig, offsetConfig, List.of());

            bindResultConstructor = bindResultClass.getDeclaredConstructor(bindTargetClass);
            bindResultConstructor.setAccessible(true);
            setForward = bindResultClass.getMethod("setForward", Vec3.class);
            setUpward = bindResultClass.getMethod("setUpward", Vec3.class);
            setPosition = bindResultClass.getMethod("setPosition", Vec3.class);

            Field emptyField = bindResultClass.getField("EMPTY");
            emptyResult = emptyField.get(null);

            initialized = true;
        }
    }
}
