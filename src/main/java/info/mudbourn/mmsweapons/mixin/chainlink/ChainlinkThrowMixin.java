package info.mudbourn.mmsweapons.mixin.chainlink;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives the Bountiful Fish anchor and Chainlink weapons the chain throw.
@Mixin(Item.class)
public abstract class ChainlinkThrowMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$chainUse(Level level,
                                      Player player,
                                      InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (Chainlink.throwable(stack)) {
            cir.setReturnValue(Chainlink.use(player, hand));
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$chainAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (Chainlink.throwable(stack)) {
            cir.setReturnValue(ItemUseAnimation.TRIDENT);
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$chainDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (Chainlink.throwable(stack)) {
            cir.setReturnValue(Chainlink.MAX_HOLD_DURATION);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$chainRelease(ItemStack stack,
                                          Level level,
                                          LivingEntity entity,
                                          int timeLeft,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (Chainlink.throwable(stack)) {
            cir.setReturnValue(Chainlink.release(stack, level, entity, timeLeft));
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void mmsWeapons$chainTick(ItemStack stack,
                                       ServerLevel level,
                                       Entity holder,
                                       EquipmentSlot slot,
                                       CallbackInfo ci) {
        if (Chainlink.throwable(stack)) {
            Chainlink.tick(stack, holder);
        }
    }
}
