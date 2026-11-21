package info.mudbourn.mmsweapons.mixin.headshot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Temporary: logs every arrow hit on a player and the player's state.
@Mixin(AbstractArrow.class)
public abstract class ArrowHitDebugMixin {

    @Unique
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons/arrowdebug");

    @WrapOperation(
        method = "onHitEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
        )
    )
    private float mmsWeapons$debugEnchant(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.world.item.ItemStack weapon,
        Entity target,
        DamageSource source,
        float damage,
        Operation<Float> original
    ) {
        float result = original.call(level, weapon, target, source, damage);
        LOG.info("arrow enchant damage: in={} out={} weapon={}", damage, result, weapon.getItem().toString());
        return result;
    }

    @WrapOperation(
        method = "onHitEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean mmsWeapons$debugHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        if (!(target instanceof Player player)) {
            return original.call(target, source, amount);
        }
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        net.minecraft.world.item.ItemStack weapon = arrow.getWeaponItem();
        LOG.info(
            "arrow pre-hit: speed={} weapon={} enchants={} owner={} arrowItem={}",
            arrow.getDeltaMovement().length(),
            weapon == null ? "null" : weapon.getItem().toString(),
            weapon == null ? "-" : weapon.getEnchantments().toString(),
            arrow.getOwner() == null ? "null" : arrow.getOwner().getType().toShortString(),
            arrow.getPickupItemStackOrigin().getItem().toString()
        );
        float before = player.getHealth();
        boolean result = original.call(target, source, amount);
        LOG.info(
            "arrow hit {}: result={} amount={} source={} health {}->{} hurtTime={} invulnerableTo={} blocking={} creative={} ramming={} side={}",
            player.getName().getString(),
            result,
            amount,
            source.getMsgId(),
            before,
            player.getHealth(),
            player.hurtTime,
            player.level() instanceof net.minecraft.server.level.ServerLevel level && player.isInvulnerableTo(level, source),
            player.isBlocking(),
            player.isCreative(),
            ShieldRam.isRamming(player),
            player.level().isClientSide() ? "client" : "server"
        );
        return result;
    }
}
