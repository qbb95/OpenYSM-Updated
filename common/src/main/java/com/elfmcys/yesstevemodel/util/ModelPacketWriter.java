package com.elfmcys.yesstevemodel.util;

import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Wait for write completion notifications without adding a delay per chunk. */
public final class ModelPacketWriter {
    private ModelPacketWriter() {}

    public static boolean send(Consumer<ChannelFutureListener> writer, long timeoutMillis) {
        var completion = new CompletableFuture<Boolean>();
        try {
            writer.accept(future -> completion.complete(future.isSuccess()));
            // The producer has one outstanding packet at most; slow connections
            // naturally apply backpressure rather than accumulating the catalog.
            return completion.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception failedWrite) {
            return false;
        }
    }
}
