package info.mudbourn.mmsweapons.client.focus;

import info.mudbourn.mmsweapons.focus.FocusSkill;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

// Holds off swinging and jumping while a focus attack executes.
public final class FocusHoldOffClient {

    private FocusHoldOffClient() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(FocusHoldOffClient::holdOffWhileExecuting);
    }

    // Runs before the tick reads input, so no swing or jump starts while a focus attack executes.
    private static void holdOffWhileExecuting(Minecraft client) {
        if (!FocusSkill.tickExecuting()) {
            return;
        }
        client.options.keyAttack.setDown(false);
        while (client.options.keyAttack.consumeClick()) {
            // Drop swings queued during the attack.
        }
        client.options.keyJump.setDown(false);
    }
}
