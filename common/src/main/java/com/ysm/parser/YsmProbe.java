package com.ysm.parser;

import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import rip.ysm.legacy.YesModelUtils;
import rip.ysm.security.YsmCrypt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Debug-only helper: native-parse a .ysm into a kept temp dir, inspect the
 * produced main.json (JSON vs binary), then feed the flat map into
 * YSMFolderDeserializer and print geometry/texture/anim stats.
 */
public final class YsmProbe {

    private YsmProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: YsmProbe <file.ysm>");
            System.exit(1);
        }
        Path file = Path.of(args[0]);
        byte[] raw = Files.readAllBytes(file);
        System.out.println("[YsmProbe] file=" + file.getFileName() + " size=" + raw.length);

        int version = YesModelUtils.getYsmCryptoVersion(raw);
        System.out.println("[YsmProbe] ysmCryptoVersion=" + version);

        RawYsmModel model = null;
        boolean nativeUsed = false;
        if (version == 3) {
            try {
                byte[] decrypted = YsmCrypt.decryptYsmFile(raw);
                System.out.println("[YsmProbe] JAVA_DECRYPT ok len=" + decrypted.length);
                try (var d = new com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer(decrypted)) {
                    model = d.deserializeKeepOpen();
                    d.parseYSMFooter(model);
                }
            } catch (Exception e) {
                System.out.println("[YsmProbe] JAVA_DECRYPT FAILED: " + e);
                model = null;
            }
        }
        if (model == null) {
            nativeUsed = true;
            model = parseNativeKeep(raw, file.getFileName().toString());
        }

        if (model == null) {
            System.out.println("[YsmProbe] RESULT: model == null  (FAILED)");
            System.exit(2);
        }

        System.out.println("[YsmProbe] nativeUsed=" + nativeUsed);
        System.out.println("[YsmProbe] formatVersion=" + model.formatVersion);
        System.out.println("[YsmProbe] metadata.name=" + model.metadata.name);
        System.out.println("[YsmProbe] properties.sha256='" + model.properties.sha256 + "'");
        System.out.println("[YsmProbe] properties.defaultTexture='" + model.properties.defaultTexture + "'");

        RawYsmModel.RawGeometry main = model.mainEntity.mainModel;
        if (main == null) {
            System.out.println("[YsmProbe] mainModel == NULL");
        } else {
            int cubes = 0, faces = 0;
            for (RawYsmModel.RawBone b : main.bones) {
                cubes += b.cubes.size();
                for (RawYsmModel.RawCube c : b.cubes) faces += c.faces.size();
            }
            System.out.println("[YsmProbe] mainModel.bones=" + main.bones.size()
                    + " cubes=" + cubes + " faces=" + faces
                    + " identifier=" + main.identifier);
        }
        System.out.println("[YsmProbe] textures=" + model.mainEntity.textures.size());
        model.mainEntity.textures.keySet().forEach(t -> System.out.println("[YsmProbe]   tex: " + t));
        System.out.println("[YsmProbe] animationFiles=" + model.mainEntity.animationFiles.size());
        model.mainEntity.animationFiles.keySet().forEach(a -> System.out.println("[YsmProbe]   anim: " + a));
        System.out.println("[YsmProbe] vehicles=" + model.vehicles.size()
                + " projectiles=" + model.projectiles.size()
                + " soundFiles=" + model.soundFiles.size()
                + " functionFiles=" + model.functionFiles.size());

        // Round-trip: serialize as format 32 (what processAndCacheModel writes
        // to the server cache) then deserialize back (what the client reads).
        try {
            rip.ysm.security.YSMByteBuf serialized =
                    com.elfmcys.yesstevemodel.resource.YSMBinarySerializer.serialize(model, 32, true);
            byte[] bytes = serialized.toArray();
            System.out.println("[YsmProbe] ROUNDTRIP serialized=" + bytes.length + " bytes");
            // Client cache read uses the explicit-format constructor (does NOT skip a
            // leading dword). The (byte[]) constructor WOULD consume 4 extra bytes.
            try (com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer d2 =
                         new com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer(bytes, 32)) {
                RawYsmModel rt = d2.deserializeKeepOpen();
                RawYsmModel.RawGeometry rtMain = rt.mainEntity.mainModel;
                if (rtMain == null) {
                    System.out.println("[YsmProbe] ROUNDTRIP mainModel == NULL");
                } else {
                    int c = 0, f = 0;
                    for (RawYsmModel.RawBone b : rtMain.bones) { c += b.cubes.size(); for (RawYsmModel.RawCube cu : b.cubes) f += cu.faces.size(); }
                    System.out.println("[YsmProbe] ROUNDTRIP bones=" + rtMain.bones.size()
                            + " cubes=" + c + " faces=" + f);
                }
                System.out.println("[YsmProbe] ROUNDTRIP textures=" + rt.mainEntity.textures.size());
                System.out.println("[YsmProbe] ROUNDTRIP anims=" + rt.mainEntity.animationFiles.size());
                System.out.println("[YsmProbe] ROUNDTRIP metadata.name=" + rt.metadata.name);
                System.out.println("[YsmProbe] ROUNDTRIP defaultTexture=" + rt.properties.defaultTexture);
            }
        } catch (Throwable e) {
            System.out.println("[YsmProbe] ROUNDTRIP ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }

    private static RawYsmModel parseNativeKeep(byte[] raw, String modelId) throws Exception {
        if (!YSMParserNativeLoader.load()) {
            System.out.println("[YsmProbe] NATIVE UNAVAILABLE");
            return null;
        }
        Path out = Path.of("C:/Users/admin/AppData/Local/Temp/ysm_probe_keep");
        Files.createDirectories(out);
        boolean ok = YSMParser.parseBytes(raw, out.toString());
        System.out.println("[YsmProbe] parseBytes=" + ok);
        if (!ok) return null;

        System.out.println("[YsmProbe] --- native output tree ---");
        try (Stream<Path> stream = Files.walk(out)) {
            stream.sorted().forEach(p -> {
                if (Files.isRegularFile(p)) {
                    try {
                        System.out.println("[YsmProbe]   " + out.relativize(p).toString().replace('\\', '/')
                                + "  (" + Files.size(p) + " bytes)");
                    } catch (Exception ignored) {}
                }
            });
        }

        Map<String, byte[]> flat = new HashMap<>();
        if (Files.isDirectory(out.resolve("models"))) {
            for (String n : new String[]{"main.json", "arm.json"}) {
                Path p = out.resolve("models").resolve(n);
                if (Files.isRegularFile(p)) flat.put(n, Files.readAllBytes(p));
            }
        } else {
            for (String n : new String[]{"main.json", "arm.json"}) {
                Path p = out.resolve(n);
                if (Files.isRegularFile(p)) flat.put(n, Files.readAllBytes(p));
            }
        }
        if (Files.isDirectory(out.resolve("textures"))) {
            try (Stream<Path> stream = Files.list(out.resolve("textures"))) {
                stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".png"))
                        .forEach(p -> {
                            try { flat.put(p.getFileName().toString(), Files.readAllBytes(p)); } catch (Exception ignored) {}
                        });
            }
        }
        for (String n : new String[]{"main.animation.json", "arm.animation.json", "extra.animation.json", "slashblade.animation.json", "tac.animation.json", "tlm.animation.json", "carryon.animation.json", "parcool.animation.json"}) {
            Path p = out.resolve("animations").resolve(n);
            if (Files.isRegularFile(p)) flat.put(n, Files.readAllBytes(p));
        }

        System.out.println("[YsmProbe] flat map keys=" + flat.keySet());

        // Inspect main.json header
        byte[] mainBytes = flat.get("main.json");
        if (mainBytes != null) {
            String head = new String(mainBytes, 0, Math.min(80, mainBytes.length), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[YsmProbe] main.json head: " + head.replace("\n", "\\n").replace("\r", ""));
        }

        try (YSMFolderDeserializer d = new YSMFolderDeserializer(flat)) {
            return d.deserialize();
        }
    }
}
