package info.mudbourn.mmsweapons.mixin.aerial;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsweapons.aerial.AerialGear;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Aerial Hell vs overworld in combat: weapon damage on mobs, and how much armor and shields count against a mob.
@Mixin(LivingEntity.class)
public abstract class LivingEntityAerialMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float mmsWeapons$aerialWeapon(float amount, ServerLevel level, DamageSource source) {
        return amount * AerialGear.weaponMultiplier(source.getWeaponItem(), (LivingEntity) (Object) this);
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I"))
    private int mmsWeapons$aerialArmor(LivingEntity self, Operation<Integer> original, DamageSource source) {
        int armor = original.call(self);
        return Math.max(0, (int) Math.round(armor + AerialGear.armorAdjustment(self, source.getEntity(), Attributes.ARMOR)));
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double mmsWeapons$aerialToughness(LivingEntity self, Holder<Attribute> attribute, Operation<Double> original, DamageSource source) {
        double value = original.call(self, attribute);
        if (attribute.equals(Attributes.ARMOR_TOUGHNESS)) {
            value = Math.max(0.0, value + AerialGear.armorAdjustment(self, source.getEntity(), Attributes.ARMOR_TOUGHNESS));
        }
        return value;
    }

    @WrapOperation(method = "applyItemBlocking", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"))
    private float mmsWeapons$aerialShield(BlocksAttacks blocks, DamageSource source, float amount, double angle, Operation<Float> original) {
        float blocked = original.call(blocks, source, amount, angle);
        LivingEntity self = (LivingEntity) (Object) this;
        return Math.min(amount, blocked * AerialGear.shieldMultiplier(self.getItemBlockingWith(), source.getEntity()));
    }
}
