package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.access.ServerCommonPacketListenerImplAccessor;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public class ServerCommonPacketListenerImplMixin implements ServerCommonPacketListenerImplAccessor {

    @Unique
    private Connection ysm$connection;

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("TAIL"))
    public void init(MinecraftServer minecraftServer, Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        this.ysm$connection = connection;
    }

    @Unique
    public Connection ysm$getConnection() {
        return ysm$connection;
    }

}
