package info.mudbourn.mmsweapons.client.mixin.spellengine;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import info.mudbourn.mmsweapons.client.spellengine.MouseSlots;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.internals.casting.SpellCast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Reorders the hotbar's options so relic and focus spells land on the mouse keys.
@Mixin(value = SpellHotbar.class, remap = false)
public abstract class SpellHotbarMouseMixin {

    @WrapOperation(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/spell_engine/client/casting/ClientCastController;displayOptions()Ljava/util/List;"
        )
    )
    private List<SpellCast.Option> mmsWeapons$mouseSlots(
        ClientCastController controller,
        Operation<List<SpellCast.Option>> original,
        @Local(argsOnly = true) LocalPlayer player,
        @Local(argsOnly = true) Options options
    ) {
        return MouseSlots.arrange(player, options, original.call(controller));
    }
}
