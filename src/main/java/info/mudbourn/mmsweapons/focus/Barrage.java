package info.mudbourn.mmsweapons.focus;

import info.mudbourn.mmsweapons.MmsSounds;
import info.mudbourn.mmsweapons.chainlink.ThrownChainEntity;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// The focus barrage: three quick dashing swings at 70% of the weapon's damage, for weapons with no right click of their own.
// A target who keeps a shield raised through two of the swings, instead of letting go between them, has their guard broken.
// Also cancels a throw being charged when the thrower is hit by a thrown weapon or an arrow.
public final class Barrage {

    public static final int SWINGS = 3;
    public static final int WIND_UP_TICKS = 6;
    public static final int MAX_HOLD_TICKS = 72000;
    public static final int SWING_GAP_TICKS = 6;
    public static final float SWING_LENGTH = 6.0F;
    public static final float SWING_UPSWING = 0.5F;
    private static final int STRIKE_OFFSET_TICKS = 2;
    private static final double DAMAGE_SHARE = 0.7;
    private static final double DASH_SPEED = 0.75;
    private static final double DASH_LIFT = 0.05;
    private static final double ARC_COS = Math.cos(Math.toRadians(60.0));
    private static final int GUARD_BREAK_HITS = 2;
    private static final float GUARD_BREAK_SECONDS = 5.0F;

    // Ticks after a weapon or shield parries a barrage swing in which raising it again blocks at once, with no wind-up.
    private static final int PARRY_WINDOW_TICKS = 20;

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Long> PARRIED = new HashMap<>();
    private static Consumer<Player> localBarrage = player -> {
    };
    private static boolean striking;

