package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.mixin.client.GuiGraphicsExtractorAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/** Backend-independent, deferred annular-sector drawing for model menus. */
public final class Pie {
    public static final float tau = (float) (Math.PI * 2.0);

    public static void draw(GuiGraphicsExtractor graphics, float x, float y, float inner, float outer,
                            float start, float end, int color) {
        draw(graphics, x, y, inner, outer, start, end, color, 1.0f);
    }

    public static void draw(GuiGraphicsExtractor graphics, float x, float y, float inner, float outer,
                            float start, float end, int color, float feather) {
        if (!(outer > inner) || !Float.isFinite(start) || !Float.isFinite(end)) return;
        float sweep = end - start;
        while (sweep < 0) sweep += tau;
        if (sweep == 0 && start != end) sweep = tau;
        if (sweep == 0) return;
        sweep = Math.min(tau, sweep);
        float edge = Math.max(0, Math.min(feather, (outer - inner) * 0.5f));
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ((GuiGraphicsExtractorAccessor) graphics).ysm$getGuiRenderState().addGuiElement(
                new Sector(pose, x, y, Math.max(0, inner), outer, start, sweep, color, edge,
                        bounds(pose, x, y, outer + edge)));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float x, float y, float radius) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            Vector2f p = pose.transformPosition(x + ((i & 1) == 0 ? -radius : radius),
                    y + ((i & 2) == 0 ? -radius : radius), new Vector2f());
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y);
        }
        return new ScreenRectangle((int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX) - (int) Math.floor(minX), (int) Math.ceil(maxY) - (int) Math.floor(minY));
    }

    private record Sector(Matrix3x2fc pose, float x, float y, float inner, float outer,
                          float start, float sweep, int color, float feather,
                          ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return null; }
        @Override public void buildVertices(VertexConsumer out) {
            int steps = Math.max(1, (int) Math.ceil(sweep * Math.max(outer, 16) / 3.0f));
            for (int i = 0; i < steps; i++) {
                float a = start + sweep * i / steps;
                float b = start + sweep * (i + 1) / steps;
                quad(out, a, b, inner, outer, color, color);
                if (feather > 0) {
                    quad(out, a, b, outer, outer + feather, color, color & 0x00FFFFFF);
                    if (inner > 0) quad(out, a, b, Math.max(0, inner - feather), inner,
                            color & 0x00FFFFFF, color);
                }
            }
        }
        private void quad(VertexConsumer out, float a, float b, float r0, float r1, int c0, int c1) {
            vertex(out, a, r1, c1); vertex(out, a, r0, c0);
            vertex(out, b, r0, c0); vertex(out, b, r1, c1);
        }
        private void vertex(VertexConsumer out, float angle, float radius, int argb) {
            out.addVertexWith2DPose(pose, x + (float) Math.cos(angle) * radius,
                    y + (float) Math.sin(angle) * radius).setColor(argb);
        }
    }
}
