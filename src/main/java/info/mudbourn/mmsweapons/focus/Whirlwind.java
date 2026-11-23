package info.mudbourn.mmsweapons.focus;

import info.mudbourn.mmsweapons.MmsSounds;
import info.mudbourn.mmsweapons.shieldbash.ShieldWeapons;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// The focus skill of weapons that block (glaives, katanas, longswords, greatswords): a spinning swing that hits
// all around the player in three heavy waves at double the weapon's reach. Slower and harder hitting than the barrage.
public final class Whirlwind {

    public static final int WAVES = 3;
    public static final int WAVE_GAP_TICKS = 14;
    public static final float SWING_LENGTH = 14.0F;
    public static final float SWING_UPSWING = 0.4F;
    public static final String ANIMATION = "bettercombat:two_handed_spin";
    private static final int STRIKE_OFFSET_TICKS = 6;
    private static final double DAMAGE_SHARE = 1.25;
    private static final double DEFAULT_RANGE = 3.0;
    private static final double RANGE_SCALE = 2.0;

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static Consumer<Player> local = player -> {
    };

    private Whirlwind() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Whirlwind::tick);
    }

    // Whether damage with this stack builds focus for the whirlwind: a Better Combat weapon that blocks without being a shield.
    public static boolean guardWeapon(ItemStack stack) {
        return !stack.isEmpty() && WeaponRegistry.getAttributes(stack) != null && ShieldWeapons.isBlockingWeapon(stack.getItem());
    }

    public static void setLocal(Consumer<Player> spin) {
        local = spin;
    }

    // Starts a whirlwind with a full-focus weapon. Called on both sides from the skill cast.
    public static void start(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            local.accept(player);
            return;
        }
        stack.set(Focus.FOCUS, 0.0F);
        RUNS.put(player.getUUID(), new Run(level.getGameTime(), stack));
        // Steps up one-block gaps while spinning, as the barrage does.
        if (player instanceof ServerPlayer server) {
            DashStep.boost(server, (WAVES - 1) * WAVE_GAP_TICKS + (int) SWING_LENGTH + 10);
        }
    }

    // Double the weapon's own Better Combat reach.
    public static double reach(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        double range = attributes != null && attributes.attackRange() > 0.0 ? attributes.attackRange() : DEFAULT_RANGE;
        return range * RANGE_SCALE;
    }

    public static boolean active(Entity entity) {
        return entity instanceof Player player && RUNS.containsKey(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Run> entry = it.next();
            Run run = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || !ItemStack.isSameItem(player.getMainHandItem(), run.weapon)) {
                it.remove();
                continue;
            }
            long elapsed = player.level().getGameTime() - run.started;
            for (int wave = run.spun; wave < WAVES && elapsed >= (long) wave * WAVE_GAP_TICKS; wave++) {
                animate(player, run.weapon, wave);
                run.spun = wave + 1;
            }
            for (int wave = run.struck; wave < WAVES && elapsed >= (long) wave * WAVE_GAP_TICKS + STRIKE_OFFSET_TICKS; wave++) {
                strike(player, run.weapon);
                run.struck = wave + 1;
            }
            if (run.struck >= WAVES) {
                it.remove();
            }
        }
    }

    private static void animate(ServerPlayer player, ItemStack weapon, int wave) {
        Packets.AttackAnimation packet = new Packets.AttackAnimation(
                player.getId(),
                Barrage.hand(weapon),
                ANIMATION,
                SWING_LENGTH,
                SWING_UPSWING,
                (float) reach(weapon),
                (int) (SWING_LENGTH * SWING_UPSWING),
                Packets.SwingParticles.EMPTY);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, packet);
        }
        player.level().playSound(null, player.blockPosition(), MmsSounds.BARRAGE_SWING, SoundSource.PLAYERS, 1.2F, 0.7F + wave * 0.05F);
    }

    // Hits every living thing in a full circle around the player within the doubled reach.
    private static void strike(ServerPlayer player, ItemStack weapon) {
        ServerLevel level = player.level();
        double reach = reach(weapon);
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SHARE);
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        AABB box = player.getBoundingBox().inflate(reach, 1.0, reach);
        Focus.spend(() -> {
            for (Entity entity : level.getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive() && e.isPickable())) {
                LivingEntity target = (LivingEntity) entity;
                Vec3 to = target.getBoundingBox().getCenter().subtract(center);
                double flat = Math.sqrt(to.x * to.x + to.z * to.z);
                if (flat > reach + target.getBbWidth() / 2.0 || !player.hasLineOfSight(target)) {
                    continue;
                }
                target.damageCooldownTime = 0;
                target.hurtServer(level, player.damageSources().playerAttack(player), damage);
            }
        });
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            double r = reach * 0.6;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + Math.cos(angle) * r, center.y, center.z + Math.sin(angle) * r, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static final class Run {
        final long started;
        final ItemStack weapon;
        int spun;
        int struck;

        Run(long started, ItemStack weapon) {
            this.started = started;
            this.weapon = weapon;
        }
    }
}
