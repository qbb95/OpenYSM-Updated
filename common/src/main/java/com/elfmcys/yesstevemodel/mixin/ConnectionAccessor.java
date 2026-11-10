package com.elfmcys.yesstevemodel.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Connection.class, remap = false)
public interface ConnectionAccessor {
    @Accessor(value = "channel", remap = false)
    Channel ysm$getChannel();
}
