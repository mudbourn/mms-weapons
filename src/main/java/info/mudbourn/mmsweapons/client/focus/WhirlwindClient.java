package info.mudbourn.mmsweapons.client.focus;

import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.FocusSkill;
import info.mudbourn.mmsweapons.focus.Whirlwind;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

// Plays the whirlwind spins on the local player, who gets no Better Combat packet for their own attack.
public final class WhirlwindClient {

    private static ItemStack weapon = ItemStack.EMPTY;
    private static int ticks = -1;
    private static int played;

    private WhirlwindClient() {
    }

    public static void register() {
        Whirlwind.setLocal(player -> {
            weapon = player.getMainHandItem();
            ticks = 0;
            played = 0;
            FocusSkill.execute((Whirlwind.WAVES - 1) * Whirlwind.WAVE_GAP_TICKS + (int) Whirlwind.SWING_LENGTH);
        });
        ClientTickEvents.END_CLIENT_TICK.register(WhirlwindClient::tick);
    }

    private static void tick(Minecraft client) {
        if (ticks < 0) {
            return;
        }
        if (client.player == null || !ItemStack.isSameItem(client.player.getMainHandItem(), weapon)) {
            ticks = -1;
            return;
        }
        while (played < Whirlwind.WAVES && ticks >= played * Whirlwind.WAVE_GAP_TICKS) {
            if (client.player instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(Whirlwind.ANIMATION, Barrage.hand(weapon), Whirlwind.SWING_LENGTH, Whirlwind.SWING_UPSWING);
            }
            played++;
        }
        ticks = played >= Whirlwind.WAVES ? -1 : ticks + 1;
    }
}
