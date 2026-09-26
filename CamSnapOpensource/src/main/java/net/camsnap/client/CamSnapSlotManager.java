package net.camsnap.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-camera slot manager for CamSnap Advanced Builder Toolkit.
 * Supports up to 5 distinct camera viewpoints, individual FOV/telephoto settings,
 * per-slot FOV/status values, and world-based disk persistence.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapSlotManager {

    public static final int TOTAL_SLOTS = 5;
    private static final CamSnapSlotManager INSTANCE = new CamSnapSlotManager();

    private final List<CameraSlot> slots = new ArrayList<>(TOTAL_SLOTS);
    private int activeSlotIndex = 0; // 0 to 4 (Slots 1 to 5)

    private CamSnapSlotManager() {
        for (int i = 1; i <= TOTAL_SLOTS; i++) {
            slots.add(new CameraSlot(i, "Slot " + i));
        }
    }

    public static CamSnapSlotManager getInstance() {
        return INSTANCE;
    }

    /**
     * Camera slot representation storing location, orientation, FOV, and visual settings.
     */
    public static class CameraSlot {
        private final int id;
        private String name;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private float fov = 70.0f;           // Telephoto (10.0f) to Wide Angle (110.0f)
        private boolean nightVision = false;
        private boolean crouching;
        private boolean captured = false;

        public CameraSlot(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public void captureFrom(LocalPlayer player) {
            if (player == null) return;
            this.x = player.getX();
            this.y = player.getEyeY();
            this.z = player.getZ();
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
            this.crouching = player.isCrouching();
            this.captured = true;
        }

        public void setCameraData(double x, double y, double z, float yaw, float pitch, float fov,
                                  boolean nightVision, boolean crouching, boolean captured) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fov = fov;
            this.nightVision = nightVision;
            this.crouching = crouching;
            this.captured = captured;
        }

        public void clear() {
            x = 0.0;
            y = 0.0;
            z = 0.0;
            yaw = 0.0f;
            pitch = 0.0f;
            nightVision = false;
            crouching = false;
            captured = false;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public float getFov() {
            return fov;
        }

        public void setFov(float fov) {
            this.fov = Math.clamp(fov, 10.0f, 110.0f);
        }

        public void adjustFov(float delta) {
            setFov(this.fov + delta);
        }

        public boolean isNightVision() {
            return nightVision;
        }

        public void setNightVision(boolean nightVision) {
            this.nightVision = nightVision;
        }

        public boolean toggleNightVision() {
            this.nightVision = !this.nightVision;
            return this.nightVision;
        }

        public boolean isCaptured() {
            return captured;
        }

        public boolean isCrouching() {
            return crouching;
        }

        public Pose getPose() {
            return crouching ? Pose.CROUCHING : Pose.STANDING;
        }

        public String getDisplayName() {
            return "CAM " + id + " - " + name;
        }
    }

    public List<CameraSlot> getSlots() {
        return slots;
    }

    public CameraSlot getSlot(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) {
            return slots.get(0);
        }
        return slots.get(index);
    }

    public CameraSlot getActiveSlot() {
        return slots.get(activeSlotIndex);
    }

    public int getActiveSlotIndex() {
        return activeSlotIndex;
    }

    /**
     * Switches the active HUD camera card to the designated slot index (0 to 4).
     */
    public CameraSlot setActiveSlotIndex(int index) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            this.activeSlotIndex = index;
            syncToCamSnapManager();
        }
        return getActiveSlot();
    }

    /**
     * Cycles to the next camera slot (1 to 5).
     */
    public CameraSlot cycleSlot() {
        this.activeSlotIndex = (this.activeSlotIndex + 1) % TOTAL_SLOTS;
        syncToCamSnapManager();
        return getActiveSlot();
    }

    /**
     * Captures current player coordinates into the specified slot (0 to 4) and saves to disk.
     */
    public boolean saveToSlot(int index, LocalPlayer player) {
        if (player == null || index < 0 || index >= TOTAL_SLOTS) {
            return false;
        }
        CameraSlot slot = slots.get(index);
        slot.captureFrom(player);
        this.activeSlotIndex = index;
        syncToCamSnapManager();

        // Persist positions to disk for current world/server
        CamSnapStorageManager.getInstance().savePositions(this.slots);
        return true;
    }

    public boolean deleteActiveSlot() {
        CameraSlot slot = getActiveSlot();
        if (!slot.isCaptured()) {
            return false;
        }

        slot.clear();
        syncToCamSnapManager();
        CamSnapStorageManager.getInstance().savePositions(slots);
        return true;
    }

    /**
     * Loads saved positions from disk for current world or server.
     */
    public void loadPositionsFromDisk() {
        boolean loaded = CamSnapStorageManager.getInstance().loadPositions(this.slots);
        if (loaded) {
            syncToCamSnapManager();
        }
    }

    /**
     * Synchronizes active slot attributes to the primary CamSnapManager singleton.
     */
    public void syncToCamSnapManager() {
        CameraSlot slot = getActiveSlot();
        CamSnapManager manager = CamSnapManager.getInstance();
        if (slot.isCaptured()) {
            manager.setCameraView(slot.getX(), slot.getY(), slot.getZ(), slot.getYaw(), slot.getPitch());
            manager.setActive(true);
        } else {
            manager.clearCapturedPoint();
        }
    }
}
