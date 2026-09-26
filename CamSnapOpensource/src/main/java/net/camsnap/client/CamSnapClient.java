package net.camsnap.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CamSnapClient implements ClientModInitializer {
    public static final String MOD_ID = "camsnap";

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "general")
    );

    private static KeyMapping saveToggleKey;
    private static KeyMapping cyclePositionKey;
    private static KeyMapping cycleSlotKey;
    private static final KeyMapping[] slotKeys = new KeyMapping[CamSnapSlotManager.TOTAL_SLOTS];
    private static KeyMapping zoomInKey;
    private static KeyMapping zoomOutKey;
    private static KeyMapping toggleGridKey;
    private static KeyMapping toggleNightVisionKey;
    private static KeyMapping toggleDaylightKey;
    private static KeyMapping openEditorKey;
    private static KeyMapping scaleUpKey;
    private static KeyMapping scaleDownKey;
    private static KeyMapping deleteCameraKey;

    @Override
    public void onInitializeClient() {
        CamSnapStorageManager.getInstance().initDirectories();
        CamSnapConfig.getInstance();
        registerKeyBindings();
        registerTickEvents();
        registerConnectionEvents();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (graphics, deltaTracker) -> CamSnapOverlay.getInstance().renderOverlay(graphics, deltaTracker)
        );
    }

    private void registerKeyBindings() {
        saveToggleKey = register("key.camsnap.save_toggle", InputConstants.KEY_K);
        cyclePositionKey = register("key.camsnap.cycle_position", InputConstants.KEY_J);
        cycleSlotKey = register("key.camsnap.cycle_slot", InputConstants.KEY_C);

        int[] defaults = {
                InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
                InputConstants.KEY_4, InputConstants.KEY_5
        };
        for (int i = 0; i < slotKeys.length; i++) {
            slotKeys[i] = register("key.camsnap.slot_" + (i + 1), defaults[i]);
        }

        zoomInKey = register("key.camsnap.zoom_in", InputConstants.KEY_LBRACKET);
        zoomOutKey = register("key.camsnap.zoom_out", InputConstants.KEY_RBRACKET);
        toggleGridKey = register("key.camsnap.toggle_grid", InputConstants.KEY_G);
        toggleNightVisionKey = register("key.camsnap.night_vision", InputConstants.KEY_N);
        toggleDaylightKey = register("key.camsnap.toggle_daylight", InputConstants.KEY_L);
        scaleUpKey = register("key.camsnap.scale_up", InputConstants.KEY_PAGEUP);
        scaleDownKey = register("key.camsnap.scale_down", InputConstants.KEY_PAGEDOWN);
        deleteCameraKey = register("key.camsnap.delete_camera", InputConstants.KEY_DELETE);
        openEditorKey = register("key.camsnap.open_editor", InputConstants.KEY_F8);
    }

    private static KeyMapping register(String translationKey, int key) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYBOARD, key, KEY_CATEGORY)
        );
    }

    private void registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) {
                return;
            }

            CamSnapManager manager = CamSnapManager.getInstance();
            CamSnapSlotManager slots = CamSnapSlotManager.getInstance();

            while (saveToggleKey.consumeClick()) {
                if (!manager.isActive()) {
                    if (slots.saveToSlot(slots.getActiveSlotIndex(), player)) {
                        client.gui.hud.setOverlayMessage(Component.literal(
                                "§a[CamSnap] " + slots.getActiveSlot().getDisplayName() + " saved; camera card §2§lON"
                        ), true);
                    }
                } else {
                    manager.setActive(false);
                    client.gui.hud.setOverlayMessage(Component.literal("§c[CamSnap] Camera card §4§lOFF"), true);
                }
            }

            while (cyclePositionKey.consumeClick()) {
                CamSnapManager.ScreenPosition position = manager.cyclePosition();
                client.gui.hud.setOverlayMessage(Component.literal("§b[CamSnap] Position preset: §e" + position.getDisplayName()), true);
            }

            while (cycleSlotKey.consumeClick()) {
                CamSnapSlotManager.CameraSlot slot = slots.cycleSlot();
                client.gui.hud.setOverlayMessage(Component.literal("§b[CamSnap] Active Slot: §f" + slot.getDisplayName()
                        + (slot.isCaptured() ? " §a(Active)" : " §7(Empty)")), true);
            }

            boolean altDown = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RALT);
            for (int i = 0; i < slotKeys.length; i++) {
                while (slotKeys[i].consumeClick()) {
                    if (altDown) {
                        slots.saveToSlot(i, player);
                        client.gui.hud.setOverlayMessage(Component.literal(
                                "§a[CamSnap] Saved perspective to §e" + slots.getSlot(i).getDisplayName()
                        ), true);
                    } else {
                        CamSnapSlotManager.CameraSlot slot = slots.setActiveSlotIndex(i);
                        client.gui.hud.setOverlayMessage(Component.literal("§b[CamSnap] Switched to §f" + slot.getDisplayName()
                                + (slot.isCaptured() ? " §a(Active)" : " §7(Empty)")), true);
                    }
                }
            }

            while (zoomInKey.consumeClick()) {
                slots.getActiveSlot().adjustFov(-5.0f);
                client.gui.hud.setOverlayMessage(Component.literal(String.format("§e[CamSnap] FOV: §f%.0f°", slots.getActiveSlot().getFov())), true);
            }
            while (zoomOutKey.consumeClick()) {
                slots.getActiveSlot().adjustFov(5.0f);
                client.gui.hud.setOverlayMessage(Component.literal(String.format("§e[CamSnap] FOV: §f%.0f°", slots.getActiveSlot().getFov())), true);
            }
            while (scaleUpKey.consumeClick()) {
                manager.adjustScale(0.02f);
                client.gui.hud.setOverlayMessage(Component.literal("§e[CamSnap] HUD Scale: §f"
                        + (int) (manager.getScreenScalePercent() * 100) + "%"), true);
            }
            while (scaleDownKey.consumeClick()) {
                manager.adjustScale(-0.02f);
                client.gui.hud.setOverlayMessage(Component.literal("§e[CamSnap] HUD Scale: §f"
                        + (int) (manager.getScreenScalePercent() * 100) + "%"), true);
            }
            while (toggleGridKey.consumeClick()) {
                CamSnapGridRenderer.GridMode mode = CamSnapGridRenderer.getInstance().cycleMode();
                client.gui.hud.setOverlayMessage(Component.literal("§d[CamSnap] Grid Mode: §f" + mode.getDisplayName()), true);
            }
            while (toggleNightVisionKey.consumeClick()) {
                boolean enabled = CamSnapEnhancer.getInstance().toggleFullbright();
                client.gui.hud.setOverlayMessage(Component.literal("§6[CamSnap] Night-Vision: "
                        + (enabled ? "§a§lENABLED" : "§c§lDISABLED")), true);
            }
            while (toggleDaylightKey.consumeClick()) {
                boolean enabled = CamSnapEnhancer.getInstance().toggleLockDaylight();
                client.gui.hud.setOverlayMessage(Component.literal("§6[CamSnap] Daylight Preview: "
                        + (enabled ? "§e§lLOCKED" : "§7§lOFF")), true);
            }
            while (openEditorKey.consumeClick()) {
                client.setScreenAndShow(new CamSnapGuiEditor(null));
            }

            while (deleteCameraKey.consumeClick()) {
                if (slots.deleteActiveSlot()) {
                    client.gui.hud.setOverlayMessage(Component.literal("§c[CamSnap] Deleted camera in "
                            + slots.getActiveSlot().getDisplayName()), true);
                } else {
                    client.gui.hud.setOverlayMessage(Component.literal("§7[CamSnap] Active camera slot is empty"), true);
                }
            }
            CamSnapCameraMarker.update(client);
        });
    }

    private void registerConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                CamSnapSlotManager.getInstance().loadPositionsFromDisk());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CamSnapStorageManager.getInstance().savePositions(CamSnapSlotManager.getInstance().getSlots());
            CamSnapCameraMarker.remove();
        });
    }

    public static KeyMapping getSaveToggleKey() {
        return saveToggleKey;
    }

    public static KeyMapping getCyclePositionKey() {
        return cyclePositionKey;
    }

    public static KeyMapping getCycleSlotKey() {
        return cycleSlotKey;
    }
}
