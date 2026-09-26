package net.camsnap.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

public final class CamSnapOverlay {
    private static final CamSnapOverlay INSTANCE = new CamSnapOverlay();
    private static final int SCREEN_MARGIN = 6;

    private net.minecraft.client.multiplayer.ClientLevel cachedLevel;
    private long cachedWorldTime = Long.MIN_VALUE;
    private double cachedX;
    private double cachedY;
    private double cachedZ;
    private float cachedYaw;
    private float cachedPitch;
    private boolean cachedLoaded;
    private String cachedLookDirection;

    private CamSnapOverlay() {
    }

    public static CamSnapOverlay getInstance() {
        return INSTANCE;
    }

    public void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        CamSnapManager manager = CamSnapManager.getInstance();
        LocalPlayer player = client.player;
        if (client.gui.hud.isHidden() || client.level == null || player == null
                || !manager.isActive() || !manager.hasCapturedPoint()) {
            return;
        }

        CamSnapConfig config = CamSnapConfig.getInstance();
        CamSnapSlotManager slots = CamSnapSlotManager.getInstance();
        List<CamSnapSlotManager.CameraSlot> cameras = new ArrayList<>();
        cameras.add(slots.getActiveSlot());
        boolean[] additionalSlots = config.additionalVisibleSlots;
        for (int i = 0; i < CamSnapSlotManager.TOTAL_SLOTS; i++) {
            CamSnapSlotManager.CameraSlot slot = slots.getSlot(i);
            if (additionalSlots[i] && slot != slots.getActiveSlot() && slot.isCaptured()) {
                cameras.add(slot);
            }
        }

