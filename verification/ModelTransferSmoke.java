import com.elfmcys.yesstevemodel.util.ModelLoadOrder;
import com.elfmcys.yesstevemodel.util.ModelPacketWriter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import java.util.concurrent.*;

/** Exercise ordering and asynchronous completion, including a stalled connection. */
public class ModelTransferSmoke {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        var catalog = new LinkedHashMap<String, Integer>();
        catalog.put("background", 1);
        catalog.put("fallback", 2);
        catalog.put("nearby", 3);
        catalog.put("self", 4);
        check(ModelLoadOrder.prioritize(catalog,
                List.of("self", "missing", "nearby", "self"), "fallback")
                .equals(List.of(4, 3, 2, 1)), "visible models first, complete stable catalog");
        check(ModelLoadOrder.prioritize(catalog, List.of(), "missing")
                .equals(List.of(1, 2, 3, 4)), "unknown fallback retains catalog order");
        check(ModelLoadOrder.prioritize(Map.of(), List.of("missing"), "missing")
                .isEmpty(), "empty catalog");
        var channel = new EmbeddedChannel();
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            check(ModelPacketWriter.send(listener -> {
                try { listener.operationComplete(channel.newSucceededFuture()); }
                catch (Exception e) { throw new RuntimeException(e); }
            }, 1000), "synchronous write completion");
            check(ModelPacketWriter.send(listener -> executor.schedule(() -> {
                try { listener.operationComplete(channel.newSucceededFuture()); }
                catch (Exception e) { throw new RuntimeException(e); }
            }, 10, TimeUnit.MILLISECONDS), 1000), "asynchronous write completion");
            check(!ModelPacketWriter.send(listener -> {
                try { listener.operationComplete(channel.newFailedFuture(new Exception("fixture"))); }
                catch (Exception e) { throw new RuntimeException(e); }
            }, 1000), "failed write stops producer");
            check(!ModelPacketWriter.send(listener -> {}, 10), "stalled write times out");
            check(!ModelPacketWriter.send(listener -> { throw new IllegalStateException("fixture"); },
                    1000), "submission failure stops producer");
            Thread.currentThread().interrupt();
            check(!ModelPacketWriter.send(listener -> {}, 1000), "interrupted wait stops producer");
            check(Thread.interrupted(), "interruption flag preserved");
        } finally { channel.finishAndReleaseAll(); }
        System.out.println("PASS ModelTransferSmoke: priority order and write completion/failure/timeout/interruption");
    }
}
