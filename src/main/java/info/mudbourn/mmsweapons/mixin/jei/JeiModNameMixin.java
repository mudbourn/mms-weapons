package info.mudbourn.mmsweapons.mixin.jei;

import info.mudbourn.mmsweapons.material.CompatOrigin;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// JEI's ingredient tooltip mod-name line, matching the WTHIT one for compat items.
@Pseudo
@Mixin(targets = "mezz.jei.library.helpers.ModIdHelper", remap = false)
public abstract class JeiModNameMixin {
    @Inject(method = "getModNameForTooltip", at = @At("HEAD"), cancellable = true)
    private <T> void mmsWeapons$origin(ITypedIngredient<T> ingredient, CallbackInfoReturnable<Optional<Component>> cir) {
        if (ingredient.getIngredient() instanceof ItemStack stack) {
            String label = CompatOrigin.label(stack);
            if (label != null) cir.setReturnValue(Optional.of(Component.literal(label).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC)));
        }
    }
}
