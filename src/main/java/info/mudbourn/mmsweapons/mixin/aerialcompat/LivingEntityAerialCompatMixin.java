package info.mudbourn.mmsweapons.mixin.aerialcompat;

import info.mudbourn.mmsweapons.aerial.AerialCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Deal-damage and take-damage abilities of the Aerial Hell original, run for equipped compat items.
@Mixin(LivingEntity.class)
public abstract class LivingEntityAerialCompatMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float mmsWeapons$aerialAbilities(float amount, ServerLevel level, DamageSource source) {
        return amount * AerialCompat.damageMultiplier(source.getEntity(), (LivingEntity) (Object) this, source);
    }
}
