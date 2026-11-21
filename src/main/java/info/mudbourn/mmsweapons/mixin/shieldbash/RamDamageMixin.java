package info.mudbourn.mmsweapons.mixin.shieldbash;

import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Halves the damage a ramming player takes while the dash lasts.
@Mixin(LivingEntity.class)
public abstract class RamDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float mmsWeapons$ramGuard(float amount, ServerLevel level, DamageSource source) {
        return ShieldRam.isRamming((LivingEntity) (Object) this) ? amount * ShieldRam.RAM_DAMAGE_TAKEN : amount;
    }
}
