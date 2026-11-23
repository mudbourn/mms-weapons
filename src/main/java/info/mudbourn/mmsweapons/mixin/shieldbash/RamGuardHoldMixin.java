package info.mudbourn.mmsweapons.mixin.shieldbash;

import info.mudbourn.mmsweapons.client.shieldbash.ShieldRamClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While a shield ram holds the guard up, letting go of right click does not lower the shield.
@Mixin(MultiPlayerGameMode.class)
public abstract class RamGuardHoldMixin {

    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$holdRamGuard(Player player, CallbackInfo ci) {
        if (ShieldRamClient.holdsGuard()) {
            ci.cancel();
        }
    }
}
