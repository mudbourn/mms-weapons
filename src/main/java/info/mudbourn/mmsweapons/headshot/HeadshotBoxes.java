package info.mudbourn.mmsweapons.headshot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Head hitboxes per entity type, relative to the bottom center of the entity's bounding box.
public final class HeadshotBoxes {

    // A head box for one entity, or null when it has no head to hit.
    @FunctionalInterface
    public interface HeadshotBox {
        AABB box(LivingEntity entity);
    }

    private static final Map<EntityType<?>, HeadshotBox> BOXES = new HashMap<>();

    static {
        BOXES.put(EntityTypes.PLAYER, HeadshotBoxes::playerHead);
        BOXES.put(EntityTypes.ZOMBIE, child(10.0, 10.0, 24.0, 0.75, 0.5));
        BOXES.put(EntityTypes.ZOMBIFIED_PIGLIN, child(10.0, 10.0, 24.0, 0.75, 0.5));
        BOXES.put(EntityTypes.HUSK, child(10.0, 10.0, 24.0, 0.75, 0.5));
        BOXES.put(EntityTypes.SKELETON, basic(10.0, 10.0, 24.0));
        BOXES.put(EntityTypes.STRAY, basic(10.0, 10.0, 24.0));
        BOXES.put(EntityTypes.CREEPER, basic(10.0, 10.0, 18.0));
        BOXES.put(EntityTypes.SPIDER, rotated(10.0, 10.0, 5.0, 7.0, false, false, true));
        BOXES.put(EntityTypes.DROWNED, basic(10.0, 10.0, 24.0));
        BOXES.put(EntityTypes.VILLAGER, noChild(basic(10.0, 9.0, 23.0)));
        BOXES.put(EntityTypes.ZOMBIE_VILLAGER, noChild(basic(10.0, 9.0, 23.0)));
        BOXES.put(EntityTypes.VINDICATOR, noChild(basic(10.0, 9.0, 23.0)));
        BOXES.put(EntityTypes.EVOKER, basic(10.0, 9.0, 23.0));
        BOXES.put(EntityTypes.PILLAGER, basic(10.0, 9.0, 23.0));
        BOXES.put(EntityTypes.ILLUSIONER, basic(10.0, 9.0, 23.0));
        BOXES.put(EntityTypes.WANDERING_TRADER, basic(10.0, 9.0, 23.0));
        BOXES.put(EntityTypes.WITCH, basic(10.0, 9.0, 23.0));
        BOXES.put(EntityTypes.SHEEP, rotated(7.5, 8.0, 15.0, 9.5, false, false, true));
        BOXES.put(EntityTypes.CHICKEN, rotated(4.0, 6.0, 9.0, 5.0, true, false, true));
        BOXES.put(EntityTypes.COW, rotated(7.5, 8.0, 16.0, 10.5, true, false, true));
        BOXES.put(EntityTypes.MOOSHROOM, rotated(7.5, 8.0, 16.0, 10.5, true, false, true));
        BOXES.put(EntityTypes.PIG, rotated(10.0, 10.0, 8.0, 10.0, true, false, true));
        BOXES.put(EntityTypes.HORSE, rotated(10.0, 10.0, 26.0, 16.0, false, false, true));
        BOXES.put(EntityTypes.SKELETON_HORSE, rotated(10.0, 10.0, 26.0, 16.0, false, false, true));
        BOXES.put(EntityTypes.DONKEY, rotated(7.5, 8.0, 20.0, 13.0, false, false, true));
        BOXES.put(EntityTypes.MULE, rotated(7.5, 8.0, 21.0, 14.0, false, false, true));
        BOXES.put(EntityTypes.LLAMA, rotated(10.0, 10.0, 26.0, 10.0, false, false, true));
        BOXES.put(EntityTypes.TRADER_LLAMA, rotated(10.0, 10.0, 26.0, 10.0, false, false, true));
        BOXES.put(EntityTypes.POLAR_BEAR, rotated(9.0, 9.0, 12.0, 20.0, false, false, true));
        BOXES.put(EntityTypes.SNOW_GOLEM, basic(10.0, 10.0, 20.5));
        BOXES.put(EntityTypes.TURTLE, rotated(6.0, 5.0, 1.0, 10.0, false, false, true));
        BOXES.put(EntityTypes.IRON_GOLEM, rotated(10.0, 10.0, 33.0, 3.5, false, false, true));
        BOXES.put(EntityTypes.PHANTOM, rotated(6.0, 3.0, 1.5, 6.5, false, true, true));
        BOXES.put(EntityTypes.HOGLIN, rotated(14.0, 16.0, 7.0, 19.0, false, false, true));
        BOXES.put(EntityTypes.ZOGLIN, rotated(14.0, 16.0, 7.0, 19.0, false, false, true));
        BOXES.put(EntityTypes.PIGLIN, child(10.0, 10.0, 24.0, 0.75, 0.5));
        BOXES.put(EntityTypes.WITHER_SKELETON, basic(10.0, 10.0, 28.0));
    }

