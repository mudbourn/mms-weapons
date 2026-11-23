package info.mudbourn.mmsweapons.shieldbash;

import info.mudbourn.mmsweapons.material.VoidCharge;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// The shield ram skill: a short dash that ploughs through a crowd, hitting every valid entity in its path once.
public final class ShieldRam {

    public static final double RAM_IMPULSE = 1.6;
    private static final int RAM_TICKS = 10;
    private static final double RAM_REACH = 0.8;
    // How far the shield's front reaches either side of the player, so a ram clears a line of mobs, not just one.
    private static final double RAM_WIDTH = 0.7;
    private static final double RAM_KNOCKBACK = 1.6;
    private static final double RAM_LIFT = 0.35;
    public static final float RAM_DAMAGE_TAKEN = 0.5F;

    private static final Map<UUID, Ram> RAMS = new ConcurrentHashMap<>();

    private ShieldRam() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(DashPayload.TYPE, DashPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(ShieldRam::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RAMS.remove(handler.getPlayer().getUUID()));
    }

    // The shield a player can ram with right now: a main-hand shield, not a blocking weapon, on the ground with no ram
    // already running. It may be raised or down; a lowered one (even one still cooling down from being lowered or
    // from a block) is put up by the ram.
    public static ItemStack ramShield(Player player) {
        ItemStack shield = player.getMainHandItem();
        boolean otherUse = player.isUsingItem() && player.getUsedItemHand() != InteractionHand.MAIN_HAND;
        if (shield.isEmpty() || otherUse || !ShieldWeapons.isShield(shield.getItem()) || player.isSpectator() || player.isPassenger() || player.isSwimming() || !player.onGround() || RAMS.containsKey(player.getUUID())) {
            return null;
        }
        return shield;
    }

    // Puts the main-hand shield up for a ram cast with the guard down, clearing any cooldown that would stop it rising.
    public static void raiseGuard(Player player) {
        if (player.isUsingItem()) {
            return;
        }
        ItemStack shield = player.getMainHandItem();
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(shield));
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    public static void start(ServerPlayer player, ItemStack shield) {
        raiseGuard(player);
        Ram ram = new Ram(Vec3.directionFromRotation(0.0F, player.getYRot()), shield.copy(), RAM_TICKS, ConcurrentHashMap.newKeySet());
        player.level().playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        VoidCharge.activate(player, shield, RAM_TICKS + 10);
        RAMS.put(player.getUUID(), ram);
        info.mudbourn.mmsweapons.focus.DashStep.boost(player, RAM_TICKS + 4);
        ServerPlayNetworking.send(player, new DashPayload());
    }

    private static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, Ram> entry : RAMS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Ram ram = entry.getValue();
            if (player == null || !player.isAlive()) {
                RAMS.remove(entry.getKey());
                continue;
            }
            for (LivingEntity target : targets(player, ram)) {
                hit(player, ram, target);
            }
            if (ram.ticksLeft() <= 1) {
                // The dash ends here; a ram that connected stops the player dead behind its shield.
                if (!ram.hit().isEmpty()) {
                    player.setDeltaMovement(Vec3.ZERO);
                    player.needsSync = true;
                }
                RAMS.remove(entry.getKey());
            } else {
                entry.setValue(new Ram(ram.direction(), ram.shield(), ram.ticksLeft() - 1, ram.hit()));
            }
        }
    }

    // Whether this entity is a player mid-ram.
    public static boolean isRamming(LivingEntity entity) {
        return RAMS.containsKey(entity.getUUID());
    }

    // Every valid entity in front of the shield that this ram has not hit yet.
    private static List<LivingEntity> targets(ServerPlayer player, Ram ram) {
        AABB path = player.getBoundingBox().inflate(RAM_WIDTH, 0.0, RAM_WIDTH).expandTowards(ram.direction().scale(RAM_REACH));
        return player.level().getEntitiesOfClass(
            LivingEntity.class,
            path,
            entity -> entity != player && entity.isAlive() && !ram.hit().contains(entity.getUUID())
                && !player.isPassengerOfSameVehicle(entity) && player.canAttack(entity)
                && entity.position().subtract(player.position()).dot(ram.direction()) > -0.2
        );
    }

    private static void hit(ServerPlayer player, Ram ram, LivingEntity target) {
        ram.hit().add(target.getUUID());
        ServerLevel level = player.level();
        DamageSource source = level.damageSources().playerAttack(player);
        float damage = EnchantmentHelper.modifyDamage(level, ram.shield(), target, source, bashDamage(player, ram.shield()));
        target.hurtServer(level, source, damage);
        target.setDeltaMovement(0.0, target.getDeltaMovement().y, 0.0);
        target.knockback(RAM_KNOCKBACK, -ram.direction().x, -ram.direction().z, source, damage);
        target.setDeltaMovement(target.getDeltaMovement().x, RAM_LIFT, target.getDeltaMovement().z);
        target.needsSync = true;
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

    private record Ram(Vec3 direction, ItemStack shield, int ticksLeft, Set<UUID> hit) {
    }

    // Server to client: the caster's own ram started, so the local player dashes.
    public record DashPayload() implements CustomPacketPayload {
        public static final Type<DashPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mms_weapons", "shield_ram"));
        public static final StreamCodec<ByteBuf, DashPayload> CODEC = StreamCodec.unit(new DashPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
