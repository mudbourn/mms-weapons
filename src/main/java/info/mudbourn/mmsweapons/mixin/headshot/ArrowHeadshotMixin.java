package info.mudbourn.mmsweapons.mixin.headshot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsweapons.headshot.HeadshotBoxes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Multiplies the damage of arrows fired from a headshot bow when they pass through the target's head.
@Mixin(AbstractArrow.class)
public abstract class ArrowHeadshotMixin {

    @Unique
    private static final TagKey<Item> HEADSHOT_BOWS = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("mms_weapons", "headshot_bows")
    );
    @Unique
    private static final float HEADSHOT_MULTIPLIER = 2.0F;
    @Unique
    private static final double HEAD_GROW = 0.15;

    @WrapOperation(
        method = "onHitEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean mmsWeapons$headshot(Entity target, DamageSource source, float amount, Operation<Boolean> original, EntityHitResult hit) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        ItemStack weapon = self.getWeaponItem();
        if (weapon != null && weapon.is(HEADSHOT_BOWS) && target instanceof LivingEntity living) {
            Vec3 motion = self.getDeltaMovement();
            Vec3 start = self.position().subtract(motion);
            Vec3 end = self.position().add(motion);
            if (HeadshotBoxes.hitsHead(living, start, end, HEAD_GROW)) {
                amount *= HEADSHOT_MULTIPLIER;
            }
        }
        return original.call(target, source, amount);
    }
}
