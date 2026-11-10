package com.elfmcys.yesstevemodel.resource;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.format.ServerAnimationInfo;
import com.elfmcys.yesstevemodel.model.format.ServerModelData;
import com.elfmcys.yesstevemodel.model.format.ServerModelInfo;
import com.elfmcys.yesstevemodel.resource.models.MainModelInfo;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.google.gson.*;
import rip.ysm.security.YsmCrypt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

/** Small persistent catalog entries; geometry and animation bodies stay in the binary cache. */
public final class ServerMetadataCache {
    private static final int SCHEMA = 1;
    private static final Gson GSON = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
        public boolean shouldSkipClass(Class<?> type) { return false; }
        public boolean shouldSkipField(FieldAttributes field) {
            return Set.of("avatarImage", "extraAvatars", "backgroundImages").contains(field.getName());
        }
    }).create();

    private final Path directory;
    private final Path binaryDirectory;
    private final byte[] serverKey;
    private int hits;
    private int rebuilt;

    public ServerMetadataCache(Path directory, Path binaryDirectory, byte[] serverKey) {
        this.directory = directory;
        this.binaryDirectory = binaryDirectory;
        this.serverKey = serverKey.clone();
    }

    public static String fingerprint(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    public static String fingerprint(Path folder) throws IOException {
        MessageDigest digest = digest();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(folder)) {
            files = stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> folder.relativize(p).toString().replace('\\', '/')))
                    .toList();
        }
        for (Path file : files) {
            byte[] name = folder.relativize(file).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8);
            byte[] content = Files.readAllBytes(file);
            digest.update(ByteBuffer.allocate(4).putInt(name.length).array());
            digest.update(name);
            digest.update(ByteBuffer.allocate(8).putLong(content.length).array());
            digest.update(content);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    public ServerModelData load(String fingerprint, String modelId, boolean auth, Set<String> validBinaryFiles) {
        try {
            Path path = directory.resolve(fingerprint + ".json");
            if (!Files.isRegularFile(path)) return miss();
            Entry entry;
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                entry = GSON.fromJson(reader, Entry.class);
            }
            if (entry == null || entry.schema != SCHEMA || !fingerprint.equals(entry.fingerprint)
                    || entry.properties == null || entry.properties.sha256 == null
                    // Legacy folder models use a 32-digit content ID in this field.
                    || !entry.properties.sha256.matches("[0-9a-f]{32}(?:[0-9a-f]{32})?")) return miss();
            long[] hashes = YsmCrypt.calculateModelHashes(entry.properties.sha256, serverKey);
            String binaryName = String.format("%016x%016x", hashes[0], hashes[1]);
            Path binary = binaryDirectory.resolve(binaryName);
            if (!Files.isRegularFile(binary)
                    || !YsmCrypt.verifyServerCache(Files.readAllBytes(binary), hashes[0], hashes[1])) return miss();

            RawYsmModel seed = new RawYsmModel();
            seed.metadata = entry.metadata;
            seed.properties = entry.properties;
            seed.footer = entry.footer;
            ServerModelInfo basic = YSMClientMapper.buildModelInfo(seed);
            ServerModelInfo info = new ServerModelInfo(basic.getExtraInfo(), basic.getModelProperties(),
                    new MainModelInfo(entry.bones, entry.cubes, entry.faces), basic.getFormatVersion(),
                    basic.getModelHash(), basic.getExtra(), basic.getTimestamp(), basic.getRand());
            ServerModelData data = new ServerModelData(modelId,
                    new ServerAnimationInfo(entry.animations, entry.textures),
                    entry.projectiles, entry.vehicles, info,
                    "misc/2_steve".equals(modelId) || "misc/1_alex".equals(modelId), auth);
            validBinaryFiles.add(binaryName);
            hits++;
            return data;
        } catch (Exception invalidCache) {
            YesSteveModel.LOGGER.debug("[YSM] Rebuilding invalid source cache entry: {}", modelId);
            return miss();
        }
    }

    private ServerModelData miss() { rebuilt++; return null; }

    public void store(String fingerprint, RawYsmModel raw, ServerModelData data) {
        Path temporary = null;
        try {
            Entry entry = new Entry();
            entry.schema = SCHEMA;
            entry.fingerprint = fingerprint;
            entry.metadata = raw.metadata;
            entry.properties = raw.properties;
            entry.footer = raw.footer;
            MainModelInfo info = data.getLoadedModelData().getMainModelInfo();
            entry.bones = info.bones(); entry.cubes = info.cubes(); entry.faces = info.faces();
            entry.animations = new LinkedHashMap<>();
            data.getModelInfo().getAnimations().forEach((name, values) ->
                    entry.animations.put(name, values.toArray(String[]::new)));
            entry.textures = data.getModelInfo().getTextures().toArray(String[]::new);
            entry.projectiles = raw.projectiles.values().stream()
                    .map(v -> v.matchIds != null ? v.matchIds : new String[] {v.identifier}).toArray(String[][]::new);
            entry.vehicles = raw.vehicles.values().stream()
                    .map(v -> v.matchIds != null ? v.matchIds : new String[] {v.identifier}).toArray(String[][]::new);
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, fingerprint, ".tmp");
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(entry, writer); }
            Path target = directory.resolve(fingerprint + ".json");
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception writeFailure) {
            YesSteveModel.LOGGER.debug("[YSM] Cannot persist source metadata cache: {}", data.getModelId());
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }

    public int hits() { return hits; }
    public int rebuilt() { return rebuilt; }

    private static final class Entry {
        int schema;
        String fingerprint;
        RawYsmModel.RawMetadata metadata;
        RawYsmModel.RawProperties properties;
        RawYsmModel.RawFooter footer;
        int bones, cubes, faces;
        Map<String, String[]> animations;
        String[] textures;
        String[][] projectiles, vehicles;
    }
}