    private Barrage() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Barrage::tick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> interruptThrow(entity, source.getDirectEntity()));
    }

    // Plays the swings on the local player, who gets no Better Combat packet for their own attack.
    public static void setLocalBarrage(Consumer<Player> barrage) {
        localBarrage = barrage;
    }

    // Whether barrage damage is being dealt right now, so it does not refill the focus that started it.
    public static boolean striking() {
        return striking;
    }

    // Starts a barrage with a full-focus weapon. Called on both sides from the right click.
    public static void start(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            localBarrage.accept(player);
            return;
        }
        stack.set(Focus.FOCUS, 0.0F);
        RUNS.put(player.getUUID(), new Run(level.getGameTime(), stack));
    }

    // The Better Combat hand a weapon swings with.
    public static AnimatedHand hand(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        return AnimatedHand.from(false, attributes != null && attributes.isTwoHanded());
    }

    // The animation for one swing of the barrage, cycling through the weapon's own combo.
    public static String animation(ItemStack stack, int swing) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null || attributes.attacks().length == 0) {
            return "bettercombat:one_handed_slash_horizontal_right";
        }
        return attributes.attacks()[swing % attributes.attacks().length].animation();
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
            for (int swing = run.swung; swing < SWINGS && elapsed >= (long) swing * SWING_GAP_TICKS; swing++) {
                animate(player, run.weapon, swing);
                run.swung = swing + 1;
            }
            for (int swing = run.struck; swing < SWINGS && elapsed >= (long) swing * SWING_GAP_TICKS + STRIKE_OFFSET_TICKS; swing++) {
                strike(player, run);
                run.struck = swing + 1;
            }
            if (run.struck >= SWINGS) {
                it.remove();
            }
        }
    }

    private static void animate(ServerPlayer player, ItemStack weapon, int swing) {
        Packets.AttackAnimation packet = new Packets.AttackAnimation(
                player.getId(),
                hand(weapon),
                animation(weapon, swing),
                SWING_LENGTH,
                SWING_UPSWING,
                (float) player.entityInteractionRange(),
                (int) (SWING_LENGTH * SWING_UPSWING),
                Packets.SwingParticles.EMPTY);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, packet);
        }
        player.level().playSound(null, player.blockPosition(), MmsSounds.BARRAGE_SWING, SoundSource.PLAYERS, 1.0F, 1.0F + swing * 0.1F);
        // Each swing lunges the player forward along where they face.
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() > 1.0E-4) {
            flat = flat.normalize().scale(DASH_SPEED);
            player.push(flat.x, DASH_LIFT, flat.z);
            player.hurtMarked = true;
        }
    }

    // Hits everything in front of the player inside their reach, ignoring the brief invulnerability between hits.
    private static void strike(ServerPlayer player, Run run) {
        ServerLevel level = player.level();
        double reach = player.entityInteractionRange();
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SHARE);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = player.getBoundingBox().inflate(reach);
        striking = true;
        try {
            for (Entity entity : level.getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive() && e.isPickable())) {
                LivingEntity target = (LivingEntity) entity;
                Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
                if (to.length() > reach + target.getBbWidth() / 2.0 || to.normalize().dot(look) < ARC_COS || !player.hasLineOfSight(target)) {
                    continue;
                }
                boolean blocking = target.isBlocking();
                ItemStack guard = blocking ? target.getItemBlockingWith() : null;
                target.invulnerableTime = 0;
                target.hurtServer(level, player.damageSources().playerAttack(player), damage);
                if (blocking) {
                    // Any blocking item parries: weapons that block and shields alike.
                    if (guard != null) {
                        PARRIED.put(target.getUUID(), level.getGameTime() + PARRY_WINDOW_TICKS);
                    }
                    trackGuard(level, run, target);
                }
            }
        } finally {
            striking = false;
        }
        Vec3 front = eye.add(look.scale(1.2));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y - 0.3, front.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // Whether this entity parried a barrage swing with a weapon or shield recently enough to block again at once.
    public static boolean inParryWindow(LivingEntity entity) {
        Long until = PARRIED.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() > until) {
            PARRIED.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    // Counts swings blocked in one unbroken hold. Letting go and raising the shield again starts the count over.
    private static void trackGuard(ServerLevel level, Run run, LivingEntity target) {
        long now = level.getGameTime();
        int held = target.getTicksUsingItem();
        Guard guard = run.guards.get(target.getUUID());
        boolean sameHold = guard != null && held >= guard.heldAt + (now - guard.at);
        int hits = sameHold ? guard.hits + 1 : 1;
        run.guards.put(target.getUUID(), new Guard(now, held, hits));
        if (hits >= GUARD_BREAK_HITS) {
            breakGuard(level, target);
            run.guards.remove(target.getUUID());
        }
    }

    private static void breakGuard(ServerLevel level, LivingEntity target) {
        ItemStack shield = target.getItemBlockingWith();
        if (shield == null) {
            return;
        }
        BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
        if (blocks != null) {
            blocks.disable(level, target, GUARD_BREAK_SECONDS, shield);
        } else {
            target.stopUsingItem();
        }
    }

    // A hit from a thrown weapon or an arrow knocks a charging throw out of the thrower's hands.
    private static void interruptThrow(LivingEntity target, Entity direct) {
        if (!(direct instanceof AbstractArrow) && !(direct instanceof ThrownChainEntity)) {
            return;
        }
        if (!target.isUsingItem()) {
            return;
        }
        ItemStack using = target.getUseItem();
        ItemUseAnimation animation = using.getUseAnimation();
        if (animation == ItemUseAnimation.TRIDENT || animation == ItemUseAnimation.SPEAR) {
            target.stopUsingItem();
        }
    }

    // One player's barrage in progress: when it started, the weapon, how many swings have played and landed, and blocked hits per target.
    private static final class Run {
        final long started;
        final ItemStack weapon;
        final Map<UUID, Guard> guards = new HashMap<>();
        int swung;
        int struck;

        Run(long started, ItemStack weapon) {
            this.started = started;
            this.weapon = weapon;
        }
    }

    // The last blocked swing on a target: the tick it landed, how long the shield had been held then, and the blocked count.
    private record Guard(long at, int heldAt, int hits) {
    }
}
