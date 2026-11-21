package info.mudbourn.mmsweapons.shieldbash;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// A blocking player's combat roll becomes a goat-style ram: a short dash that hits the first living entity in the way.
// The client spends the roll, dashes and asks for the ram; the server checks the block and deals the hit.
public final class ShieldRam {

    public static final double RAM_IMPULSE = 1.6;
    private static final int RAM_TICKS = 10;
    private static final double RAM_REACH = 0.8;
    private static final double RAM_KNOCKBACK = 1.6;
    private static final double RAM_LIFT = 0.35;
    public static final float RAM_DAMAGE_TAKEN = 0.5F;

    private static final Map<UUID, Ram> RAMS = new ConcurrentHashMap<>();

    private ShieldRam() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RamPayload.TYPE, RamPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RamPayload.TYPE, (payload, context) -> start(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(ShieldRam::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RAMS.remove(handler.getPlayer().getUUID()));
    }

    private static void start(ServerPlayer player) {
        ItemStack shield = player.getItemBlockingWith();
        // The ram is the shield's own move: a weapon that blocks can't do it.
        if (shield == null || !ShieldWeapons.isShield(shield.getItem()) || player.isSpectator() || player.isPassenger() || RAMS.containsKey(player.getUUID())) {
            return;
        }
        Ram ram = new Ram(Vec3.directionFromRotation(0.0F, player.getYRot()), shield.copy(), RAM_TICKS);
        player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        info.mudbourn.mmsweapons.material.VoidCharge.activate(player, shield, RAM_TICKS + 10);
        RAMS.put(player.getUUID(), ram);
    }

    private static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, Ram> entry : RAMS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Ram ram = entry.getValue();
            if (player == null || !player.isAlive()) {
                RAMS.remove(entry.getKey());
                continue;
            }
            LivingEntity target = firstTarget(player, ram.direction());
            if (target != null) {
                hit(player, ram, target);
                RAMS.remove(entry.getKey());
            } else if (ram.ticksLeft() <= 1) {
                RAMS.remove(entry.getKey());
            } else {
                entry.setValue(new Ram(ram.direction(), ram.shield(), ram.ticksLeft() - 1));
            }
        }
    }

    // Whether this entity is a player mid-ram.
    public static boolean isRamming(LivingEntity entity) {
        return RAMS.containsKey(entity.getUUID());
    }

    private static LivingEntity firstTarget(ServerPlayer player, Vec3 direction) {
        AABB path = player.getBoundingBox().inflate(0.3, 0.0, 0.3).expandTowards(direction.scale(RAM_REACH));
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                path,
                entity -> entity != player && entity.isAlive() && !player.isPassengerOfSameVehicle(entity) && player.canAttack(entity)
            )
            .stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }

    private static void hit(ServerPlayer player, Ram ram, LivingEntity target) {
        ServerLevel level = player.level();
        DamageSource source = level.damageSources().playerAttack(player);
        float damage = EnchantmentHelper.modifyDamage(level, ram.shield(), target, source, bashDamage(player, ram.shield()));
        target.hurtServer(level, source, damage);
        target.setDeltaMovement(0.0, target.getDeltaMovement().y, 0.0);
        target.knockback(RAM_KNOCKBACK, -ram.direction().x, -ram.direction().z, source, damage);
        target.setDeltaMovement(target.getDeltaMovement().x, RAM_LIFT, target.getDeltaMovement().z);
        target.needsSync = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.needsSync = true;
        level.playSound(null, target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, target, SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // The shield's attack damage as if held in the main hand, before enchantment damage effects such as might.
    private static float bashDamage(ServerPlayer player, ItemStack shield) {
        AttributeInstance damage = new AttributeInstance(Attributes.ATTACK_DAMAGE, instance -> {
        });
        damage.setBaseValue(player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        shield.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                damage.addTransientModifier(modifier);
            }
        });
        return (float) damage.getValue();
    }

    private record Ram(Vec3 direction, ItemStack shield, int ticksLeft) {
    }

    // Client to server: the player is blocking and pressed the roll key, so ram instead of rolling.
    public record RamPayload() implements CustomPacketPayload {
        public static final Type<RamPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mms_weapons", "shield_ram"));
        public static final StreamCodec<ByteBuf, RamPayload> CODEC = StreamCodec.unit(new RamPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
