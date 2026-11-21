package info.mudbourn.mmsweapons.mixin.shieldbreak;

import info.mudbourn.mmsweapons.shieldbreak.ShieldBreakers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ShieldBreakerMixin {

    @Inject(method = "getSecondsToDisableBlocking", at = @At("RETURN"), cancellable = true)
    private void mmsWeapons$breakShields(CallbackInfoReturnable<Float> cir) {
        ItemStack weapon = ((LivingEntity) (Object) this).getWeaponItem();
        if (weapon != null && weapon.is(ShieldBreakers.TAG) && cir.getReturnValueF() < ShieldBreakers.SECONDS) {
            cir.setReturnValue(ShieldBreakers.SECONDS);
        }
    }
}
