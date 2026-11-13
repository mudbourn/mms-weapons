package info.mudbourn.mmsweapons.client.mixin.focus;

import info.mudbourn.mmsweapons.focus.FocusSkill;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Cancels the local player's jump while a focus skill charges or executes, whatever pressed jump.
@Mixin(LivingEntity.class)
public abstract class FocusJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$noFocusJump(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && FocusSkill.holdsOff(player)) {
            ci.cancel();
        }
    }
}
