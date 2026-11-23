package info.mudbourn.mmsweapons.throwable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
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

    /**
     * Reach of a slam: it lands SLAM_REACH in front and hits an oval skewed forward around that point, SLAM_LENGTH
     * deep along the look direction and SLAM_WIDTH either side, so it covers from just behind the impact to about
     * 6.5 blocks ahead.
     */
    private static final double SLAM_REACH = 3.0;
    private static final double SLAM_LENGTH = 3.5;
    private static final double SLAM_WIDTH = 2.5;
    /** The hulk hammer throws what it slams twice as far. */
    private static final double HULK_KNOCKBACK = 2.0;

    /**
     * The heavy hammers' Slam: the holder swings the hammer down into the ground in front of them and it lands as a
     * smash from {@code fall} blocks, hitting everything around the impact for the weapon's hit plus that smash
     * bonus (scaled by weight), with the mace's ground burst, sound and knockback.
     */
    public static void slam(ServerPlayer player, float fall) {
        ServerLevel level = player.level();
        ItemStack weapon = player.getMainHandItem();
        // A slow, full swing, so the hammer visibly comes down.
        player.swing(InteractionHand.MAIN_HAND, new net.minecraft.world.item.component.SwingAnimation(
                net.minecraft.world.item.component.SwingAnimation.DEFAULT.type(), 12), true);

        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
        Vec3 impact = player.position().add(look.scale(SLAM_REACH));
        level.levelEvent(2013, BlockPos.containing(impact).below(), 750);
        level.playSound(null, impact.x, impact.y, impact.z,
                fall > SMASH_ATTACK_HEAVY_THRESHOLD ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND,
                player.getSoundSource(), 1.0F, 0.8F);

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                + (float) fallBonus(fall) * Hammers.smashScale(weapon.getItem());
        AABB area = new AABB(impact, impact).inflate(SLAM_LENGTH, 1.5, SLAM_LENGTH);
        Vec3 forward = look;
        double kick = info.mudbourn.mmsweapons.spellengine.WeaponSkills.isHulkHammer(weapon) ? HULK_KNOCKBACK : 1.0;
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area, nearby ->
                nearby != player && nearby.isAlive() && !nearby.isSpectator() && !player.isAlliedTo(nearby)
                        && !(nearby instanceof ArmorStand stand && stand.isMarker())
                        && oval(nearby.position().subtract(impact), forward) <= 1.0);
        for (LivingEntity target : hit) {
            target.hurtServer(level, player.damageSources().mace(player), damage);
            Vec3 away = target.position().subtract(impact);
            // Strongest at the impact, fading to nothing at the oval's edge.
            double power = (1.0 - oval(away, forward)) * SMASH_ATTACK_KNOCKBACK_RADIUS * SMASH_ATTACK_KNOCKBACK_POWER * kick
                    * (fall > SMASH_ATTACK_HEAVY_THRESHOLD ? 2 : 1)
                    * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (power > 0.0 && away.lengthSqr() > 1.0E-4) {
                Vec3 push = away.normalize().scale(power);
                target.push(push.x, SMASH_ATTACK_KNOCKBACK_POWER, push.z);
                if (target instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                }
            }
        }
        if (!hit.isEmpty()) {
            weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    /** Where an offset from the impact sits in the slam's oval: 0 at the centre, 1 on its edge. */
    private static double oval(Vec3 offset, Vec3 forward) {
        double along = offset.x * forward.x + offset.z * forward.z;
        double side = forward == Vec3.ZERO ? Math.sqrt(offset.x * offset.x + offset.z * offset.z)
                : Math.abs(offset.x * forward.z - offset.z * forward.x);
        double length = forward == Vec3.ZERO ? SLAM_WIDTH : SLAM_LENGTH;
        return Math.sqrt((along / length) * (along / length) + (side / SLAM_WIDTH) * (side / SLAM_WIDTH));
    }

    /** The mace's fall-distance curve. */
    private static double fallBonus(double fall) {
        if (fall <= 3.0) {
            return 4.0 * fall;
        }
        if (fall <= 8.0) {
            return 12.0 + 2.0 * (fall - 3.0);
        }
        return 22.0 + fall - 8.0;
    }

    private static double effectiveFall(LivingEntity attacker) {
        return attacker.fallDistance;
    }

    public static boolean canSmashAttack(LivingEntity attacker) {
        return attacker.fallDistance > SMASH_ATTACK_FALL_THRESHOLD && !attacker.isFallFlying();
    }

    /** Ends a smash: the fall resets. */
    public static void spend(LivingEntity attacker) {
        attacker.resetFallDistance();
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
            serverPlayer.setIgnoreFallDamageFromCurrentImpulse(true, serverPlayer.currentImpulseImpactPos);
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }

        if (target.onGround()) {
            if (attacker instanceof ServerPlayer serverPlayer) {
                serverPlayer.setSpawnExtraParticlesOnFall(true);
            }

            SoundEvent sound = effectiveFall(attacker) > SMASH_ATTACK_HEAVY_THRESHOLD
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

        double fall = effectiveFall(attacker);
        double bonus = fallBonus(fall);

        if (attacker.level() instanceof ServerLevel serverLevel) {
            float perBlock = EnchantmentHelper.modifyFallBasedDamage(
                    serverLevel, attacker.getWeaponItem(), target, source, 0.0F);
            return (float) ((bonus + perBlock * fall) * Hammers.smashScale(attacker.getWeaponItem().getItem()));
        }
        return (float) (bonus * Hammers.smashScale(attacker.getWeaponItem().getItem()));
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
                * (attacker instanceof LivingEntity living && effectiveFall(living) > SMASH_ATTACK_HEAVY_THRESHOLD ? 2 : 1)
                * (1.0 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }
}
