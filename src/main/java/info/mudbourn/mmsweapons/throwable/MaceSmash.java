package info.mudbourn.mmsweapons.throwable;

import java.util.function.Predicate;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The mace's falling smash attack, lent to tagged weapons.
 *
 * <p>This exists because of Density. Breach and Wind Burst are generic — an
 * armour-effectiveness modifier and a post-attack effect, both of which fire for
 * any weapon whose {@code supported_items} tag accepts the enchantment. Density
 * is not: it only writes {@code smash_damage_per_fallen_block}, a component that
 * nothing anywhere reads except the mace's own smash calculation. Tagging a
 * hammer as mace-enchantable without this would let players spend levels on a
 * enchantment that does exactly nothing.
 *
 * <p>The numbers are vanilla's, unchanged: the same 1.5-block threshold, the
 * same three-segment damage curve, the same 3.5-block knockback burst. A hammer
 * that smashes for less than a mace would be its own balance question; matching
 * is the one choice that needs no justification beyond "it is the mace attack".
 */
public final class MaceSmash {

    private MaceSmash() {
    }

    public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
    private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
    public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
    private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

    public static boolean isSmashWeapon(Item item) {
        return item.builtInRegistryHolder().is(MmsThrowables.MACE_SMASH_WEAPONS);
    }

    public static boolean canSmashAttack(LivingEntity attacker) {
        return attacker.fallDistance > SMASH_ATTACK_FALL_THRESHOLD && !attacker.isFallFlying();
    }

    /** Mirrors {@code MaceItem.hurtEnemy}: kill the attacker's fall, make noise, shove the crowd. */
    public static void hurtEnemy(LivingEntity target, LivingEntity attacker) {
        if (!canSmashAttack(attacker)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) attacker.level();
        attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
        if (attacker instanceof ServerPlayer serverPlayer) {
            serverPlayer.currentImpulseImpactPos = impactPosition(serverPlayer);
            serverPlayer.setIgnoreFallDamageFromCurrentImpulse(true);
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }

        if (target.onGround()) {
            if (attacker instanceof ServerPlayer serverPlayer) {
                serverPlayer.setSpawnExtraParticlesOnFall(true);
            }

            SoundEvent sound = attacker.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD
                    ? SoundEvents.MACE_SMASH_GROUND_HEAVY
                    : SoundEvents.MACE_SMASH_GROUND;
            serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    sound, attacker.getSoundSource(), 1.0F, 1.0F);
        } else {
            serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.MACE_SMASH_AIR, attacker.getSoundSource(), 1.0F, 1.0F);
        }

        knockback(serverLevel, target, attacker);
    }

    private static Vec3 impactPosition(ServerPlayer player) {
        return player.isIgnoringFallDamageFromCurrentImpulse()
                && player.currentImpulseImpactPos != null
                && player.currentImpulseImpactPos.y <= player.position().y
                ? player.currentImpulseImpactPos
                : player.position();
    }

    /**
     * The fall-distance damage curve. Density multiplies the fall distance,
     * which is why {@code modifyFallBasedDamage} is scaled by it rather than
     * simply added.
     */
    public static float attackDamageBonus(Entity target, DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || !canSmashAttack(attacker)) {
            return 0.0F;
        }

        double fall = attacker.fallDistance;
        double bonus;
        if (fall <= 3.0) {
            bonus = 4.0 * fall;
        } else if (fall <= 8.0) {
            bonus = 12.0 + 2.0 * (fall - 3.0);
        } else {
            bonus = 22.0 + fall - 8.0;
        }

        if (attacker.level() instanceof ServerLevel serverLevel) {
            float perBlock = EnchantmentHelper.modifyFallBasedDamage(
                    serverLevel, attacker.getWeaponItem(), target, source, 0.0F);
            return (float) (bonus + perBlock * fall);
        }
        return (float) bonus;
    }

    private static void knockback(Level level, Entity target, Entity attacker) {
        level.levelEvent(2013, target.getOnPos(), 750);
        level.getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(SMASH_ATTACK_KNOCKBACK_RADIUS),
                        knockbackPredicate(attacker, target))
                .forEach(nearby -> {
                    Vec3 away = nearby.position().subtract(target.position());
                    double power = knockbackPower(attacker, nearby, away);
                    if (power > 0.0) {
                        Vec3 push = away.normalize().scale(power);
                        nearby.push(push.x, SMASH_ATTACK_KNOCKBACK_POWER, push.z);
                        if (nearby instanceof ServerPlayer serverPlayer) {
                            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                        }
                    }
                });
    }

    private static Predicate<LivingEntity> knockbackPredicate(Entity attacker, Entity target) {
        return nearby -> {
            boolean notSpectator = !nearby.isSpectator();
            boolean notInvolved = nearby != attacker && nearby != target;
            boolean notAllied = !attacker.isAlliedTo(nearby);
            boolean notOwnPet = !(nearby instanceof TamableAnimal tamable
                    && target instanceof LivingEntity owner
                    && tamable.isTame()
                    && tamable.isOwnedBy(owner));
            boolean notMarker = !(nearby instanceof ArmorStand stand && stand.isMarker());
            boolean inRange = target.distanceToSqr(nearby) <= Math.pow(SMASH_ATTACK_KNOCKBACK_RADIUS, 2.0);
            boolean notFlyingCreative = !(nearby instanceof Player player
                    && player.isCreative() && player.getAbilities().flying);
            return notSpectator && notInvolved && notAllied && notOwnPet && notMarker && inRange && notFlyingCreative;
        };
    }

    private static double knockbackPower(Entity attacker, LivingEntity nearby, Vec3 away) {
        return (SMASH_ATTACK_KNOCKBACK_RADIUS - away.length())
                * SMASH_ATTACK_KNOCKBACK_POWER
                * (attacker.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? 2 : 1)
                * (1.0 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }
}
