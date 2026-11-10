package info.mudbourn.mmsweapons.mixin.chainlink;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Drops a Chainlink weapon's two-handed hold while it is out on its chain.
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class ChainlinkPoseMixin {

    private static final Pose EMPTY_HANDS = new Pose("", "");

    @Inject(method = "poseForPlayer", at = @At("HEAD"), cancellable = true)
    private static void mmsWeapons$thrownChainPose(Player player, CallbackInfoReturnable<Pose> cir) {
        ItemStack held = player.getMainHandItem();
        if (Chainlink.throwable(held) && Chainlink.out(held)) {
            cir.setReturnValue(EMPTY_HANDS);
        }
    }
}
