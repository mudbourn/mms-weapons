package info.mudbourn.mmsweapons.mixin.expandedweaponry;

import com.kielson.item.CustomBow;
import com.kielson.item.ExpandedWeaponryItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.Item;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the longbow's arrow fly faster without touching its draw time.
 *
 * <p>MMS wants the longbow to read as a sniping weapon: same slow pull, but a
 * far flatter, quicker shot than a plain bow. {@code projectileVelocity} is a
 * private final constructor argument shared by every {@code CustomBow} — the
 * slingshot included — so rather than rewrite the field this intercepts the two
 * places it is read and substitutes a higher number for the longbow only.
 *
 * <p>Both reads have to move together. {@code releaseUsing} multiplies it by
 * the pull progress to get the launch speed, while {@code shootProjectile}
 * <em>divides</em> the ranged-damage attribute by it before calling
 * {@code setBaseDamage}, because vanilla multiplies base damage back out by the
 * arrow's actual speed on impact. The two cancel exactly, which is what keeps
 * damage pinned to the 12 set in {@code WeaponTuning} no matter what velocity
 * is used here. Bumping only one of them would silently rescale damage.
 */
@Mixin(CustomBow.class)
public abstract class LongbowVelocityMixin {

    /**
     * Kielson ships 3.75; a plain vanilla bow is 3.0. 4.5 flattens the arc
     * noticeably at sniping range while keeping the arrow visible in flight.
     */
    @Unique
    private static final double MMS_LONGBOW_VELOCITY = 4.5;

    @ModifyExpressionValue(
            method = {
                    "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
                    "shootProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFLnet/minecraft/world/entity/LivingEntity;)V"
            },
            at = @At(
                    value = "FIELD",
                    target = "Lcom/kielson/item/CustomBow;projectileVelocity:D",
                    opcode = Opcodes.GETFIELD))
    private double mmsWeapons$longbowVelocity(double original) {
        return (Item) (Object) this == ExpandedWeaponryItems.LONGBOW ? MMS_LONGBOW_VELOCITY : original;
    }
}
