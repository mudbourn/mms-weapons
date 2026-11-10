package info.mudbourn.mmsweapons.throwable.sickle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Charged throwing for any sickle, from any mod whose item id ends in "_sickle".
public final class SickleThrowing {

    public static final int MAX_CHARGE_TICKS = 72000;
    private static final int MIN_CHARGE_TICKS = 10;
    private static final float THROW_SPEED = 2.0F;
    private static final int COOLDOWN_TICKS = 10;

    private SickleThrowing() {
    }

    public static boolean isSickle(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sickle");
    }

    public static boolean release(Level level, Player player, ItemStack stack, int chargedTicks) {
        if (chargedTicks < MIN_CHARGE_TICKS) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            ThrownSickleEntity sickle = new ThrownSickleEntity(serverLevel, player, stack.copyWithCount(1));
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
            serverLevel.addFreshEntity(sickle);
            serverLevel.playSound(
                null,
                sickle,
                SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        }
        return true;
    }
}
