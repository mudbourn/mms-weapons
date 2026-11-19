package info.mudbourn.mmsweapons.mixin.spellengine;

import info.mudbourn.mmsweapons.spellengine.BarrageAssignment;
import net.spell_engine.utils.WeaponCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Runs once Spell Engine has handed every melee weapon its fallback skill, before the assignments are synced to clients.
@Mixin(WeaponCompatibility.class)
public abstract class WeaponCompatibilityMixin {

    @Inject(method = "initialize", at = @At("RETURN"))
    private static void mmsWeapons$addBarrage(CallbackInfo ci) {
        BarrageAssignment.assign();
    }
}
