package info.mudbourn.mmsweapons.mixin.expandedweaponry;

import com.kielson.item.CustomBow;
import com.kielson.item.ExpandedWeaponryItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Raises the longbow's arrow velocity without touching its draw time.
@Mixin(CustomBow.class)
public abstract class LongbowVelocityMixin {

    @Unique
    private static final double MMS_LONGBOW_VELOCITY = 4.5;

    @ModifyExpressionValue(
            method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/kielson/util/RangedWeaponStats;velocity()D"))
    private double mmsWeapons$longbowVelocity(double original) {
        return (Item) (Object) this == ExpandedWeaponryItems.LONGBOW ? MMS_LONGBOW_VELOCITY : original;
    }
}
