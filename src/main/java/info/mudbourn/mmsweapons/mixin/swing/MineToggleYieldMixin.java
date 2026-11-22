package info.mudbourn.mmsweapons.mixin.swing;

import net.bettercombat.client.AttackInteractor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AttackInteractor.class, remap = false)
public abstract class MineToggleYieldMixin {

    // The mine toggle shares its key with a Spell Engine spell, so it only toggles with an empty main hand; presses made while holding something are drained so they don't toggle later.
    @Redirect(method = "postTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean mmsWeapons$yieldToSpells(KeyMapping key) {
        boolean clicked = key.consumeClick();
        Minecraft client = Minecraft.getInstance();
        if (clicked && client.player != null && !client.player.getMainHandItem().isEmpty()) {
            while (key.consumeClick()) { }
            return false;
        }
        return clicked;
    }
}
