package info.mudbourn.mmsweapons.mixin.aerialcompat;

import fr.factionbedrock.aerialhell.Item.Ability.AbilityUseSituation;
import fr.factionbedrock.aerialhell.Item.Ability.UsingItemUseSituationInfo;
import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import info.mudbourn.mmsweapons.aerial.AerialCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Tick, use and release abilities of the Aerial Hell original, run for the compat item.
@Mixin(ItemStack.class)
public abstract class ItemStackAerialCompatMixin {

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void mmsWeapons$aerialTick(Level level, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        Item original = AerialCompat.counterpart(self.getItem());
        if (original != null && level instanceof ServerLevel server
            && (original instanceof AerialHellItem || AerialCompat.sameKind(self.getItem()))) {
            // AerialHellItem runs its tick abilities; shadow armor runs its full-set bonus.
            original.inventoryTick(self, server, entity, slot);
        }
    }

    @Inject(method = "use", at = @At("HEAD"))
    private void mmsWeapons$aerialUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack self = (ItemStack) (Object) this;
        AerialHellItem original = AerialCompat.abilities(self.getItem());
        if (original != null && original.abilitySelector != null) {
            original.abilitySelector.tryUseAbility(new AbilityUseSituation.OnUse(self, player, hand.asEquipmentSlot()));
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void mmsWeapons$aerialRelease(Level level, LivingEntity entity, int remaining, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        AerialHellItem original = AerialCompat.abilities(self.getItem());
        if (original != null && original.abilitySelector != null) {
            int used = self.getUseDuration(entity) - remaining;
            original.abilitySelector.tryUseAbility(new AbilityUseSituation.OnReleaseUsing(self, entity, new UsingItemUseSituationInfo(used)));
        }
    }
}
