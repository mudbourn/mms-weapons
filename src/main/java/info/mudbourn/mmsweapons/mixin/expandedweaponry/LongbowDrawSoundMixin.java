package info.mudbourn.mmsweapons.mixin.expandedweaponry;

import com.kielson.item.CustomBow;
import dev.imb11.sounds.api.config.ConfiguredSound;
import dev.imb11.sounds.config.SoundsConfig;
import dev.imb11.sounds.config.WorldSoundsConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Expanded Weaponry's bows the draw sound vanilla bows get.
 *
 * <p>The sound comes from the Sounds mod, whose {@code BowPullSoundEffect}
 * targets {@code net.minecraft.world.item.BowItem}. {@code CustomBow} extends
 * {@code ProjectileWeaponItem} directly and is not a {@code BowItem}, so the
 * longbow and slingshot draw in silence. This mirrors that mixin onto
 * {@code CustomBow} and reuses the same configured sound, so the volume, pitch
 * and enable toggle a player has already set for bows apply here too.
 *
 * <p>The instance field is per-item, not per-player, which is fine because this
 * only ever runs client-side on the local player's own draw — the same
 * assumption the Sounds mod makes.
 */
@Mixin(CustomBow.class)
public abstract class LongbowDrawSoundMixin {

    @Unique
    private SoundInstance mmsWeapons$pullSound;

    @Inject(
            method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
                    shift = At.Shift.AFTER))
    private void mmsWeapons$startPullSound(Level level, Player player, InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide()) {
            return;
        }
        ConfiguredSound sound = SoundsConfig.get(WorldSoundsConfig.class).bowPullSoundEffect;
        this.mmsWeapons$pullSound = sound.getSoundInstance();
        if (this.mmsWeapons$pullSound != null) {
            sound.playSound(this.mmsWeapons$pullSound);
        }
    }

    @Inject(
            method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
            at = @At("HEAD"))
    private void mmsWeapons$stopPullSound(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide() && this.mmsWeapons$pullSound != null) {
            SoundsConfig.get(WorldSoundsConfig.class).bowPullSoundEffect.stopSound(this.mmsWeapons$pullSound);
            this.mmsWeapons$pullSound = null;
        }
    }
}
