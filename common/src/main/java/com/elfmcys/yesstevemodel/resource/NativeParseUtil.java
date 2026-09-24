package com.elfmcys.yesstevemodel.resource;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.ysm.parser.YSMParser;
import com.ysm.parser.YSMParserNativeLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Falls back to the native YSMParser to load newer-encrypted .ysm files that
 * the Java-only decryption path cannot read.
 *
 * <p>The native C++ YSMParser extracts the model's resources to a temporary
 * directory, which is then flattened into a {@code Map<String, byte[]>} and fed
 * into the existing {@link YSMFolderDeserializer} so the result is a normal
 * {@link RawYsmModel}.
 */
public final class NativeParseUtil {

    private NativeParseUtil() {}

    /**
     * Parse a .ysm via native YSMParser into a {@link RawYsmModel}.
     *
     * @param raw     raw bytes of the .ysm file (already read into memory)
     * @param modelId model identifier, used only for logging
     * @return the parsed model, or {@code null} if native parsing is unavailable
     *         or fails
     */
    public static RawYsmModel parseNative(byte[] raw, String modelId) {
        if (raw == null || raw.length == 0) return null;
        if (!YSMParserNativeLoader.load()) {
            YesSteveModel.LOGGER.warn("[YSM] Native YSMParser unavailable, skipping model: " + modelId);
            return null;
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("ysm_v3_");
            boolean ok = YSMParser.parseBytes(raw, tempDir.toString());
            if (!ok) {
                YesSteveModel.LOGGER.error("[YSM] YSMParser.parseBytes() failed for model: " + modelId);
                return null;
            }

            Map<String, byte[]> flat = collectFlatFiles(tempDir);
            if (flat.isEmpty()) {
                YesSteveModel.LOGGER.warn("[YSM] Native parser produced no files for model: " + modelId);
                return null;
            }

            try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(flat)) {
                return deserializer.deserialize();
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[YSM] Native parse failed for model: " + modelId, e);
            return null;
        } finally {
            if (tempDir != null) {
                try (Stream<Path> walk = Files.walk(tempDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Flatten the directory produced by YSMParser into a map keyed by the
     * RELATIVE paths that the ysm.json manifest references (e.g.
     * {@code models/main.json}, {@code textures/skin.png}). The
     * {@link YSMFolderDeserializer}'s new-format path reads resources by those
     * relative keys via {@code inMemoryFiles.get(path)}, so flattening to bare
     * filenames broke it (empty main model).
     */
    private static Map<String, byte[]> collectFlatFiles(Path parsedDir) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (Stream<Path> walk = Files.walk(parsedDir)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                String rel = parsedDir.relativize(p).toString().replace('\\', '/');
                try {
                    result.put(rel, Files.readAllBytes(p));
                } catch (IOException ignored) {
                }
            });
        }
        return result;
    }
}