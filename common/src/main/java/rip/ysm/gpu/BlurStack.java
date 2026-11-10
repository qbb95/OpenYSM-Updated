package rip.ysm.gpu;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.List;

public final class BlurStack {
    private static final List<Region> regions = new ArrayList<>();
    private static final Matrix4f mvpScratch = new Matrix4f();
    private static final float[] mvpFloats = new float[16];
    private static long frameCounter = 0L;

    private BlurStack() {
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius) {
        pushBlur(x, y, w, h, cornerRadius, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius, int tintRgba) {
        Region r = new Region();
        r.isPie = false;
        r.x = x;
        r.y = y;
        r.w = w;
        r.h = h;
        r.cornerRadius = cornerRadius;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius) {
        pushBlurPie(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius, int tintRgba) {
        float pad = 1.0f;
        Region r = new Region();
        r.isPie = true;
        r.x = centerX - outerRadius - pad;
        r.y = centerY - outerRadius - pad;
        r.w = (outerRadius + pad) * 2.0f;
        r.h = (outerRadius + pad) * 2.0f;
        r.pieCenterX = centerX;
        r.pieCenterY = centerY;
        r.pieInner = innerRadius;
        r.pieOuter = outerRadius;
        r.pieStart = startAngle;
        r.pieEnd = endAngle;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void popBlur() {
        if (!regions.isEmpty()) regions.remove(regions.size() - 1);
    }

    public static void clear() {
        regions.clear();
    }

    public static boolean isEmpty() {
        return regions.isEmpty();
    }

    public static void flush(GuiGraphicsExtractor graphics) {
        if (regions.isEmpty()) return;
        // 26.2 owns blur and render ordering. Blur the preceding GUI stratum,
        // then submit the queued region tints; never issue GL during extraction.
        var state = ((com.elfmcys.yesstevemodel.mixin.client.GuiGraphicsExtractorAccessor) graphics)
                .ysm$getGuiRenderState();
        // A parent screen or vanilla's background extraction may have already
        // requested the frame's only blur. Reuse that request for nested panels.
        if (((com.elfmcys.yesstevemodel.mixin.client.GuiRenderStateAccessor) state)
                .ysm$getFirstStratumAfterBlur() == Integer.MAX_VALUE) {
            graphics.blurBeforeThisStratum();
        }
        for (Region region : regions) {
            // White is the neutral multiplicative tint of the old blur shader.
            if (region.tintRgba == 0xFFFFFFFF) continue;
            if (region.isPie) {
                Pie.draw(graphics, region.pieCenterX, region.pieCenterY, region.pieInner,
                        region.pieOuter, region.pieStart, region.pieEnd, region.tintRgba);
            } else {
                graphics.fill((int) region.x, (int) region.y,
                        (int) Math.ceil(region.x + region.w), (int) Math.ceil(region.y + region.h),
                        region.tintRgba);
            }
        }
        regions.clear();
    }

    private static final class Region {
        boolean isPie;
        float x;
        float y;
        float w;
        float h;
        float cornerRadius;
        float pieCenterX;
        float pieCenterY;
        float pieInner;
        float pieOuter;
        float pieStart;
        float pieEnd;
        float blurRadius;
        int tintRgba;
    }
}
