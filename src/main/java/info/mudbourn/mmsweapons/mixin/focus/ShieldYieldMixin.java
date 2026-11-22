package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A one-handed weapon passes its right click on when a shield is in the off hand, so the shield raises instead.
@Mixin(ItemStack.class)
public abstract class ShieldYieldMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$yieldToShield(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (Focus.shieldTakesRightClick(player, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
