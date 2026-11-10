package info.mudbourn.mmsweapons.client.focus;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.WeaponRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Plays the barrage swings on the local player, and tags each weapon's tooltip with what its right click does.
public final class BarrageClient {

    private static ItemStack weapon = ItemStack.EMPTY;
    private static int ticks = -1;
    private static int played;

    private BarrageClient() {
    }

    public static void register() {
        Barrage.setLocalBarrage(player -> {
            weapon = player.getMainHandItem();
            ticks = 0;
            played = 0;
        });
        ClientTickEvents.END_CLIENT_TICK.register(BarrageClient::tick);
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            Component tag = rightClickTag(stack);
            if (tag != null) {
                lines.add(tag);
            }
        });
    }

    // The tooltip line naming what this weapon's right click does, or null when it has none of ours.
    private static Component rightClickTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (Chainlink.throwable(stack)) {
            return Component.translatable("tooltip.mms_weapons.chain_throw").withStyle(ChatFormatting.GRAY);
        }
        if (stack.has(DataComponents.BLOCKS_ATTACKS) && WeaponRegistry.getAttributes(stack) != null) {
            return Component.translatable("tooltip.mms_weapons.blockable").withStyle(ChatFormatting.GRAY);
        }
        if (stack.is(MmsThrowables.THROWABLE_WEAPONS) || SickleThrowing.isSickle(stack) || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) {
            return Component.translatable("tooltip.mms_weapons.throwable").withStyle(ChatFormatting.GRAY);
        }
        if (Focus.eligible(stack)) {
            return Component.translatable("tooltip.mms_weapons.focus", (int) Focus.focus(stack), (int) Focus.MAX_FOCUS)
                    .withStyle(Focus.focused(stack) ? ChatFormatting.DARK_RED : ChatFormatting.GRAY);
        }
        return null;
    }

    private static void tick(Minecraft client) {
        if (ticks < 0) {
            return;
        }
        if (client.player == null || !ItemStack.isSameItem(client.player.getMainHandItem(), weapon)) {
            ticks = -1;
            return;
        }
        while (played < Barrage.SWINGS && ticks >= played * Barrage.SWING_GAP_TICKS) {
            if (client.player instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(Barrage.animation(weapon, played), Barrage.hand(weapon), Barrage.SWING_LENGTH, Barrage.SWING_UPSWING);
            }
            played++;
        }
        ticks = played >= Barrage.SWINGS ? -1 : ticks + 1;
    }
}
