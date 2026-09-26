package net.camsnap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class CamSnapCameraMarker {
    private static int nextEntityId = Integer.MIN_VALUE;

    private static ClientLevel markerLevel;
    private static Mannequin marker;

    private CamSnapCameraMarker() {
    }

    public static void update(Minecraft client) {
        CamSnapSlotManager.CameraSlot slot = CamSnapSlotManager.getInstance().getActiveSlot();
        if (client.level == null || !slot.isCaptured()) {
            remove();
            return;
        }

        int chunkX = (int) Math.floor(slot.getX()) >> 4;
        int chunkZ = (int) Math.floor(slot.getZ()) >> 4;
        if (!client.level.hasChunk(chunkX, chunkZ)) {
            remove();
            return;
        }

        if (markerLevel != client.level || marker == null || marker.isRemoved()) {
            remove();
            markerLevel = client.level;
            marker = EntityTypes.MANNEQUIN.create(markerLevel, EntitySpawnReason.COMMAND);
            if (marker == null) {
                markerLevel = null;
                throw new IllegalStateException("Minecraft could not create CamSnap's camera marker");
            }

            marker.setId(nextEntityId++);
            marker.noPhysics = true;
            marker.setNoGravity(true);
            marker.setSilent(true);
            marker.setCustomNameVisible(true);
            markerLevel.addEntity(marker);
        }

        Pose pose = slot.getPose();
        marker.setPose(pose);
        marker.setPos(slot.getX(), slot.getY() - marker.getEyeHeight(pose), slot.getZ());
        marker.setDeltaMovement(Vec3.ZERO);
        marker.setYRot(slot.getYaw());
        marker.setYBodyRot(slot.getYaw());
        marker.setYHeadRot(slot.getYaw());
        marker.setXRot(slot.getPitch());
        marker.setCustomName(Component.literal(slot.getDisplayName()));
        CamSnapSkinManager.applySelectedSkin(client, marker);
    }

    public static boolean isInBlock(ClientLevel level, CamSnapSlotManager.CameraSlot slot) {
        int chunkX = (int) Math.floor(slot.getX()) >> 4;
        int chunkZ = (int) Math.floor(slot.getZ()) >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) {
            return false;
        }

        Pose pose = slot.getPose();
        EntityDimensions dimensions = marker != null && markerLevel == level
                ? marker.getDimensions(pose)
                : EntityTypes.MANNEQUIN.getDimensions();
        double eyeHeight = marker != null && markerLevel == level
                ? marker.getEyeHeight(pose)
                : dimensions.eyeHeight();
        double baseY = slot.getY() - eyeHeight;
        AABB bounds = dimensions.makeBoundingBox(slot.getX(), baseY, slot.getZ());
        return level.getBlockCollisionsFromContext(CollisionContext.empty(), bounds).iterator().hasNext();
    }

    public static void remove() {
        if (marker != null && !marker.isRemoved()) {
            marker.remove(Entity.RemovalReason.DISCARDED);
        }
        marker = null;
        markerLevel = null;
    }
}
