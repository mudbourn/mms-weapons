package info.mudbourn.mmsweapons.client.mixin.shieldbash;

import info.mudbourn.mmsweapons.client.shieldbash.ShieldRamClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Runs after combat_roll's own roll check, which never rolls while blocking.
@Mixin(Minecraft.class)
public abstract class ShieldRamKeyMixin {

    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void mmsWeapons$ramInsteadOfRoll(CallbackInfo ci) {
        ShieldRamClient.tryRam((Minecraft) (Object) this);
    }
}
