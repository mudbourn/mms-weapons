package info.mudbourn.mmsweapons.chainlink;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsweapons.material.NebuliteFuel;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The chain throw: hold to wind up, release to hurl the weapon out on a chain that reels back in.
// The Bountiful Fish anchor has it, and the shadoline morningstar spends Nebulite fuel on it.
public final class Chainlink {

    public static final int MIN_CHARGE_TICKS = 5;
    public static final int FULL_CHARGE_TICKS = 20;
    public static final int MAX_FLIGHT_COOLDOWN = 200;
    public static final int RETURN_COOLDOWN = 60;
    public static final int MAX_HOLD_DURATION = 72000;

    private static final Identifier BOUNTIFUL_ANCHOR = Identifier.fromNamespaceAndPath("bountiful-fish", "anchor");
    private static final Identifier SHADOLINE_MORNINGSTAR = Identifier.fromNamespaceAndPath("mms_weapons", "shadoline_morningstar");


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

    // Whether this stack throws on a chain: the Bountiful Fish anchor or the shadoline morningstar.
    public static boolean throwable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(BOUNTIFUL_ANCHOR) || id.equals(SHADOLINE_MORNINGSTAR);
    }

    // The shadoline morningstar keeps the focus barrage: a full focus bar turns its right click
    // into the barrage, and the chain throw comes back once the bar is spent.
    public static boolean keepsBarrage(ItemStack stack) {
        return fueled(stack);
    }

    // Whether right click throws the chain right now, rather than firing the barrage.
    public static boolean throwsNow(ItemStack stack) {
        return throwable(stack) && !(fueled(stack) && info.mudbourn.mmsweapons.focus.Focus.focused(stack));
    }

    // Whether this stack spends Nebulite fuel on the throw.
    private static boolean fueled(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SHADOLINE_MORNINGSTAR);
    }

    // Whether this stack is out on its chain.
    public static boolean out(ItemStack stack) {
        return stack.getOrDefault(CHAIN_OUT, false);
    }

    public static InteractionResult use(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (fueled(stack) && !NebuliteFuel.canSpend(stack, player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    // Clears the out flag once the cooldown has run out, in case the weapon never made it back.
    public static void tick(ItemStack stack, Entity holder) {
        if (out(stack) && holder instanceof Player player && !player.getCooldowns().isOnCooldown(stack)) {
            stack.remove(CHAIN_OUT);
        }
    }

    public static boolean release(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int held = MAX_HOLD_DURATION - timeLeft;
        if (held < MIN_CHARGE_TICKS) {
            return false;
        }
        if (fueled(stack) && !NebuliteFuel.canSpend(stack, entity)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        if (fueled(stack)) {
            NebuliteFuel.spend(stack, entity);
            info.mudbourn.mmsweapons.material.VoidCharge.charge(entity, MAX_FLIGHT_COOLDOWN);
        }
        float charge = Math.min(1.0F, held / (float) FULL_CHARGE_TICKS);
        level.addFreshEntity(new ThrownChainEntity(level, entity, stack, charge));
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                1.0F,
                0.6F);
        stack.set(CHAIN_OUT, true);
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(stack, MAX_FLIGHT_COOLDOWN);
            stack.hurtAndBreak(1, player, entity.getUsedItemHand());
        }
        return true;
    }
}
