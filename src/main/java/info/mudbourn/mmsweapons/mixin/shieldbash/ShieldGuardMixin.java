package info.mudbourn.mmsweapons.mixin.shieldbash;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// A shield blocks by its item's own guard, not the flat 50% Combat Amenities swaps in for each hit.
@Mixin(LivingEntity.class)
public abstract class ShieldGuardMixin {

    @WrapOperation(method = "applyItemBlocking", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"))
    private float mmsWeapons$itemGuard(BlocksAttacks blocks, DamageSource source, float amount, double angle, Operation<Float> original) {
        ItemStack guard = ((LivingEntity) (Object) this).getItemBlockingWith();
        BlocksAttacks own = guard != null && guard.getItem() instanceof ShieldItem
            ? guard.getItem().components().get(DataComponents.BLOCKS_ATTACKS)
            : null;
        return original.call(own != null ? own : blocks, source, amount, angle);
    }
}
