package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.penumbra.enderscape.item.component.FueledTool;
import net.penumbra.enderscape.registry.component.EnderscapeDataComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Nebulite's upgrade for shadoline weapons. When a shadoline weapon's main ability fires
// (the focus barrage, a throw, the morningstar's chain, a shield ram) it spends one Nebulite
// and void-charges its user for the length of that ability. Hits the user lands while
// charged slow the target, snatch it through the void to the user's front once per
// ability, and an enemy killed while charged collapses into a short pull field.
// Without fuel the ability still fires, just uncharged.
public final class VoidCharge {

    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    private static final int SLOW_TICKS = 60;
    private static final int SLOW_AMPLIFIER = 1;
    private static final double SNATCH_MIN_DISTANCE = 2.5;
    private static final double SNATCH_REACH = 1.8;
    private static final double SNATCH_MAX_WIDTH = 2.0;
    private static final int FIELD_TICKS = 40;
    private static final double FIELD_RADIUS = 5.0;
    private static final double FIELD_PULL = 0.12;

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();
    private static final List<Field> FIELDS = new ArrayList<>();

    private VoidCharge() {
    }

    public static void register() {
        // Basic Weapons' shadoline weapons come from a material pack, so their fuel tank is added here.
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(
            item -> isShadoline(BuiltInRegistries.ITEM.getKey(item)) && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("basicweapons"),
            (builder, item) -> builder.set(EnderscapeDataComponents.FUELED_TOOL, FueledTool.simple(NebuliteFuel.MAX_FUEL))));
        ServerTickEvents.END_SERVER_TICK.register(VoidCharge::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register(VoidCharge::onDeath);
    }

    private static boolean isShadoline(Identifier id) {
        return id.getPath().startsWith("shadoline_");
    }

    // Whether this stack is a shadoline weapon with a Nebulite tank.
    public static boolean shadoline(ItemStack stack) {
        return !stack.isEmpty() && FueledTool.is(stack) && isShadoline(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    // Called when a main ability fires: spends one Nebulite from a shadoline stack and charges its user.
    public static void activate(LivingEntity user, ItemStack stack, int ticks) {
        if (user.level().isClientSide() || !shadoline(stack) || !NebuliteFuel.canSpend(stack, user)) {
            return;
        }
        NebuliteFuel.spend(stack, user);
        charge(user, ticks);
    }

    // Charges a user whose ability already paid its Nebulite, such as the morningstar's chain.
    public static void charge(LivingEntity user, int ticks) {
        if (!(user.level() instanceof ServerLevel level)) {
            return;
        }
        CHARGES.put(user.getUUID(), new Charge(level.getGameTime() + ticks, new HashSet<>()));
        level.playSound(null, user, SoundEvents.ENDERMAN_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, user.getX(), user.getY(0.6), user.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
    }

    private static Charge active(LivingEntity user) {
        Charge charge = CHARGES.get(user.getUUID());
        if (charge == null) {
            return null;
        }
        if (user.level().getGameTime() > charge.until) {
            CHARGES.remove(user.getUUID());
            return null;
        }
        return charge;
    }

    // Every damage instance, before health drops.
    public static void onDamage(LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F || !(source.getEntity() instanceof LivingEntity attacker) || attacker == target
            || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        Charge charge = active(attacker);
        if (charge == null) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOW_AMPLIFIER), attacker);
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.5, 0.3, 0.3);
        if (charge.snatched.add(target.getUUID())) {
            snatch(level, attacker, target);
        }
    }

    // Pulls the target through the void to just in front of the attacker, if it fits there.
    private static void snatch(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        if (target.getType().builtInRegistryHolder().is(BOSSES) || target.getBbWidth() > SNATCH_MAX_WIDTH || target.isPassenger()
            || target.distanceTo(attacker) < SNATCH_MIN_DISTANCE || target.level() != attacker.level()) {
            return;
        }
        Vec3 look = attacker.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 dest = attacker.position().add(flat.normalize().scale(SNATCH_REACH + target.getBbWidth() / 2.0));
        AABB box = target.getBoundingBox().move(dest.subtract(target.position()));
        if (!level.noCollision(target, box)) {
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY(0.5), target.getZ(), 24, 0.3, 0.5, 0.3, 0.5);
        target.teleportTo(dest.x, dest.y, dest.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.needsSync = true;
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void onDeath(LivingEntity dead, DamageSource source) {
        if (source.getEntity() instanceof LivingEntity killer && killer != dead
            && dead.level() instanceof ServerLevel level && active(killer) != null) {
            FIELDS.add(new Field(level, dead.position().add(0.0, dead.getBbHeight() / 2.0, 0.0), killer.getUUID(), level.getGameTime() + FIELD_TICKS));
            level.playSound(null, dead.getX(), dead.getY(), dead.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.35F, 1.8F);
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Field> it = FIELDS.iterator();
        while (it.hasNext()) {
            Field field = it.next();
            if (field.level.getGameTime() > field.until) {
                it.remove();
                continue;
            }
            field.level.sendParticles(ParticleTypes.REVERSE_PORTAL, field.center.x, field.center.y, field.center.z, 6, 0.6, 0.6, 0.6, 0.02);
            AABB area = new AABB(field.center, field.center).inflate(FIELD_RADIUS);
            for (LivingEntity entity : field.level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (entity.getUUID().equals(field.owner) || entity.isSpectator() || entity.getType().builtInRegistryHolder().is(BOSSES)
                    || entity instanceof OwnableEntity pet && field.owner.equals(pet.getOwnerReference() == null ? null : pet.getOwnerReference().getUUID())) {
                    continue;
                }
                Vec3 toward = field.center.subtract(entity.position());
                double distance = toward.length();
                if (distance < 0.5 || distance > FIELD_RADIUS) {
                    continue;
                }
                entity.push(toward.scale(FIELD_PULL / distance));
                entity.needsSync = true;
            }
        }
        if (!CHARGES.isEmpty()) {
            long now = server.overworld().getGameTime();
            CHARGES.values().removeIf(charge -> charge.until < now - 200);
        }
    }

    private record Charge(long until, Set<UUID> snatched) {
    }

    private record Field(ServerLevel level, Vec3 center, UUID owner, long until) {
    }
}
