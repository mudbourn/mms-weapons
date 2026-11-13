package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Shows a focus weapon's focus as a red bar under it, in place of durability, while it has any focus.
@Mixin(Item.class)
public abstract class FocusBarMixin {

    private static final int FOCUS_BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.9F, 0.1F, 0.1F);

    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$focusBarVisible(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (showsFocus(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$focusBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (showsFocus(stack)) {
            cir.setReturnValue(Math.round(Focus.focus(stack) / Focus.MAX_FOCUS * 13.0F));
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$focusBarColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (showsFocus(stack)) {
            cir.setReturnValue(FOCUS_BAR_COLOR);
        }
    }

    private static boolean showsFocus(ItemStack stack) {
        return Focus.focus(stack) > 0.0F && Focus.builds(stack);
    }
}
