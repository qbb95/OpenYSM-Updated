package com.elfmcys.yesstevemodel.client.renderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import rip.ysm.render.DeferredBufferSource;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jetbrains.annotations.Nullable;

public final class RenderContext {
    private record Scope(SubmitNodeCollector collector, CameraRenderState camera, DeferredBufferSource buffers) {}
    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private RenderContext() {
    }

    public static void enter(SubmitNodeCollector collector, CameraRenderState cameraState) {
        SCOPES.get().push(new Scope(collector, cameraState, new DeferredBufferSource(collector)));
    }

    public static void exit() {
        Deque<Scope> scopes = SCOPES.get();
        if (scopes.isEmpty()) {
            throw new IllegalStateException("No YSM render scope to exit");
        }
        Scope scope = scopes.pop();
        try {
            scope.buffers().endBatch();
        } finally {
            if (scopes.isEmpty()) {
                SCOPES.remove();
            }
        }
    }

    @Nullable
    public static SubmitNodeCollector collector() {
        Scope scope = SCOPES.get().peek();
        return scope == null ? null : scope.collector();
    }

    @Nullable
    public static CameraRenderState camera() {
        Scope scope = SCOPES.get().peek();
        return scope == null ? null : scope.camera();
    }

    public static DeferredBufferSource bufferSource() {
        Scope scope = SCOPES.get().peek();
        if (scope == null) {
            throw new IllegalStateException("YSM geometry must be produced inside a render scope");
        }
        return scope.buffers();
    }
}
