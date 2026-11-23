package info.mudbourn.mmsweapons.focus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Set;

// Places a dashing attacker at the right distance from the enemy in front of them before the attack's hit check runs.
// A lunge from too close carries the player into or past the target and the hit misses. So when an enemy is within
// SNAP_RANGE, the player is moved straight to the attack's ideal spacing and turned to face it, and the lunge is skipped.
public final class Engage {

    // How close, edge to edge, an enemy has to be before the player is placed instead of lunging.
    public static final double SNAP_RANGE = 5.0;
    // How far either side of the look direction an enemy may stand and still be picked, as a cosine (about 50 degrees).
    private static final double PICK_COS = Math.cos(Math.toRadians(50.0));

    private Engage() {
    }

    // Moves the player to `gap` blocks from the nearest enemy in front of them, edge to edge, facing it.
    // Returns whether the player was placed, so the caller can drop its lunge.
    public static boolean snap(ServerPlayer player, double gap) {
        LivingEntity target = target(player);
        if (target == null) {
            return false;
        }
        ServerLevel level = player.level();
        Vec3 toTarget = target.position().subtract(player.position());
        Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
        }
        flat = flat.normalize();
        double centres = target.getBbWidth() / 2.0 + player.getBbWidth() / 2.0 + gap;
        Vec3 spot = new Vec3(target.getX() - flat.x * centres, player.getY(), target.getZ() - flat.z * centres);
        AABB box = player.getBoundingBox().move(spot.subtract(player.position()));
        if (!level.noCollision(player, box)) {
            return false;
        }
        Vec3 aim = target.getBoundingBox().getCenter().subtract(spot.add(0.0, player.getEyeHeight(), 0.0));
        float yaw = (float) (Mth.atan2(aim.z, aim.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z)) * Mth.RAD_TO_DEG);
        player.teleportTo(level, spot.x, spot.y, spot.z, Set.of(), yaw, pitch, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.syncVelocity = true;
        return true;
    }

    // The enemy closest to the crosshair within SNAP_RANGE in front of the player, or null.
    private static LivingEntity target(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(SNAP_RANGE + 2.0, 1.5, SNAP_RANGE + 2.0);
        return player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> valid(player, entity))
            .stream()
            .filter(entity -> edgeDistance(player, entity) <= SNAP_RANGE)
            .filter(entity -> entity.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= PICK_COS)
            .filter(player::hasLineOfSight)
            .max(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)))
            .orElse(null);
    }

    private static boolean valid(ServerPlayer player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !entity.isSpectator() && !(entity instanceof ArmorStand)
            && !player.isPassengerOfSameVehicle(entity) && player.canAttack(entity);
    }

    // Horizontal distance between the two hitboxes' edges.
    private static double edgeDistance(ServerPlayer player, LivingEntity entity) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz) - entity.getBbWidth() / 2.0 - player.getBbWidth() / 2.0;
    }
}
