package info.mudbourn.mmsweapons.throwable.sickle;

import info.mudbourn.mmsweapons.material.VoidCharge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

// Throwing for any sickle, from any mod whose item id ends in "_sickle".
public final class SickleThrowing {

    private static final float THROW_SPEED = 2.0F;
    private static final int COOLDOWN_TICKS = 10;

    private SickleThrowing() {
    }

    public static boolean isSickle(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sickle");
    }

    // Whether this stack is a sickle that is ready to throw.
    public static boolean canThrow(Player player, ItemStack stack) {
        return isSickle(stack) && !player.getCooldowns().isOnCooldown(stack);
    }

    public static void throwSickle(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        VoidCharge.activate(player, stack, 80);
        ThrownSickleEntity sickle = new ThrownSickleEntity(level, player, stack.copyWithCount(1));
        sickle.shootFromRotation(
            player,
            player.getXRot(),
            player.getYRot(),
            0.0F,
            THROW_SPEED,
            1.0F);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        if (player.hasInfiniteMaterials()) {
            sickle.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        } else {
            stack.shrink(1);
        }
        level.addFreshEntity(sickle);
        level.playSound(
            null,
            sickle,
            SoundEvents.TRIDENT_THROW.value(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F);
    }
}