    private static Entity headshotVictim;

    private HeadshotBoxes() {
    }

    // Runs a hurt call on the target flagged as a headshot, so damage readouts can tell it apart.
    public static boolean hurtAsHeadshot(Entity target, BooleanSupplier hurt) {
        Entity previous = headshotVictim;
        headshotVictim = target;
        try {
            return hurt.getAsBoolean();
        } finally {
            headshotVictim = previous;
        }
    }

    // Whether the hurt call now running on the target is a headshot.
    public static boolean isHeadshot(Entity target) {
        return headshotVictim == target;
    }

    public static HeadshotBox get(EntityType<?> type) {
        return BOXES.get(type);
    }

    // Adds or replaces the head box for an entity type.
    public static void register(EntityType<?> type, HeadshotBox box) {
        BOXES.put(type, box);
    }

    // Whether a path from start to end passes through the entity's head, with the box grown sideways by grow.
    public static boolean hitsHead(LivingEntity entity, Vec3 start, Vec3 end, double grow) {
        HeadshotBox headBox = BOXES.get(entity.getType());
        AABB head = headBox == null ? null : headBox.box(entity);
        if (head == null) {
            return false;
        }
        AABB body = entity.getBoundingBox();
        head = head.move(body.getCenter().x, body.minY, body.getCenter().z).inflate(grow, 0.0, grow);
        return head.clip(start, end).isPresent();
    }

    private static AABB playerHead(LivingEntity entity) {
        AABB head = new AABB(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);
        double scale = 0.9375;
        if (entity.isSwimming()) {
            head = head.move(0.0, 0.1875, 0.0);
            head = head.move(Vec3.directionFromRotation(entity.getXRot(), entity.yBodyRot).normalize().scale(0.8));
        } else {
            head = head.move(0.0, entity.isShiftKeyDown() ? 1.25 : 1.5, 0.0);
        }
        return new AABB(
            head.minX * scale,
            head.minY * scale,
            head.minZ * scale,
            head.maxX * scale,
            head.maxY * scale,
            head.maxZ * scale
        );
    }

    private static HeadshotBox basic(double width, double height, double yOffset) {
        return entity -> {
            double half = width / 2.0 * 0.0625;
            return new AABB(-half, 0.0, -half, half, height * 0.0625, half).move(0.0, yOffset * 0.0625, 0.0);
        };
    }

    private static HeadshotBox child(double width, double height, double yOffset, double childScale, double yOffsetScale) {
        HeadshotBox adult = basic(width, height, yOffset);
        return entity -> {
            AABB box = adult.box(entity);
            if (!entity.isBaby()) {
                return box;
            }
            return new AABB(
                box.minX * childScale,
                box.minY * yOffsetScale,
                box.minZ * childScale,
                box.maxX * childScale,
                box.maxY * (yOffsetScale + 0.065),
                box.maxZ * childScale
            );
        };
    }

    private static HeadshotBox noChild(HeadshotBox box) {
        return entity -> entity.isBaby() ? null : box.box(entity);
    }

    private static HeadshotBox rotated(double width, double height, double yOffset, double zOffset, boolean noChild, boolean pitch, boolean yaw) {
        HeadshotBox base = basic(width, height, yOffset);
        HeadshotBox turned = entity -> base.box(entity).move(
            Vec3.directionFromRotation(pitch ? entity.getXRot() : 0.0F, yaw ? entity.yBodyRot : 0.0F)
                .normalize()
                .scale(zOffset * 0.0625)
        );
        return noChild ? noChild(turned) : turned;
    }
}
