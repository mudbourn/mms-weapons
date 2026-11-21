package info.mudbourn.mmsweapons.mixin.shieldbash;

import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A shield in the off hand is held to block, not swung as a second weapon.
@Mixin(PlayerAttackHelper.class)
public abstract class ShieldDualWieldMixin {

    @Inject(method = "isDualWielding(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"), cancellable = true)
    private static void mmsWeapons$offHandShieldBlocks(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
            cir.setReturnValue(false);
        }
    }
}
