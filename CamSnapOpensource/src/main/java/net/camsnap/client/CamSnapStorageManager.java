package net.camsnap.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage manager for CamSnap.
 * Manages filesystem structure under .minecraft/camsnap/ and persists
 * camera presets separately for singleplayer worlds and multiplayer servers.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapStorageManager {

    private static final CamSnapStorageManager INSTANCE = new CamSnapStorageManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Base root: .minecraft/camsnap/
    private final Path rootDir;
    private final File skinsFolder;
    private final File multiplayerFolder;
    private final File singleplayerFolder;
    private final File singleplayerFolderAlt; // Alias for spelling compatibility

    private CamSnapStorageManager() {
        this.rootDir = FabricLoader.getInstance().getGameDir().resolve("camsnap");
        this.skinsFolder = rootDir.resolve("skinsforcamera").toFile();
        this.multiplayerFolder = rootDir.resolve("multiplayercamerapositions").toFile();
        this.singleplayerFolder = rootDir.resolve("singleplayercameraposistions").toFile();
        this.singleplayerFolderAlt = rootDir.resolve("singleplayercamerapositions").toFile();
    }

    public static CamSnapStorageManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes required directories under .minecraft/camsnap/ at client startup.
     */
    public void initDirectories() {
        try {
            ensureDir(rootDir.toFile());
            ensureDir(skinsFolder);
            ensureDir(multiplayerFolder);
            ensureDir(singleplayerFolder);
            ensureDir(singleplayerFolderAlt);
        } catch (Exception e) {
            System.err.println("[CamSnap] Failed to initialize storage directories: " + e.getMessage());
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("[CamSnap] Initialized directory: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Data Transfer Object for persisting slot data.
     */
    public static class StoredSlotData {
        public int id;
        public String name;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float fov;
        public boolean nightVision;
        public boolean crouching;
        public boolean captured;

        public StoredSlotData() {}

        public StoredSlotData(CamSnapSlotManager.CameraSlot slot) {
            this.id = slot.getId();
            this.name = slot.getName();
            this.x = slot.getX();
            this.y = slot.getY();
            this.z = slot.getZ();
            this.yaw = slot.getYaw();
            this.pitch = slot.getPitch();
            this.fov = slot.getFov();
            this.nightVision = slot.isNightVision();
            this.crouching = slot.isCrouching();
            this.captured = slot.isCaptured();
        }

        public void applyTo(CamSnapSlotManager.CameraSlot slot) {
            slot.setName(this.name);
            slot.setCameraData(this.x, this.y, this.z, this.yaw, this.pitch, this.fov,
                    this.nightVision, this.crouching, this.captured);
        }
    }

    /**
     * Identifies current world/server target file for storing camera positions.
     */
    private File getCurrentTargetFile() {
        Minecraft client = Minecraft.getInstance();

        if (client.isLocalServer() && client.getSingleplayerServer() != null) {
            String worldName = sanitizeFilename(client.getSingleplayerServer().getServerDirectory().getFileName().toString());
            return new File(singleplayerFolder, worldName + ".json");
        }

        ServerData serverEntry = client.getCurrentServer();
        if (serverEntry != null) {
            String serverAddress = sanitizeFilename(serverEntry.ip);
            return new File(multiplayerFolder, serverAddress + ".json");
        }

        return new File(rootDir.toFile(), "default_cam_positions.json");
    }

    /**
     * Saves camera slots for current world or server.
     */
    public void savePositions(List<CamSnapSlotManager.CameraSlot> slots) {
        File targetFile = getCurrentTargetFile();
        try {
            ensureDir(targetFile.getParentFile());
            List<StoredSlotData> dataList = new ArrayList<>();
            for (CamSnapSlotManager.CameraSlot slot : slots) {
                dataList.add(new StoredSlotData(slot));
            }
            try (FileWriter writer = new FileWriter(targetFile)) {
                GSON.toJson(dataList, writer);
            }
        } catch (Exception e) {
            System.err.println("[CamSnap] Failed to save camera positions to " + targetFile.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Loads camera slots for current world or server.
     */
    public boolean loadPositions(List<CamSnapSlotManager.CameraSlot> slots) {
        File targetFile = getCurrentTargetFile();
        if (!targetFile.exists()) {
            return false;
        }

        try (FileReader reader = new FileReader(targetFile)) {
            Type listType = new TypeToken<List<StoredSlotData>>() {}.getType();
            List<StoredSlotData> loadedList = GSON.fromJson(reader, listType);
            if (loadedList != null && !loadedList.isEmpty()) {
                for (int i = 0; i < Math.min(slots.size(), loadedList.size()); i++) {
                    loadedList.get(i).applyTo(slots.get(i));
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("[CamSnap] Failed to load camera positions from " + targetFile.getName() + ": " + e.getMessage());
        }
        return false;
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public File getSkinsFolder() {
        return skinsFolder;
    }

    public File getMultiplayerFolder() {
        return multiplayerFolder;
    }

    public File getSingleplayerFolder() {
        return singleplayerFolder;
    }
}
