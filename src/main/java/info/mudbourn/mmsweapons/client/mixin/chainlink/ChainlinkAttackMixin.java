package info.mudbourn.mmsweapons.client.mixin.chainlink;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import net.bettercombat.client.AttackInteractor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Swallows the attack key while a Chainlink weapon is out on its chain, so it cannot swing until it is back.
@Mixin(value = AttackInteractor.class, remap = false)
public abstract class ChainlinkAttackMixin {

    @Shadow
    @Final
    private Minecraft client;

    @Inject(method = "onDoAttack", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$noSwingWhileThrown(CallbackInfoReturnable<Boolean> cir) {
        if (this.client.player == null) {
            return;
        }
        ItemStack held = this.client.player.getMainHandItem();
        if (Chainlink.throwable(held) && Chainlink.out(held)) {
            cir.setReturnValue(true);
        }
    }
}
