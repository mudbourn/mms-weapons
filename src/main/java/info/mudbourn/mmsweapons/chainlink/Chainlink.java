package info.mudbourn.mmsweapons.chainlink;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsweapons.material.NebuliteFuel;
import info.mudbourn.mmsweapons.material.VoidCharge;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// The chain throw of the Bountiful Fish anchor and the Nebulite-fuelled shadoline morningstar: the weapon flies out on a chain that reels back in.
public final class Chainlink {

    public static final int MAX_FLIGHT_COOLDOWN = 200;
    public static final int RETURN_COOLDOWN = 60;

    private static final Identifier BOUNTIFUL_ANCHOR = Identifier.fromNamespaceAndPath("bountiful-fish", "anchor");
    private static final Identifier SHADOLINE_MORNINGSTAR = Identifier.fromNamespaceAndPath("mms_weapons", "shadoline_morningstar");
    private static final Identifier HULK_HAMMER = Identifier.fromNamespaceAndPath("mutantmonsters", "hulk_hammer");

    // Set while this stack is out on its chain.
    public static final DataComponentType<Boolean> CHAIN_OUT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("mms_weapons", "chain_out"),
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private static final ResourceKey<EntityType<?>> THROWN_CHAIN_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("mms_weapons", "thrown_chain"));

    public static final EntityType<ThrownChainEntity> THROWN_CHAIN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            THROWN_CHAIN_KEY,
            EntityType.Builder.<ThrownChainEntity>of(ThrownChainEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build(THROWN_CHAIN_KEY));

    private Chainlink() {
    }

    // Called from the mod initializer to force the registrations above to load.
    public static void register() {
    }

    // Whether this stack throws on a chain: the Bountiful Fish anchor, the shadoline morningstar or the hulk hammer.
    public static boolean throwable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(BOUNTIFUL_ANCHOR) || id.equals(SHADOLINE_MORNINGSTAR) || id.equals(HULK_HAMMER);
    }

    // The shadoline morningstar keeps the focus barrage alongside its chain throw; the anchor does not.
    public static boolean keepsBarrage(ItemStack stack) {
        return fueled(stack);
    }

    // The heavy chain weapons, the anchor and the hulk hammer, ground slam where they land on the floor.
    public static boolean slams(ItemStack stack) {
        return throwable(stack) && !fueled(stack);
    }

    // Whether this stack spends Nebulite fuel on the throw.
    private static boolean fueled(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SHADOLINE_MORNINGSTAR);
    }

    // Whether this stack is out on its chain.
    public static boolean out(ItemStack stack) {
        return stack.getOrDefault(CHAIN_OUT, false);
    }

    // Whether this stack can be thrown now: not already out, off cooldown, fuelled, and it survives the durability cost.
    public static boolean canThrow(Player player, ItemStack stack) {
        if (!throwable(stack) || out(stack) || player.getCooldowns().isOnCooldown(stack) || stack.nextDamageWillBreak()) {
            return false;
        }
        return !fueled(stack) || NebuliteFuel.canSpend(stack, player) && wearsShadoline(player);
    }

    // Whether the shadoline morningstar's chain throw is offered to this player: always for the other chain weapons,
    // and for the morningstar only in a full set of shadoline armor, Enderscape's or our heavy one (mixed per slot).
    public static boolean offered(Player player, ItemStack stack) {
        return !fueled(stack) || wearsShadoline(player);
    }

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {
        net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
        net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
    private static final String[] ARMOR_PIECES = {"helmet", "chestplate", "leggings", "boots"};

    public static boolean wearsShadoline(Player player) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            Identifier worn = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(ARMOR_SLOTS[i]).getItem());
            String piece = ARMOR_PIECES[i];
            if (!worn.equals(Identifier.fromNamespaceAndPath("enderscape", "shadoline_" + piece))
                    && !worn.equals(Identifier.fromNamespaceAndPath("mms_weapons", "heavy_shadoline_" + piece))) {
                return false;
            }
        }
        return true;
    }

    // Clears the out flag once the cooldown has run out, in case the weapon never made it back.
    public static void tick(ItemStack stack, Entity holder) {
        if (out(stack) && holder instanceof Player player && !player.getCooldowns().isOnCooldown(stack)) {
            stack.remove(CHAIN_OUT);
        }
    }

    // Hurls the held weapon out on its chain at full charge.
    public static void throwChain(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        if (fueled(stack)) {
            NebuliteFuel.spend(stack, player);
            VoidCharge.charge(player, MAX_FLIGHT_COOLDOWN);
        }
        level.addFreshEntity(new ThrownChainEntity(level, player, stack, 1.0F));
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                1.0F,
                0.6F);
        stack.set(CHAIN_OUT, true);
        player.getCooldowns().addCooldown(stack, MAX_FLIGHT_COOLDOWN);
        stack.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
    }
}
