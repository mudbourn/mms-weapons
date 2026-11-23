package info.mudbourn.mmsweapons.mixin.aerialcompat;

import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import info.mudbourn.mmsweapons.aerial.AerialCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

// The "Press SHIFT to view Abilities detail" line, right after the item's own tooltip text.
@Mixin(ItemStack.class)
public abstract class ItemStackAerialTooltipMixin {

    @Inject(
        method = "addDetailsToTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            shift = At.Shift.AFTER
        )
    )
    private void mmsWeapons$aerialAbilityTooltip(Item.TooltipContext context, TooltipDisplay display, Player player, TooltipFlag flag, Consumer<Component> consumer, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        Item counterpart = AerialCompat.counterpart(self.getItem());
        if (counterpart instanceof AerialHellItem original) {
            Player viewer = Minecraft.getInstance().player;
            if (viewer != null) {
                original.appendAbilityDescriptionHoverText(viewer, context, consumer);
            }
        } else if (counterpart != null && AerialCompat.sameKind(self.getItem())) {
            // Shadow armor's set-bonus lines live in its own appendHoverText.
            counterpart.appendHoverText(self, context, display, consumer, flag);
        }
    }
}
