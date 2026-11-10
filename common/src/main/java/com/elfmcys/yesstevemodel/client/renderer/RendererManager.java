package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.mixin.client.EntityRenderDispatcherAccessor;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import rip.ysm.api.PlatformAPI;
import rip.ysm.compat.sbackpack.SBackpackCompat;

public class RendererManager {

    private static CustomPlayerRenderer playerRenderer;

    private static ProjectileRenderer projectileRenderer;

    private static HandItemRenderer handRenderer;

    private static VehicleRenderer vehicleRenderer;

    private static net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver;

    private RendererManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, listener, Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "renderer_manager"));
    }

    private static void resetRenderers() {
        playerRenderer = null;
        projectileRenderer = null;
        handRenderer = null;
        vehicleRenderer = null;
        blockModelResolver = null;
    }

    private static void initRenderers(ResourceManager resourceManager) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                entityRenderDispatcher,
                getBlockModelResolver(),
                Minecraft.getInstance().getItemModelResolver(),
                Minecraft.getInstance().getMapRenderer(),
                resourceManager,
                Minecraft.getInstance().getEntityModels(),
                ((EntityRenderDispatcherAccessor) Minecraft.getInstance().getEntityRenderDispatcher()).getEquipmentAssetManager(),
                Minecraft.getInstance().getAtlasManager(),
                Minecraft.getInstance().font,
                Minecraft.getInstance().playerSkinRenderCache(),
                Minecraft.getInstance().getPalettedTextureManager()
        );
        playerRenderer = new CustomPlayerRenderer(context);
        projectileRenderer = new ProjectileRenderer(context);
        handRenderer = new HandItemRenderer();
        vehicleRenderer = new VehicleRenderer(context);
        SBackpackCompat.setupRenderLayers();
    }

    public static net.minecraft.client.renderer.block.BlockModelResolver getBlockModelResolver() {
        if (blockModelResolver == null) {
            blockModelResolver = new net.minecraft.client.renderer.block.BlockModelResolver(Minecraft.getInstance().getModelManager());
        }
        return blockModelResolver;
    }

    public static CustomPlayerRenderer getPlayerRenderer() {
        if (playerRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return playerRenderer;
    }

    public static ProjectileRenderer getProjectileRenderer() {
        if (projectileRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return projectileRenderer;
    }

    public static HandItemRenderer getHandRenderer() {
        if (handRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return handRenderer;
    }

    public static VehicleRenderer getVehicleRenderer() {
        if (vehicleRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return vehicleRenderer;
    }
}
