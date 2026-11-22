package info.mudbourn.mmsweapons.mixin.spellengine;

import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.spellengine.WizardFocus;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adds Empower to a spellcaster's weapon wherever its spell container is read.
@Mixin(value = SpellContainerHelper.class, remap = false)
public abstract class SpellContainerEmpowerMixin {

    @Inject(method = "containerFromItemStack", at = @At("RETURN"), cancellable = true)
    private static void mmsWeapons$addEmpower(ItemStack stack, CallbackInfoReturnable<SpellContainer> cir) {
        SpellContainer container = cir.getReturnValue();
        if (container != null && !container.spell_ids().isEmpty() && Focus.spellFocus(stack)
                && !container.spell_ids().contains(WizardFocus.SPELL_ID)) {
            List<String> spells = new ArrayList<>(container.spell_ids());
            spells.add(WizardFocus.SPELL_ID);
            cir.setReturnValue(container.copyWith(spells));
        }
    }
}
