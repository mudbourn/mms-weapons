package info.mudbourn.mmsweapons.client.mixin.spellengine;

import info.mudbourn.mmsweapons.client.spellengine.BlockingCastClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Spell Engine skips its skill keys while an item is in use, so a raised shield gets its ram skill handled here, at the start of key handling; Spell Engine itself casts nothing while the shield is in use.
@Mixin(value = Minecraft.class, priority = 1100)
public abstract class BlockingCastMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void mmsWeapons$castWhileBlocking(CallbackInfo ci) {
        BlockingCastClient.handle((Minecraft) (Object) this);
    }
}
