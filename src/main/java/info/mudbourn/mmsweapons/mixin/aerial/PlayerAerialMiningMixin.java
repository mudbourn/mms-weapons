package info.mudbourn.mmsweapons.mixin.aerial;

import info.mudbourn.mmsweapons.aerial.AerialGear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Aerial Hell tools mine overworld blocks 1.25x faster; overworld tools mine Aerial Hell blocks at 0.75x.
@Mixin(Player.class)
public abstract class PlayerAerialMiningMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void mmsWeapons$aerialMining(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float multiplier = AerialGear.miningMultiplier(self.getMainHandItem(), state);
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
