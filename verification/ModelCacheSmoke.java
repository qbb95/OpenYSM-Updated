import java.util.*;
import java.util.concurrent.*;
import rip.ysm.security.YsmCrypt;

/** A downloaded cache must decode identically on the next process startup. */
public class ModelCacheSmoke {
    public static void main(String[] args) throws Exception {
        byte[] serverKey = new byte[56], clientKey = new byte[56];
        new Random(263).nextBytes(serverKey);
        new Random(262).nextBytes(clientKey);
        try (var workers = Executors.newFixedThreadPool(6)) {
            var checks = new ArrayList<Future<?>>();
            for (int i = 0; i < 18; i++) {
                final int sample = i;
                checks.add(workers.submit(() -> {
                    try {
                        byte[] original = new byte[257 + sample * 8192];
                        new Random(sample).nextBytes(original);
                        for (int j = 0; j < original.length / 2; j++) original[j] = (byte) j;
                        long h1 = 91 + sample, h2 = -278 - sample;
                        byte[] server = YsmCrypt.encryptServerCache(original, serverKey, h1, h2);
                        var received = YsmCrypt.transcodeAndDecodeServerData(server, serverKey, clientKey, h1, h2);
                        if (!Arrays.equals(original, received.decompressedModel())
                                || !Arrays.equals(original, YsmCrypt.read(received.cacheData(), clientKey)))
                            throw new AssertionError("fresh download/cache restart mismatch: " + sample);
                    } catch (Exception e) { throw new RuntimeException(e); }
                }));
            }
            for (var check : checks) check.get();
        }
        System.out.println("PASS ModelCacheSmoke: 18 concurrent server/download/cache roundtrips");
    }
}
