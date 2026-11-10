package info.mudbourn.mmsweapons.mixin.throwable;

import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives sickles a trident-style charged throw: hold use to wind up, release to throw.
@Mixin(Item.class)
public abstract class SickleThrowMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$startSickleCharge(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (SickleThrowing.isSickle(stack) && !player.getCooldowns().isOnCooldown(stack)) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$sickleChargeAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (SickleThrowing.isSickle(stack)) {
            cir.setReturnValue(ItemUseAnimation.TRIDENT);
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$sickleChargeDuration(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (SickleThrowing.isSickle(stack)) {
            cir.setReturnValue(SickleThrowing.MAX_CHARGE_TICKS);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$releaseSickle(ItemStack stack, Level level, LivingEntity user, int remainingTicks, CallbackInfoReturnable<Boolean> cir) {
        if (SickleThrowing.isSickle(stack)) {
            cir.setReturnValue(user instanceof Player player
                && SickleThrowing.release(level, player, stack, SickleThrowing.MAX_CHARGE_TICKS - remainingTicks));
        }
    }
}
