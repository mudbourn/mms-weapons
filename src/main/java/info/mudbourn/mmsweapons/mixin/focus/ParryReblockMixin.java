package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Barrage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// After a weapon or shield parries a barrage swing, raising it again blocks at once instead of waiting out the block delay,
// so letting go between swings and re-raising is rewarded.
@Mixin(LivingEntity.class)
public abstract class ParryReblockMixin {

    @Inject(method = "getItemBlockingWith", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$instantReblock(CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || !self.isUsingItem()) {
            return;
        }
        ItemStack using = self.getUseItem();
        if (using.has(DataComponents.BLOCKS_ATTACKS) && Barrage.inParryWindow(self)) {
            cir.setReturnValue(using);
        }
    }
}
