package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

// Gives weapons with no right click of their own the focus barrage as a held skill: hold to wind up, release to fire.
// Holding gives a window to crouch-cancel it. Only applies where the base use did nothing.
@Mixin(Item.class)
public abstract class BarrageUseMixin {

    private static final Identifier CHARGE_SOUND = Identifier.fromNamespaceAndPath("mms_arsenal", "dragon_slayer_charge");

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    private void mmsWeapons$windUp(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND || cir.getReturnValue() != InteractionResult.PASS) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (ready(stack) && !player.getCooldowns().isOnCooldown(stack)) {
            player.startUsingItem(hand);
            // The same charge sound the Dragon Slayer and Odachi wind up with.
            SoundEvent charge = BuiltInRegistries.SOUND_EVENT.getValue(CHARGE_SOUND);
            if (charge != null) {
                level.playSound(player, player.blockPosition(), charge, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$windUpDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (ready(stack)) {
            cir.setReturnValue(Barrage.MAX_HOLD_TICKS);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$windUpAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (ready(stack)) {
            cir.setReturnValue(ItemUseAnimation.NONE);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$release(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfoReturnable<Boolean> cir) {
        if (!ready(stack)) {
            return;
        }
        boolean fired = entity instanceof Player player
                && !player.isShiftKeyDown()
                && Barrage.MAX_HOLD_TICKS - timeLeft >= Barrage.WIND_UP_TICKS;
        if (fired) {
            Barrage.start((Player) entity, stack);
        }
        cir.setReturnValue(fired);
    }

    private static boolean ready(ItemStack stack) {
        return Focus.focused(stack) && Focus.eligible(stack);
    }
}
