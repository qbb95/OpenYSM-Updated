package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.ysm.compat.realcamera.ScrollDolly;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    /**
     * 绑定模式第一人称 + 触发动画期间把滚轮改成镜头推拉。
     * 必须整段取消：原版无屏幕分支会把滚轮拿去切换快捷栏槽位
     * （{@code Inventory.setSelectedSlot}），不取消的话推拉的同时还会换手。
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ysm$scrollDolly(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ScrollDolly.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
