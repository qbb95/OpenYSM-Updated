package com.ysm.parser;

import com.elfmcys.yesstevemodel.resource.NativeParseUtil;
import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMBinarySerializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YsmCrypt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Debug-only: reproduce the server->client cache pipeline end to end.
 *   raw .ysm -> native parse -> serialize format 32 -> encryptServerCache(serverKey)
 *   -> verifyServerCache -> transcodeServerDataToClientCache(clientKey)
 *   -> decrypt serverData -> deserialize format 32 -> report stats.
 */
public final class YsmCacheProbe {

    private YsmCacheProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: YsmCacheProbe <file.ysm> <serverKeyB64>");
            System.exit(1);
        }
        Path file = Path.of(args[0]);
        byte[] raw = Files.readAllBytes(file);
        byte[] serverKey = Base64.getDecoder().decode(args[1]);
        System.out.println("[CacheProbe] file=" + file.getFileName() + " size=" + raw.length
                + " serverKey.len=" + serverKey.length);

        RawYsmModel model = NativeParseUtil.parseNative(raw, file.getFileName().toString());
        if (model == null) {
            System.out.println("[CacheProbe] native parse FAILED");
            System.exit(2);
        }
        System.out.println("[CacheProbe] sha256='" + model.properties.sha256 + "'");
        long[] hashes = YsmCrypt.calculateModelHashes(model.properties.sha256, serverKey);
        System.out.printf("[CacheProbe] cacheName=%016x%016x%n", hashes[0], hashes[1]);

        byte[] serialized;
        try (YSMByteBuf buf = YSMBinarySerializer.serialize(model, 32, true)) {
            serialized = buf.toArray();
        }
        System.out.println("[CacheProbe] serialized(len 32)=" + serialized.length);

        byte[] serverCache = YsmCrypt.encryptServerCache(serialized, serverKey, hashes[0], hashes[1]);
        System.out.println("[CacheProbe] encryptServerCache=" + serverCache.length
                + " verify=" + YsmCrypt.verifyServerCache(serverCache, hashes[0], hashes[1]));

        byte[] clientKey = new byte[56];
        new SecureRandom().nextBytes(clientKey);
        byte[] clientCache = YsmCrypt.transcodeServerDataToClientCache(serverCache, serverKey, clientKey, hashes[0], hashes[1]);
        System.out.println("[CacheProbe] transcode -> clientCache=" + clientCache.length);

        byte[] clearText = YsmCrypt.read(clientCache, clientKey);
        System.out.println("[CacheProbe] YsmCrypt.read(clientCache) -> clearText=" + clearText.length);

        try (YSMBinaryDeserializer d = new YSMBinaryDeserializer(clearText, 32)) {
            RawYsmModel rt = d.deserializeKeepOpen();
            RawYsmModel.RawGeometry m = rt.mainEntity.mainModel;
            if (m == null) {
                System.out.println("[CacheProbe] mainModel == NULL");
            } else {
                int c = 0, f = 0;
                for (RawYsmModel.RawBone b : m.bones) { c += b.cubes.size(); for (RawYsmModel.RawCube cu : b.cubes) f += cu.faces.size(); }
                System.out.println("[CacheProbe] READBACK bones=" + m.bones.size() + " cubes=" + c + " faces=" + f
                        + " textures=" + rt.mainEntity.textures.size()
                        + " anims=" + rt.mainEntity.animationFiles.size()
                        + " defaultTex='" + rt.properties.defaultTexture + "'");
            }
        }
    }
}
