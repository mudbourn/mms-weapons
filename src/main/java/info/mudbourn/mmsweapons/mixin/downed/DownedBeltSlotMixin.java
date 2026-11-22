package info.mudbourn.mmsweapons.mixin.downed;

import info.mudbourn.mmsweapons.downed.Downed;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A downed player can't swap items with the Combat Amenities belt slot.
@Pseudo
@Mixin(targets = "net.hollowed.combatamenities.networking.slots.belt.BeltSlotServerPacket", remap = false)
public abstract class DownedBeltSlotMixin {

    @Inject(method = "lambda$registerServerPacket$1", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mmsWeapons$noSwapWhileDowned(ServerPlayNetworking.Context context, CallbackInfo ci) {
        if (Downed.is(context.player())) ci.cancel();
    }
}