        int detailLines = 0;
        if (config.showCameraDetails) {
            detailLines = 3;
            if (config.showDistanceWhenUnloaded && cameras.stream().anyMatch(slot ->
                    !client.level.hasChunk((int) Math.floor(slot.getX()) >> 4,
                            (int) Math.floor(slot.getZ()) >> 4))) {
                detailLines++;
            }
        }
        int[] dimensions = manager.computeCurrentDimensions(graphics.guiWidth(), detailLines, cameras.size());
        int width = dimensions[0];
        int height = dimensions[1];
        CamSnapManager.PositionCoord position = manager.getPositionPreset().computeCoordinates(
                graphics.guiWidth(), graphics.guiHeight(), width, height, SCREEN_MARGIN
        );
        int x = Math.clamp(position.x(), 0, Math.max(0, graphics.guiWidth() - width));
        int y = Math.clamp(position.y(), 0, Math.max(0, graphics.guiHeight() - height));

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xCC000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, config.borderColor);
        graphics.fill(x, y, x + width, y + height, 0xF5080A0E);

        int cardWidth = width / cameras.size();
        int remainder = width % cameras.size();
        int cardX = x;
        for (int i = 0; i < cameras.size(); i++) {
            int currentWidth = cardWidth + (i < remainder ? 1 : 0);
            renderCameraCard(graphics, client, player, config, cameras.get(i),
                    cardX, y, currentWidth, height);
            cardX += currentWidth;
        }
    }

    private void renderCameraCard(GuiGraphicsExtractor graphics, Minecraft client, LocalPlayer player,
                                  CamSnapConfig config, CamSnapSlotManager.CameraSlot slot,
                                  int x, int y, int width, int height) {
        boolean loaded = client.level.hasChunk((int) Math.floor(slot.getX()) >> 4,
                (int) Math.floor(slot.getZ()) >> 4);
        boolean inBlock = loaded && CamSnapCameraMarker.isInBlock(client.level, slot);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 14, 0xDD11151E);
        if (x > 0) {
            graphics.fill(x, y, x + 1, y + height, 0x664A90E2);
        }

        String label = "CAM " + slot.getId();
        String status = !loaded ? "NOT LOADED" : inBlock ? "CAM IN BLOCK" : "LOADED";
        int labelX = x + 5;
        int statusX = labelX + client.font.width(label) + 6;
        int availableStatusWidth = Math.max(1, x + width - 4 - statusX);
        String visibleStatus = client.font.plainSubstrByWidth(status, availableStatusWidth);
        graphics.text(client.font, label, labelX, y + 4, 0xFFB0B7C6, false);
        graphics.text(client.font, visibleStatus, statusX, y + 4,
                !loaded || inBlock ? 0xFFFF2D20 : 0xFF55FF55, false);

        int contentTop = y + 15;
        int contentBottom = y + height - 2;
        int imageWidth = Math.max(1, width - 2);
        int imageHeight = Math.min(Math.max(1, imageWidth * 9 / 16),
                Math.max(1, contentBottom - contentTop));
        graphics.fill(x + 1, contentTop, x + width - 1, contentTop + imageHeight, 0xFF080A0E);
        String placeholder = client.font.plainSubstrByWidth("NO LIVE 3D FEED", Math.max(1, imageWidth - 4));
        String viewStatus = !loaded ? "CAMERA NOT LOADED"
                : inBlock ? "CAMERA IN A BLOCK" : placeholder;
        viewStatus = client.font.plainSubstrByWidth(viewStatus, Math.max(1, imageWidth - 4));
        graphics.centeredText(client.font, viewStatus, x + width / 2,
                contentTop + imageHeight / 2 - 4, inBlock ? 0xFFFF5555 : 0xFF777F90);
        CamSnapGridRenderer.getInstance().renderGrid(
                graphics, x + 1, contentTop, imageWidth, imageHeight
        );

        if (!config.showCameraDetails) {
            return;
        }

        List<String> details = new ArrayList<>();
        details.add(String.format("X %.1f  Y %.1f  Z %.1f", slot.getX(), slot.getY(), slot.getZ()));
        details.add(String.format("Yaw %.0f  Pitch %.0f  FOV %.0f",
                slot.getYaw(), slot.getPitch(), slot.getFov()));
        details.add(inBlock ? "Camera is in a block" : getLookDirection(client, slot, loaded));
        if (!loaded && config.showDistanceWhenUnloaded) {
            double dx = player.getX() - slot.getX();
            double dy = player.getY() - slot.getY();
            double dz = player.getZ() - slot.getZ();
            details.add(String.format("Distance: %d blocks",
                    Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz))));
        }

        int detailsTop = contentTop + imageHeight + 3;
        graphics.fill(x + 1, detailsTop - 2, x + width - 1, detailsTop - 1, 0x664A90E2);
        int maxTextWidth = Math.max(1, width - 10);
        int detailX = x + 5;
        int detailLineHeight = client.font.lineHeight + 1;
        for (int i = 0; i < details.size(); i++) {
            String text = client.font.plainSubstrByWidth(details.get(i), maxTextWidth);
            graphics.text(client.font, text, detailX, detailsTop + i * detailLineHeight,
                    0xFFB0B7C6, false);
        }
    }

    private String getLookDirection(Minecraft client, CamSnapSlotManager.CameraSlot slot, boolean loaded) {
        long worldTime = client.level.getGameTime();
        if (cachedLevel == client.level && cachedWorldTime == worldTime
                && cachedX == slot.getX() && cachedY == slot.getY() && cachedZ == slot.getZ()
                && cachedYaw == slot.getYaw() && cachedPitch == slot.getPitch()
                && cachedLoaded == loaded) {
            return cachedLookDirection;
        }

        double yawRadians = Math.toRadians(slot.getYaw());
        double pitchRadians = Math.toRadians(slot.getPitch());
        double horizontalLook = Math.cos(pitchRadians);
        Vec3 cameraPosition = new Vec3(slot.getX(), slot.getY(), slot.getZ());
        Vec3 lookVector = new Vec3(
                -Math.sin(yawRadians) * horizontalLook,
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * horizontalLook
        );
        Vec3 directionPoint = cameraPosition.add(lookVector.scale(16.0));
        cachedLookDirection = String.format("Facing %.0f, %.0f, %.0f",
                directionPoint.x, directionPoint.y, directionPoint.z);

        if (loaded) {
            BlockHitResult target = client.level.clip(new ClipContext(
                    cameraPosition,
                    cameraPosition.add(lookVector.scale(64.0)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            ));
            if (target.getType() == HitResult.Type.BLOCK) {
                var blockPos = target.getBlockPos();
                String blockName = client.level.getBlockState(blockPos).getBlock().getName().getString();
                cachedLookDirection = String.format("Looking at %s (%d, %d, %d)",
                        blockName, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }
        }

        cachedLevel = client.level;
        cachedWorldTime = worldTime;
        cachedX = slot.getX();
        cachedY = slot.getY();
        cachedZ = slot.getZ();
        cachedYaw = slot.getYaw();
        cachedPitch = slot.getPitch();
        cachedLoaded = loaded;
        return cachedLookDirection;
    }

    public static void draw1pxBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
