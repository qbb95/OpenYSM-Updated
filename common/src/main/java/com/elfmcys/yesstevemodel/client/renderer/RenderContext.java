package com.elfmcys.yesstevemodel.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.jetbrains.annotations.Nullable;

public final class RenderContext {
    private static final ThreadLocal<SubmitNodeCollector> COLLECTOR = new ThreadLocal<>();
    private static final ThreadLocal<CameraRenderState> CAMERA = new ThreadLocal<>();

    private RenderContext() {
    }

    public static void enter(SubmitNodeCollector collector, CameraRenderState cameraState) {
        COLLECTOR.set(collector);
        CAMERA.set(cameraState);
    }

    public static void exit() {
        COLLECTOR.remove();
        CAMERA.remove();
    }

    @Nullable
    public static SubmitNodeCollector collector() {
        return COLLECTOR.get();
    }

    @Nullable
    public static CameraRenderState camera() {
        return CAMERA.get();
    }

    /**
     * 当前是否处于"外部捕获"上下文：即 collector 不是原版主提交队列（{@code GameRenderer#getSubmitNodeStorage()}），
     * 例如 RealCamera 的模型绑定视图 / 绑定计算会用自己新建的 SubmitNodeStorage 来抓取模型几何。
     * <p>
     * 此时模型网格必须通过 {@code collector.submitCustomGeometry} 交给捕获方，而不能写进
     * {@code Minecraft#renderBuffers().bufferSource()}（那样捕获方永远拿不到几何），
     * 也不能走 GPU 直写路径（会画到当前帧缓冲而不是捕获目标上）。
     */
    public static boolean isExternalCapture() {
        SubmitNodeCollector current = COLLECTOR.get();
        if (current == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return true;
        }
        return minecraft.gameRenderer.getSubmitNodeStorage() != current;
    }
}
