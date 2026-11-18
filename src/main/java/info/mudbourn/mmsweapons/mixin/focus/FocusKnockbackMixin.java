package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Barrage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Drops hit knockback on a player winding up or swinging a focus barrage.
@Mixin(LivingEntity.class)
public abstract class FocusKnockbackMixin {

    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$holdGround(double power, double x, double z, DamageSource source, float scale, boolean dealtByPlayer, CallbackInfo ci) {
        if (Barrage.knockbackImmune((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
