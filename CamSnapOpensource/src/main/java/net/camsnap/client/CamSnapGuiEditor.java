package net.camsnap.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class CamSnapGuiEditor extends Screen {
    private final Screen parent;
    private final CamSnapManager manager = CamSnapManager.getInstance();
    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;
    private boolean dragging;
    private boolean resizing;
    private double dragOffsetX;
    private double dragOffsetY;
    private static final int HANDLE_SIZE = 8;

    public CamSnapGuiEditor(Screen parent) {
        super(Component.literal("CamSnap HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CamSnapConfig config = CamSnapConfig.getInstance();
        int detailLines = config.showCameraDetails ? 3 : 0;
        int cameraCount = getSelectedCameraCount(config);
        int[] dimensions = manager.computeCurrentDimensions(width, detailLines, cameraCount);
        boxWidth = dimensions[0];
        boxHeight = dimensions[1];
        if (manager.isCustomPosition()) {
            boxX = manager.getCustomX();
            boxY = manager.getCustomY();
        } else {
            CamSnapManager.PositionCoord position = manager.getPositionPreset()
                    .computeCoordinates(width, height, boxWidth, boxHeight, 6);
            boxX = position.x();
            boxY = position.y();
        }

        int buttonWidth = 72;
        int cameraRow = height - 72;
        int firstRow = height - 48;
        int secondRow = height - 24;
        for (int i = 0; i < CamSnapSlotManager.TOTAL_SLOTS; i++) {
            int slotIndex = i;
            boolean activeSlot = slotIndex == CamSnapSlotManager.getInstance().getActiveSlotIndex();
            boolean selected = config.additionalVisibleSlots[slotIndex];
            addRenderableWidget(Button.builder(
                            Component.literal("CAM " + (slotIndex + 1) + ": "
                                    + (activeSlot ? "ACTIVE" : selected ? "ON" : "OFF")),
                            button -> {
                                if (slotIndex == CamSnapSlotManager.getInstance().getActiveSlotIndex()) {
                                    return;
                                }
                                config.additionalVisibleSlots[slotIndex] = !config.additionalVisibleSlots[slotIndex];
                                button.setMessage(Component.literal("CAM " + (slotIndex + 1) + ": "
                                        + (config.additionalVisibleSlots[slotIndex] ? "ON" : "OFF")));
                                config.save();
                            })
                    .bounds(10 + slotIndex * 76, cameraRow, buttonWidth, 20).build());
        }
        addRenderableWidget(Button.builder(
                        Component.literal("Details: " + (config.showCameraDetails ? "ON" : "OFF")),
                        button -> {
                            config.showCameraDetails = !config.showCameraDetails;
                            button.setMessage(Component.literal("Details: "
                                    + (config.showCameraDetails ? "ON" : "OFF")));
                            config.save();
                        })
                .bounds(width - 170, cameraRow, 82, 20).build());
        addPreset("Top-Left", 10, firstRow, CamSnapManager.ScreenPosition.TOP_LEFT, buttonWidth);
        addPreset("Top-Mid-L", 86, firstRow, CamSnapManager.ScreenPosition.TOP_LEFT_MID, buttonWidth);
        addPreset("Top-Mid-R", 162, firstRow, CamSnapManager.ScreenPosition.TOP_RIGHT_MID, buttonWidth);
        addPreset("Top-Right", 238, firstRow, CamSnapManager.ScreenPosition.TOP_RIGHT, buttonWidth);
        addPreset("Mid-Left", 10, secondRow, CamSnapManager.ScreenPosition.MIDDLE_LEFT, buttonWidth);
        addPreset("Bottom-Left", 86, secondRow, CamSnapManager.ScreenPosition.BOTTOM_LEFT, buttonWidth);
        addPreset("Mid-Right", 162, secondRow, CamSnapManager.ScreenPosition.MIDDLE_RIGHT, buttonWidth);
        addPreset("Bottom-Right", 238, secondRow, CamSnapManager.ScreenPosition.BOTTOM_RIGHT, buttonWidth);
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> resetDefaults())
                .bounds(width - 170, firstRow, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> saveAndClose())
                .bounds(width - 92, firstRow, 88, 20).build());
    }

    private void addPreset(String label, int x, int y, CamSnapManager.ScreenPosition preset, int buttonWidth) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> snapTo(preset))
                .bounds(x, y, buttonWidth, 20).build());
    }

    private void snapTo(CamSnapManager.ScreenPosition preset) {
        manager.setPositionPreset(preset);
        CamSnapManager.PositionCoord position = preset.computeCoordinates(width, height, boxWidth, boxHeight, 6);
        boxX = position.x();
        boxY = position.y();
    }

    private void resetDefaults() {
        boxWidth = 160;
        boxHeight = 90;
        snapTo(CamSnapManager.ScreenPosition.TOP_RIGHT);
    }

    private void saveAndClose() {
        manager.setCustomPosition(true);
        manager.setCustomX(boxX);
        manager.setCustomY(boxY);
        manager.setCustomWidth(Math.max(1, boxWidth / getSelectedCameraCount(CamSnapConfig.getInstance())));
        manager.setCustomHeight(boxHeight);
        onClose();
    }

    private int getSelectedCameraCount(CamSnapConfig config) {
        int count = 1;
        CamSnapSlotManager slots = CamSnapSlotManager.getInstance();
        for (int i = 0; i < CamSnapSlotManager.TOTAL_SLOTS; i++) {
            if (config.additionalVisibleSlots[i] && slots.getSlot(i).isCaptured()
                    && slots.getSlot(i) != slots.getActiveSlot()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x55000000);
        graphics.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        graphics.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);
        graphics.text(font, "Select extra saved cameras to show beside the active camera; details can be hidden.",
                10, height - 84, 0xFFFFFFFF, true);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xCC11141A);
        CamSnapOverlay.draw1pxBorder(graphics, boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, 0xFF4A90E2);
        graphics.text(font, "CamSnap camera position card (drag to move)", boxX + 6, boxY + 6, 0xFFFFFFFF, true);
        graphics.text(font, String.format("Pos: [%d, %d] | Size: [%d x %d]", boxX, boxY, boxWidth, boxHeight),
                boxX + 6, boxY + 18, 0xFFAAAAAA, true);
        int handleX = boxX + boxWidth - HANDLE_SIZE;
        int handleY = boxY + boxHeight - HANDLE_SIZE;
        boolean hovered = mouseX >= handleX && mouseX <= handleX + HANDLE_SIZE
                && mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE;
        graphics.fill(handleX, handleY, handleX + HANDLE_SIZE, handleY + HANDLE_SIZE,
                hovered ? 0xFFFFCC00 : 0xFF4A90E2);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            int handleX = boxX + boxWidth - HANDLE_SIZE;
            int handleY = boxY + boxHeight - HANDLE_SIZE;
            if (event.x() >= handleX && event.x() <= handleX + HANDLE_SIZE
                    && event.y() >= handleY && event.y() <= handleY + HANDLE_SIZE) {
                resizing = true;
                return true;
            }
            if (event.x() >= boxX && event.x() <= boxX + boxWidth
                    && event.y() >= boxY && event.y() <= boxY + boxHeight) {
                dragging = true;
                dragOffsetX = event.x() - boxX;
                dragOffsetY = event.y() - boxY;
                return true;
            }
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == 0) {
            if (dragging) {
                boxX = (int) Math.clamp(event.x() - dragOffsetX, 0, width - boxWidth);
                boxY = (int) Math.clamp(event.y() - dragOffsetY, 0, height - boxHeight);
                return true;
            }
            if (resizing) {
                boxWidth = (int) Math.clamp(event.x() - boxX, 120, Math.min(480, width - boxX));
                boxHeight = (int) Math.clamp(event.y() - boxY, 68, Math.min(270, height - boxY));
                return true;
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = false;
            resizing = false;
        }
        return super.mouseReleased(event);
    }
}
