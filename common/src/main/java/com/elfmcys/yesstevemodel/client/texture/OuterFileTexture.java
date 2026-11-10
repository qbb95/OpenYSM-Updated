package com.elfmcys.yesstevemodel.client.texture;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import rip.ysm.compat.oculus.ShadersTextureType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class OuterFileTexture extends AbstractTexture implements ITextureMap {
    private final byte[] data;

    private Map<ShadersTextureType, OuterFileTexture> suffixTextures = Reference2ReferenceMaps.emptyMap();

    public OuterFileTexture(byte[] data) {
        this.data = data;
    }

    public void load() {
        if (RenderSystem.isOnRenderThread()) {
            doLoad();
        } else {
            RenderSystem.queueFencedTask(this::doLoad);
        }
    }

    public void doLoad() {
        NativeImage image = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(data));
            GpuDevice gpuDevice = RenderSystem.getDevice();
            this.texture = gpuDevice.createTexture("OutFileTexture", 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
            this.textureView = gpuDevice.createTextureView(this.texture);
            gpuDevice.createCommandEncoder().writeToTexture(this.texture, image);
            this.sampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.REPEAT, AddressMode.REPEAT,
                FilterMode.NEAREST, FilterMode.NEAREST,
                false
            );
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // writeToTexture 是同步上传，上传后解码图即为死重（W*H*4）。data 需保留：
            // locatable 被 GC 后纹理会 release，模型切回时 load() 会再次调用。
            if (image != null) {
                image.close();
            }
        }
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(map));
    }

    public Map<ShadersTextureType, ? extends AbstractTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
