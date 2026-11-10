package rip.ysm.render;

import com.elfmcys.yesstevemodel.util.accessors.BufferSourceAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Records the fully transformed model vertices produced during extraction, then
 * gives immutable snapshots to Minecraft's deferred geometry renderer. The
 * callback never reads live entities, animation matrices, or shared scratch data.
 */
public final class DeferredBufferSource implements BufferSourceAccessor {
    private final SubmitNodeCollector collector;
    private final Map<RenderType, RecordingConsumer> consumers = new LinkedHashMap<>();

    public DeferredBufferSource(SubmitNodeCollector collector) {
        this.collector = Objects.requireNonNull(collector, "collector");
    }

    public SubmitNodeCollector collector() {
        return collector;
    }

    public VertexConsumer getBuffer(RenderType type) {
        return consumers.computeIfAbsent(Objects.requireNonNull(type, "type"), ignored -> new RecordingConsumer());
    }

    public void endBatch() {
        for (Map.Entry<RenderType, RecordingConsumer> entry : consumers.entrySet()) {
            submit(entry.getKey(), entry.getValue());
        }
        consumers.clear();
    }

    public void endBatch(RenderType type) {
        RecordingConsumer consumer = consumers.remove(type);
        if (consumer != null) {
            submit(type, consumer);
        }
    }

    @Override
    public void initialize() {
        endBatch();
    }

    private void submit(RenderType type, RecordingConsumer consumer) {
        List<Vertex> vertices = consumer.snapshot();
        if (vertices.isEmpty()) {
            return;
        }
        // NativeModelRenderer and vanilla ModelPart already apply the caller's
        // pose before writing vertices, so the submitted pose is the identity.
        collector.submitCustomGeometry(new PoseStack(), type, (pose, output) -> {
            for (Vertex vertex : vertices) {
                vertex.emit(output);
            }
        });
    }

    private record Vertex(float x, float y, float z, int color, float u, float v,
                          int overlay, int light, float u3, float v3, float nx, float ny, float nz, float lineWidth) {
        void emit(VertexConsumer output) {
            output.addVertex(x, y, z).setColor(color).setUv(u, v)
                    .setOverlay(overlay).setLight(light).setUv3(u3, v3).setNormal(nx, ny, nz).setLineWidth(lineWidth);
        }
    }

    private static final class RecordingConsumer implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private boolean pending;
        private float x, y, z, u, v, u3, v3, nx, nz;
        private float ny = 1.0f;
        private float lineWidth = 1.0f;
        private int color = -1;
        private int overlay;
        private int light;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            finishVertex();
            this.x = x;
            this.y = y;
            this.z = z;
            color = -1;
            u = v = u3 = v3 = 0;
            overlay = light = 0;
            nx = nz = 0;
            ny = lineWidth = 1;
            pending = true;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            color = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            overlay = u & 0xFFFF | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            light = u & 0xFFFF | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            u3 = u;
            v3 = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            lineWidth = width;
            return this;
        }

        private void finishVertex() {
            if (pending) {
                vertices.add(new Vertex(x, y, z, color, u, v, overlay, light, u3, v3, nx, ny, nz, lineWidth));
                pending = false;
            }
        }

        private List<Vertex> snapshot() {
            finishVertex();
            return List.copyOf(vertices);
        }
    }
}
