package info.mudbourn.mmsweapons.mixin.throwable;

import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Every throwable winds up with the trident's arm-back pose, replacing the low spear stance pikes and hammers report.
// Hooked on ItemStack so it also covers items that override getUseAnimation, such as Basic Weapons pikes.
@Mixin(ItemStack.class)
public abstract class ThrowUseAnimationMixin {

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$throwUseAnimation(CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (MmsThrowables.isThrowable((ItemStack) (Object) this)) {
            cir.setReturnValue(ItemUseAnimation.TRIDENT);
        }
    }
}
