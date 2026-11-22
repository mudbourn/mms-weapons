package info.mudbourn.mmsweapons.focus;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

// The focus skill: a full focus bar unleashed as a shockwave that stuns every enemy within 7 blocks for 6 seconds
// and takes a quarter of each non-boss's maximum health.
public final class FocusStun {

    private static final double RADIUS = 7.0;
    private static final int STUN_TICKS = 6 * 20;
    private static final float HEALTH_SHARE = 0.25F;

    // Bosses are stunned but take no share of their health: anything tagged c:bosses, or with 200+ max health
    // (Aerial Hell's bosses aren't tagged; its strongest regular mobs top out at 150).
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    private static final float BOSS_HEALTH = 200.0F;

    private static final Identifier STUN = Identifier.fromNamespaceAndPath("spell_engine", "stun");

    private FocusStun() {
    }

    public static void unleash(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        stack.set(Focus.FOCUS, 0.0F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, player.getSoundSource(), 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2.0 / 48.0;
            level.sendParticles(ParticleTypes.CRIT, player.getX() + Math.cos(angle) * RADIUS, player.getY() + 0.2,
                player.getZ() + Math.sin(angle) * RADIUS, 2, 0.1, 0.1, 0.1, 0.0);
        }

        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area,
            target -> target.isAlive() && target.distanceToSqr(player) <= RADIUS * RADIUS && isEnemy(player, target));
        Holder<MobEffect> stun = BuiltInRegistries.MOB_EFFECT.get(STUN).<Holder<MobEffect>>map(h -> h).orElse(MobEffects.SLOWNESS);
        Focus.spend(() -> {
            for (LivingEntity target : enemies) {
                target.addEffect(new MobEffectInstance(stun, STUN_TICKS, stun == MobEffects.SLOWNESS ? 9 : 0), player);
                if (!isBoss(target)) {
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), target.getMaxHealth() * HEALTH_SHARE);
                }
            }
        });
    }

    // Hostile mobs, and any mob currently targeting the player. Players, pets and allies are left alone.
    private static boolean isEnemy(ServerPlayer player, LivingEntity target) {
        if (target == player || target instanceof Player || player.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == player;
    }

    private static boolean isBoss(LivingEntity target) {
        return target.getType().builtInRegistryHolder().is(BOSSES) || target.getMaxHealth() >= BOSS_HEALTH;
    }
}
