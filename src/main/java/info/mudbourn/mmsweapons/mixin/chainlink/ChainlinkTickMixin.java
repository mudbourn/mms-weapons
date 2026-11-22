package info.mudbourn.mmsweapons.mixin.chainlink;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Clears the out flag on a Chainlink weapon whose chain has run its course.
@Mixin(Item.class)
public abstract class ChainlinkTickMixin {

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
