package info.mudbourn.mmsweapons.client.focus;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

// Spawns a focused weapon's black smoke from where the weapon is actually drawn in the hand, so it follows every pose.
// The hand layer marks the weapon it is about to draw; once the item's display transform is applied, the blade
// segment in model space is carried to world space and smoke is put along it.
public final class FocusSmokeClient {

    private static final DustParticleOptions PARTICLE = new DustParticleOptions(0x101010, 0.6F);
    private static final double SPACING = 0.2;
    private static final int INTERVAL_TICKS = 2;
    private static final ThreadLocal<ItemStack> DRAWING = new ThreadLocal<>();
    private static final Map<ItemStack, Long> LAST_SPAWN = new WeakHashMap<>();

    private FocusSmokeClient() {
    }

    // Called as the hand layer starts drawing a main-hand weapon.
    public static void beginHand(ItemStack stack) {
        DRAWING.set(Focus.showsSmoke(stack) ? stack : null);
    }

    public static void endHand() {
        DRAWING.remove();
    }

    // Called with the item's model-space matrix, which here is still relative to the camera.
    public static void atModel(Matrix4f pose) {
        ItemStack stack = DRAWING.get();
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (stack == null || level == null || client.isPaused()) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_SPAWN.get(stack);
        if (last != null && now - last < INTERVAL_TICKS) {
            return;
        }
        LAST_SPAWN.put(stack, now);
        Focus.Blade blade = Focus.blade(stack);
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        Vec3 previous = null;
        for (Vec3 point : blade.points()) {
            Vector3f moved = pose.transformPosition(new Vector3f((float) point.x, (float) point.y, (float) point.z));
            Vec3 at = camera.add(moved.x, moved.y, moved.z);
            if (previous != null) {
                spawnAlong(level, previous, at);
            }
            previous = at;
        }
    }

    private static void spawnAlong(ClientLevel level, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / SPACING));
        for (int i = 0; i <= samples; i++) {
            Vec3 at = from.lerp(to, i / (double) samples);
            level.addParticle(PARTICLE,
                    at.x + (level.random.nextDouble() - 0.5) * 0.06,
                    at.y + (level.random.nextDouble() - 0.5) * 0.06,
                    at.z + (level.random.nextDouble() - 0.5) * 0.06,
                    0.0, 0.01, 0.0);
        }
    }
}
