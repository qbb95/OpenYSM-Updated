package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import rip.ysm.api.client.KeyMappingFactory;

public class InputUtil {
    public static boolean isKeyPressed(KeyEvent event, KeyMapping keyMapping) {
        return KeyMappingFactory.isActiveAndMatches(keyMapping, event);
    }

    public static boolean isPlayerReady() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null || minecraft.screen != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return minecraft.isWindowActive();
    }
}
