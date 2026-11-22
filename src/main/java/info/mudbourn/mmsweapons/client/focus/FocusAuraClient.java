package info.mudbourn.mmsweapons.client.focus;

import info.mudbourn.mmsweapons.focus.Focus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// A loose smoke aura around a spellcaster with a full focus bar.
public final class FocusAuraClient {

    private static final DustParticleOptions PARTICLE = new DustParticleOptions(0x101010, 0.7F);
    private static final int PER_TICK = 2;
    // How far past the hitbox the aura scatters.
    private static final double SPREAD = 0.45;

    private FocusAuraClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FocusAuraClient::tick);
    }

    private static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.isPaused()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Player player : level.players()) {
            ItemStack held = player.getMainHandItem();
            if (!Focus.spellFocus(held) || !Focus.showsSmoke(held) || player.isInvisible()) {
                continue;
            }
            // Skip the local player's own aura in first person, where it would only cloud the view.
            if (player == client.player && client.options.getCameraType().isFirstPerson()) {
                continue;
            }
            double width = player.getBbWidth() / 2.0 + SPREAD;
            for (int i = 0; i < PER_TICK; i++) {
                level.addParticle(PARTICLE,
                        player.getX() + (random.nextDouble() * 2.0 - 1.0) * width,
                        player.getY() + random.nextDouble() * player.getBbHeight(),
                        player.getZ() + (random.nextDouble() * 2.0 - 1.0) * width,
                        0.0, 0.02, 0.0);
            }
        }
    }
}
