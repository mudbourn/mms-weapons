package info.mudbourn.mmsweapons.mixin.throwable;

import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.ThrownWeaponEntity;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives tagged melee weapons the trident's wind-up-and-throw behaviour.
 *
 * <p>This sits on {@code Item} rather than on Basic Weapons' {@code PikeItem}
 * for one structural reason: {@code PikeItem} does not declare {@code use},
 * {@code getUseDuration} or {@code releaseUsing}, and a mixin cannot inject into
 * a method a target class inherits rather than defines. Injecting at the base
 * class is the only seam that exists. The cost is a tag check on every
 * right-click of an item that doesn't override {@code use} — a single tag
 * lookup, well below the noise floor of an interaction.
 *
 * <p>The animation comes free: {@code PikeItem} already returns
 * {@code ItemUseAnimation.SPEAR}, it simply had nothing that ever started a use.
 *
 * @see MmsThrowables
 */
@Mixin(Item.class)
public abstract class ItemThrowMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$startThrow(Level level, Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!MmsThrowables.isThrowable(stack)) {
            return;
        }

        // Refusing at one durability point from breaking is vanilla's rule for
        // the trident: a weapon that shatters mid-flight is unrecoverable.
        if (stack.nextDamageWillBreak()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        // Riptide only releases in water or rain, and it is the wind-up that has
        // to refuse — otherwise the player charges a throw that can never fire.
        if (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0F && !player.isInWaterOrRain()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$throwUseDuration(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (MmsThrowables.isThrowable(stack)) {
            // Vanilla's "hold indefinitely" sentinel. The throw is gated on a
            // minimum wind-up in releaseUsing, not on the use running out.
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void mmsWeapons$releaseThrow(ItemStack stack, Level level, LivingEntity user, int timeLeft,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!MmsThrowables.isThrowable(stack) || !(user instanceof Player player)) {
            return;
        }

        cir.setReturnValue(mmsWeapons$throwWeapon(stack, level, player, timeLeft));
    }

    /**
     * Mirrors {@code TridentItem.releaseUsing}. Kept as one block rather than
     * split across injectors because the Riptide and throw branches share the
     * sound holder and the durability cost, and separating them is how those
     * two silently drift apart.
     */
    private static boolean mmsWeapons$throwWeapon(ItemStack stack, Level level, Player player, int timeLeft) {
        Item item = stack.getItem();
        int charge = 72000 - timeLeft;
        if (charge < MmsThrowables.THROW_THRESHOLD_TIME) {
            return false;
        }

        float riptide = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (riptide > 0.0F && !player.isInWaterOrRain()) {
            return false;
        }

        if (stack.nextDamageWillBreak()) {
            return false;
        }

        Holder<SoundEvent> throwSound = EnchantmentHelper
                .pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                .orElse(SoundEvents.TRIDENT_THROW);
        player.awardStat(Stats.ITEM_USED.get(item));

        if (level instanceof ServerLevel serverLevel) {
            stack.hurtWithoutBreaking(1, player);
            if (riptide == 0.0F) {
                ItemStack thrown = stack.consumeAndReturn(1, player);
                ThrownWeaponEntity weapon = Projectile.spawnProjectileFromRotation(
                        ThrownWeaponEntity::new, serverLevel, thrown, player,
                        0.0F, MmsThrowables.PROJECTILE_SHOOT_POWER, 1.0F);
                weapon.setChargeScale(MmsThrowables.chargeScale(charge));
                if (player.hasInfiniteMaterials()) {
                    weapon.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.playSound(null, weapon, throwSound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
        }

        if (riptide > 0.0F) {
            // Riptide launches the player, not the weapon, so it runs on both
            // sides — the client needs the impulse immediately for it to feel
            // like anything but a rubber band.
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            float x = -Mth.sin(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
            float y = -Mth.sin(pitch * (float) (Math.PI / 180.0));
            float z = Mth.cos(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
            float length = Mth.sqrt(x * x + y * y + z * z);
            x *= riptide / length;
            y *= riptide / length;
            z *= riptide / length;
            player.push(x, y, z);
            player.startAutoSpinAttack(20, 8.0F, stack);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
            }

            level.playSound(null, player, throwSound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }

        return false;
    }
}
