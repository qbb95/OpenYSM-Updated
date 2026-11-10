package com.elfmcys.yesstevemodel.fabric;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;

public final class YesSteveModelFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YesSteveModel.init();
        EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
            if (entity instanceof ServerPlayer subject && NetworkHandler.isPlayerConnected(viewer)) {
                ModelInfoCapability.get(subject).ifPresent(cap ->
                        cap.createSyncMessage(subject, true).ifPresent(packet ->
                                NetworkHandler.sendToClientPlayer(packet, viewer)));
            }
        });
    }
}
