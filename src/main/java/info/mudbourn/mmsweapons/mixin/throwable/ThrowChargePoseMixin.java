package info.mudbourn.mmsweapons.mixin.throwable;

import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Drops the Better Combat idle pose while a throw is being charged, so the vanilla arm-back throwing pose shows.
// Without this, a weapon idling in a held pose (hammers on the shoulder) kept its arm down through the wind-up.
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class ThrowChargePoseMixin {

    private static final Pose NO_POSE = new Pose("", "");

    @Inject(method = "poseForPlayer", at = @At("HEAD"), cancellable = true)
    private static void mmsWeapons$throwChargePose(Player player, CallbackInfoReturnable<Pose> cir) {
        if (!player.isUsingItem()) {
            return;
        }
        ItemUseAnimation animation = player.getUseItem().getUseAnimation();
        if (animation == ItemUseAnimation.TRIDENT || animation == ItemUseAnimation.SPEAR) {
            cir.setReturnValue(NO_POSE);
        }
    }
}
