package rip.ysm.gpu;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Own framebuffer for legacy GL helpers; avoids Minecraft's private GlDevice. */
final class LegacyFramebuffers {
    private static int framebuffer;

    static int framebuffer(GlTextureView color, GlTextureView depth) {
        int previous = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        if (framebuffer == 0) framebuffer = GL30.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        try {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, color.glId(), color.fboMipLevel());
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depth.glId(), depth.fboMipLevel());
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Incomplete legacy YSM framebuffer");
            }
            return framebuffer;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
        }
    }

    private LegacyFramebuffers() { }
}
