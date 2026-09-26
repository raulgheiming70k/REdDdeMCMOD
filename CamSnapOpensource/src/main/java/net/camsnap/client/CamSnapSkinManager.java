package net.camsnap.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class CamSnapSkinManager {
    public static final String DEFAULT_SKIN = "Steve (default)";
    private static final Identifier TEXTURE_ASSET_ID =
            Identifier.fromNamespaceAndPath(CamSnapClient.MOD_ID, "camera_skin");

    private static String loadedSkinKey;
    private static String appliedProfileKey;
    private static Mannequin appliedMannequin;
    private static Identifier registeredTexturePath;

    private CamSnapSkinManager() {
    }

    public static List<String> getAvailableSkins() {
        CamSnapStorageManager storage = CamSnapStorageManager.getInstance();
        storage.initDirectories();
        Path directory = storage.getSkinsFolder().toPath();
        List<String> skins = new ArrayList<>();
        skins.add(DEFAULT_SKIN);
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(skins::add);
        } catch (IOException e) {
            System.err.println("[CamSnap] Could not scan mannequin skins folder: " + e.getMessage());
        }
        return skins;
    }

    public static String getSelectedSkin() {
        String selected = CamSnapConfig.getInstance().mannequinSkin;
        return getAvailableSkins().contains(selected) ? selected : DEFAULT_SKIN;
    }

    public static void applySelectedSkin(Minecraft client, Mannequin mannequin) {
        String selected = CamSnapConfig.getInstance().mannequinSkin;
        if (selected == null || selected.isBlank()) {
            selected = DEFAULT_SKIN;
        }
        if (DEFAULT_SKIN.equals(selected)) {
            applyDefaultProfile(mannequin);
            return;
        }

        Path skinPath = CamSnapStorageManager.getInstance().getSkinsFolder().toPath().resolve(selected);
        if (!Files.isRegularFile(skinPath)) {
            applyDefaultProfile(mannequin);
            return;
        }

        try {
            String key = skinPath.toAbsolutePath() + ":" + Files.getLastModifiedTime(skinPath).toMillis()
                    + ":" + Files.size(skinPath);
            if (mannequin == appliedMannequin && key.equals(appliedProfileKey)) {
                return;
            }
            if (!key.equals(loadedSkinKey)) {
                reloadTexture(client, skinPath);
                loadedSkinKey = key;
            }

            ClientAsset.ResourceTexture bodyTexture = new ClientAsset.ResourceTexture(TEXTURE_ASSET_ID);
            JsonObject profileJson = new JsonObject();
            profileJson.addProperty("name", "CamSnap");
            profileJson.addProperty("texture", bodyTexture.id().toString());
            profileJson.addProperty("model", PlayerModelType.WIDE.getSerializedName());
            ResolvableProfile profile = ResolvableProfile.CODEC.parse(JsonOps.INSTANCE, profileJson)
                    .resultOrPartial(message -> System.err.println("[CamSnap] Invalid mannequin skin profile: " + message))
                    .orElseThrow(() -> new IllegalStateException("Could not create mannequin skin profile"));
            mannequin.setComponent(DataComponents.PROFILE, profile);
            appliedMannequin = mannequin;
            appliedProfileKey = key;
        } catch (IOException | IllegalStateException e) {
            System.err.println("[CamSnap] Could not load mannequin skin '" + selected + "': " + e.getMessage());
            applyDefaultProfile(mannequin);
        }
    }

    private static void applyDefaultProfile(Mannequin mannequin) {
        if (mannequin != appliedMannequin || !DEFAULT_SKIN.equals(appliedProfileKey)) {
            mannequin.setComponent(DataComponents.PROFILE, Mannequin.DEFAULT_PROFILE);
            appliedMannequin = mannequin;
            appliedProfileKey = DEFAULT_SKIN;
        }
    }

    private static void reloadTexture(Minecraft client, Path skinPath) throws IOException {
        NativeImage image;
        try (InputStream input = Files.newInputStream(skinPath)) {
            image = NativeImage.read(input);
        }
        if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
            image.close();
            throw new IOException("skin PNG must be 64x64 or 64x32 pixels");
        }

        Identifier texturePath = new ClientAsset.ResourceTexture(TEXTURE_ASSET_ID).texturePath();
        if (registeredTexturePath != null) {
            client.getTextureManager().release(registeredTexturePath);
        }
        client.getTextureManager().register(texturePath,
                new DynamicTexture(() -> "CamSnap skin " + skinPath.getFileName(), image));
        registeredTexturePath = texturePath;
    }
}
