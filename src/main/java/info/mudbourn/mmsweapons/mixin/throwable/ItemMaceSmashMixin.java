package info.mudbourn.mmsweapons.mixin.throwable;

import info.mudbourn.mmsweapons.throwable.MaceSmash;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes the mace's smash attack onto tagged weapons — Expanded Weaponry's
 * hammers — so the mace enchantments have something to modify.
 *
 * <p>Same seam as {@link ItemThrowMixin} and for the same reason: Expanded
 * Weaponry's {@code HammerItem} declares none of these four methods, so
 * {@code Item} is the only class that can be injected. All four move together —
 * the damage bonus without {@code hurtEnemy} would deal smash damage with no
 * knockback or sound, and without {@code postHurtEnemy} the attacker's fall
 * distance never resets, so every subsequent hit that tick would smash again.
 */
@Mixin(Item.class)
public abstract class ItemMaceSmashMixin {

    @Inject(method = "hurtEnemy", at = @At("HEAD"))
    private void mmsWeapons$smashOnHit(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (MaceSmash.isSmashWeapon(stack.getItem())) {
            MaceSmash.hurtEnemy(target, attacker);
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"))
    private void mmsWeapons$resetFallAfterSmash(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (MaceSmash.isSmashWeapon(stack.getItem()) && MaceSmash.canSmashAttack(attacker)) {
            attacker.resetFallDistance();
        }
    }

    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$smashDamage(Entity target, float damage, DamageSource source,
                                       CallbackInfoReturnable<Float> cir) {
        if (MaceSmash.isSmashWeapon((Item) (Object) this)) {
            cir.setReturnValue(MaceSmash.attackDamageBonus(target, source));
        }
    }

    /**
     * A smash hit reports as {@code minecraft:mace_smash}, which is what the
     * fall-damage immunity and the death message key off.
     */
    @Inject(method = "getItemDamageSource", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$smashDamageSource(LivingEntity attacker, CallbackInfoReturnable<DamageSource> cir) {
        if (MaceSmash.isSmashWeapon((Item) (Object) this) && MaceSmash.canSmashAttack(attacker)) {
            cir.setReturnValue(attacker.damageSources().mace(attacker));
        }
    }
}
