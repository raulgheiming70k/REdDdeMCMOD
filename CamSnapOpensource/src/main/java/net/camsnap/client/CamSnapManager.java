package net.camsnap.client;

import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only manager for CamSnap saved camera positions,
 * snap positioning, dynamic HUD scaling (8% - 40%), and layout geometry.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapManager {

    private static final CamSnapManager INSTANCE = new CamSnapManager();

    // Active viewpoint state
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean active = false;
    private ScreenPosition positionPreset = ScreenPosition.TOP_RIGHT;

    // Viewpoint metadata
    private boolean hasCapturedPoint = false;

    // Dynamic HUD scaling (8% to 40% of screen width, default 16%)
    private float screenScalePercent = 0.16f;

    // Custom HUD position and sizing (for drag/resize editor)
    private boolean customPosition = false;
    private int customX = 20;
    private int customY = 20;
    private int customWidth = 160;
    private int customHeight = 90;

    private CamSnapManager() {
    }

    public static CamSnapManager getInstance() {
        return INSTANCE;
    }

    /**
     * Enum defining 8 HUD snap locations.
     * Excludes TOP_CENTER and BOTTOM_CENTER to ensure crosshairs and hotbar remain unobstructed.
     */
    public enum ScreenPosition {
        TOP_LEFT("Top Left"),
        TOP_RIGHT("Top Right"),
        MIDDLE_LEFT("Middle Left"),
        MIDDLE_RIGHT("Middle Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_RIGHT("Bottom Right"),
        TOP_LEFT_MID("Top Left-Mid"),
        TOP_RIGHT_MID("Top Right-Mid");

        private final String displayName;

        ScreenPosition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ScreenPosition next() {
            ScreenPosition[] positions = values();
            return positions[(this.ordinal() + 1) % positions.length];
        }

        public ScreenPosition previous() {
            ScreenPosition[] positions = values();
            return positions[(this.ordinal() - 1 + positions.length) % positions.length];
        }

        /**
         * Computes the top-left (x, y) coordinates for rendering the overlay box.
         */
        public PositionCoord computeCoordinates(int screenWidth, int screenHeight, int pipWidth, int pipHeight, int margin) {
            CamSnapManager manager = CamSnapManager.getInstance();
            if (manager.isCustomPosition()) {
                return new PositionCoord(manager.getCustomX(), manager.getCustomY());
            }

            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            int x = switch (this) {
                case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> margin;
                case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenWidth - pipWidth - margin;
                case TOP_LEFT_MID -> centerX - pipWidth - margin;
                case TOP_RIGHT_MID -> centerX + margin;
            };

            int y = switch (this) {
                case TOP_LEFT, TOP_RIGHT, TOP_LEFT_MID, TOP_RIGHT_MID -> margin;
                case MIDDLE_LEFT, MIDDLE_RIGHT -> centerY - (pipHeight / 2);
                case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - pipHeight - margin;
            };

            return new PositionCoord(x, y);
        }
    }

    public record PositionCoord(int x, int y) {}

    /**
     * Dynamic scale adjustment (clamped between 8% and 40% of screen width).
     */
    public void adjustScale(float delta) {
        this.screenScalePercent = Math.clamp(this.screenScalePercent + delta, 0.08f, 0.40f);
        this.customPosition = false; // Reset custom drag dimensions to honor proportional scale
    }

    public float getScreenScalePercent() {
        return screenScalePercent;
    }

    public void setScreenScalePercent(float screenScalePercent) {
        this.screenScalePercent = Math.clamp(screenScalePercent, 0.08f, 0.40f);
    }

    /**
     * Computes the camera preview dimensions, with optional details below the image.
     */
    public int[] computeCurrentDimensions(int screenWidth) {
        boolean showDetails = CamSnapConfig.getInstance().showCameraDetails;
        return computeCurrentDimensions(screenWidth, showDetails ? 3 : 0);
    }

    public int[] computeCurrentDimensions(int screenWidth, int detailLineCount) {
        return computeCurrentDimensions(screenWidth, detailLineCount, 1);
    }

    public int[] computeCurrentDimensions(int screenWidth, int detailLineCount, int cameraCount) {
        int maxWidth = Math.max(1, screenWidth - 2 * 6);
        int preferredCardWidth = customPosition ? customWidth : Math.round(screenWidth * screenScalePercent);
        int totalWidth = Math.min(maxWidth, Math.max(1, preferredCardWidth) * Math.max(1, cameraCount));
        int cardWidth = Math.max(1, totalWidth / Math.max(1, cameraCount));
        int imageHeight = Math.max(1, (cardWidth - 2) * 9 / 16);
        int detailHeight = detailLineCount > 0 ? detailLineCount * 10 + 5 : 0;
        int height = 17 + imageHeight + detailHeight;
        if (customPosition) {
            height = Math.max(height, customHeight);
        }
        return new int[]{totalWidth, height};
    }

    public boolean saveFromPlayer(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        this.x = player.getX();
        this.y = player.getEyeY();
        this.z = player.getZ();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.hasCapturedPoint = true;
        this.active = true;
        return true;
    }

    public void setCameraView(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.hasCapturedPoint = true;
    }

    public boolean toggleActive() {
        this.active = !this.active;
        return this.active;
    }

    public ScreenPosition cyclePosition() {
        this.customPosition = false;
        this.positionPreset = this.positionPreset.next();
        return this.positionPreset;
    }

    // Getters and Setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public ScreenPosition getPositionPreset() { return positionPreset; }
    public void setPositionPreset(ScreenPosition positionPreset) {
        if (positionPreset != null) {
            this.positionPreset = positionPreset;
            this.customPosition = false;
        }
    }

    public boolean hasCapturedPoint() { return hasCapturedPoint; }
    public void clearCapturedPoint() {
        this.hasCapturedPoint = false;
        this.active = false;
    }

    public boolean isCustomPosition() { return customPosition; }
    public void setCustomPosition(boolean customPosition) { this.customPosition = customPosition; }

    public int getCustomX() { return customX; }
    public void setCustomX(int customX) { this.customX = customX; }

    public int getCustomY() { return customY; }
    public void setCustomY(int customY) { this.customY = customY; }

    public int getCustomWidth() { return customWidth; }
    public void setCustomWidth(int customWidth) { this.customWidth = customWidth; }

    public int getCustomHeight() { return customHeight; }
    public void setCustomHeight(int customHeight) { this.customHeight = customHeight; }
}
