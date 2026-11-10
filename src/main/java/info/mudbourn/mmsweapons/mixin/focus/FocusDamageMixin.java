package info.mudbourn.mmsweapons.mixin.focus;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Feeds every damage instance into focus, killing blows included. Fabric's AFTER_DAMAGE skips the hit that kills,
// so one-shot kills like small slimes never built any focus.
@Mixin(CombatTracker.class)
public abstract class FocusDamageMixin {

    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "recordDamage", at = @At("HEAD"))
    private void mmsWeapons$recordFocus(DamageSource source, float damage, CallbackInfo ci) {
        if (!this.mob.level().isClientSide()) {
            Focus.onDamage(source, damage);
        }
    }
}
