package info.mudbourn.mmsweapons.mixin.spellengine;

import info.mudbourn.mmsweapons.spellengine.WizardFocus;
import net.minecraft.world.entity.LivingEntity;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Raises a caster's spell power while their empowered spell is going off. Server side only, so tooltips are unchanged.
@Mixin(value = SpellPower.class, remap = false)
public abstract class SpellPowerEmpowerMixin {

    @Inject(method = "getSpellPower", at = @At("RETURN"), cancellable = true)
    private static void mmsWeapons$empower(SpellSchool school, LivingEntity caster, CallbackInfoReturnable<SpellPower.Result> cir) {
        if (caster == null || caster.level().isClientSide()) {
            return;
        }
        double multiplier = WizardFocus.multiplier(caster);
        if (multiplier != 1.0) {
            SpellPower.Result result = cir.getReturnValue();
            cir.setReturnValue(new SpellPower.Result(result.school(), result.baseValue() * multiplier, result.criticalChance(), result.criticalDamage()));
        }
    }
}
