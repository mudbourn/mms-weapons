package info.mudbourn.mmsweapons.mixin.swing;

import net.bettercombat.BetterCombatMod;
import net.bettercombat.client.AttackInteractor;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Plays slow capped_swing weapons at a capped length; the vanilla attack cooldown still gates the next swing.
@Mixin(AttackInteractor.class)
public abstract class SwingDurationMixin {

    private static final float MAX_SWING_TICKS = 12.0F;

    private static final TagKey<Item> CAPPED_SWING = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("mms_weapons", "capped_swing")
    );

    @Shadow
    @Final
    private Minecraft client;

    @Shadow
    private int comboReset;

    @Redirect(
        method = {"startUpswing", "cancelWeaponSwing"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/bettercombat/logic/PlayerAttackHelper;getAttackCooldownTicksCapped(Lnet/minecraft/world/entity/player/Player;)F"
        ),
        require = 2
    )
    private float mmsWeapons$capSwing(Player player) {
        float cooldown = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
        if (player.getMainHandItem().is(CAPPED_SWING)) {
            return Math.min(cooldown, MAX_SWING_TICKS);
        }
        return cooldown;
    }

    // Keeps the combo window sized to the full cooldown so combos survive the wait between swings.
    @Inject(method = "startUpswing", at = @At("TAIL"))
    private void mmsWeapons$keepComboWindow(CallbackInfo ci) {
        Player player = this.client.player;
        if (player != null && player.getMainHandItem().is(CAPPED_SWING)) {
            float cooldown = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
            this.comboReset = Math.round(cooldown * BetterCombatMod.config.combo_reset_rate);
        }
    }
}
