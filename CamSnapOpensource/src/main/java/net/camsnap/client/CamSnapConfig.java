package net.camsnap.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Configuration holder and persistence manager for CamSnap.
 * Stores the active camera-card layout and status display settings.
 * Target: Minecraft 26.3 / Fabric Client / Java 25.
 */
public final class CamSnapConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "camsnap.json");

    private static CamSnapConfig INSTANCE;

    // Config Options
    public boolean enableOverlay = true;
    public CamSnapManager.ScreenPosition defaultPosition = CamSnapManager.ScreenPosition.TOP_RIGHT;
    public float hudScale = 0.16f; // 8% to 40% of screen width
    public boolean lockDaylight = false; // Fixed daylight preview
    public boolean showDistanceWhenUnloaded = true;
    public boolean showCameraDetails = true;
    public boolean[] additionalVisibleSlots = new boolean[CamSnapSlotManager.TOTAL_SLOTS];
    public String mannequinSkin = CamSnapSkinManager.DEFAULT_SKIN;
    public int borderColor = 0xFF333333; // Default #333333 with full alpha

    public static CamSnapConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    /**
     * Loads configuration from config/camsnap.json or defaults if not present.
     */
    public static CamSnapConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                CamSnapConfig config = GSON.fromJson(reader, CamSnapConfig.class);
                if (config != null) {
                    config.validate();
                    config.applyToRuntime();
                    return config;
                }
            } catch (Exception e) {
                System.err.println("[CamSnap] Failed to load config from " + CONFIG_FILE.getName() + ", restoring defaults. " + e.getMessage());
            }
        }
        CamSnapConfig defaultConfig = new CamSnapConfig();
        defaultConfig.save();
        defaultConfig.applyToRuntime();
        return defaultConfig;
    }

    /**
     * Persists current settings to disk and synchronizes active mod singletons.
     */
    public void save() {
        validate();
        applyToRuntime();
        try {
            File parentDir = CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[CamSnap] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Validates values within allowed limits.
     */
    public void validate() {
        if (defaultPosition == null) {
            defaultPosition = CamSnapManager.ScreenPosition.TOP_RIGHT;
        }
        hudScale = Math.clamp(hudScale, 0.08f, 0.40f);
        if (additionalVisibleSlots == null
                || additionalVisibleSlots.length != CamSnapSlotManager.TOTAL_SLOTS) {
            boolean[] validSlots = new boolean[CamSnapSlotManager.TOTAL_SLOTS];
            if (additionalVisibleSlots != null) {
                System.arraycopy(additionalVisibleSlots, 0, validSlots, 0,
                        Math.min(additionalVisibleSlots.length, validSlots.length));
            }
            additionalVisibleSlots = validSlots;
        }
    }

    /**
     * Instantly pushes loaded config values to active runtime singletons.
     */
    public void applyToRuntime() {
        CamSnapManager manager = CamSnapManager.getInstance();
        manager.setPositionPreset(this.defaultPosition);
        manager.setScreenScalePercent(this.hudScale);
        manager.setActive(this.enableOverlay);

        CamSnapEnhancer.getInstance().setLockDaylight(this.lockDaylight);

    }

    /**
     * Creates a Cloth Config GUI screen.
     */
    public static Screen createClothConfigScreen(Screen parent) {
        CamSnapConfig config = getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("CamSnap Configuration"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 1. Display Settings Category
        ConfigCategory displayCategory = builder.getOrCreateCategory(Component.literal("Display Settings"));

        displayCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Overlay"), config.enableOverlay)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Toggle whether the saved camera position card is active."))
                .setSaveConsumer(val -> {
                    config.enableOverlay = val;
                    CamSnapManager.getInstance().setActive(val);
                })
                .build());

        displayCategory.addEntry(entryBuilder.startEnumSelector(Component.literal("Default Screen Position"), CamSnapManager.ScreenPosition.class, config.defaultPosition)
                .setDefaultValue(CamSnapManager.ScreenPosition.TOP_RIGHT)
                .setTooltip(Component.literal("Default snap position on the HUD grid."))
                .setSaveConsumer(val -> {
                    config.defaultPosition = val;
                    CamSnapManager.getInstance().setPositionPreset(val);
                })
                .build());

        displayCategory.addEntry(entryBuilder.startIntSlider(Component.literal("HUD Scale Percentage"), (int) (config.hudScale * 100), 8, 40)
                .setDefaultValue(16)
                .setTooltip(Component.literal("Scale of the camera overlay relative to total screen width (8% to 40%)."))
                .setSaveConsumer(val -> config.hudScale = val / 100.0f)
                .build());

        displayCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Camera Details"), config.showCameraDetails)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show coordinates, direction, FOV, and target below the camera view."))
                .setSaveConsumer(val -> config.showCameraDetails = val)
                .build());

        List<String> availableSkins = CamSnapSkinManager.getAvailableSkins();
        if (!availableSkins.contains(config.mannequinSkin)) {
            config.mannequinSkin = CamSnapSkinManager.DEFAULT_SKIN;
        }
        String[] skinOptions = availableSkins.toArray(String[]::new);
        displayCategory.addEntry(entryBuilder.startSelector(
                        Component.literal("Camera Mannequin Skin"), skinOptions, config.mannequinSkin)
                .setDefaultValue(CamSnapSkinManager.DEFAULT_SKIN)
                .setNameProvider(Component::literal)
                .setTooltip(Component.literal("Choose a PNG from .minecraft/camsnap/skinsforcamera. "
                        + "If no skin is selected or available, the mannequin uses the default Steve skin."))
                .setSaveConsumer(val -> config.mannequinSkin = val)
                .build());

        displayCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Daylight Marker (Noon)"), config.lockDaylight)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Adds a daylight marker to the saved camera position card."))
                .setSaveConsumer(val -> {
                    config.lockDaylight = val;
                    CamSnapEnhancer.getInstance().setLockDaylight(val);
                })
                .build());

        displayCategory.addEntry(entryBuilder.startAlphaColorField(Component.literal("Border Color"), config.borderColor)
                .setDefaultValue(0xFF333333)
                .setTooltip(Component.literal("Hex color for the outer camera position card border."))
                .setSaveConsumer(val -> config.borderColor = val)
                .build());

        // 2. Warning Overlay Category
        ConfigCategory warningCategory = builder.getOrCreateCategory(Component.literal("Warning Overlay"));

        warningCategory.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Block Distance when Unloaded"), config.showDistanceWhenUnloaded)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Display block distance to camera coordinates when chunks are unloaded."))
                .setSaveConsumer(val -> config.showDistanceWhenUnloaded = val)
                .build());

        builder.setSavingRunnable(config::save);
        return builder.build();
    }
}
