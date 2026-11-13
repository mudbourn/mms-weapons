package info.mudbourn.mmsweapons.client.focus;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmsweapons.focus.FocusSkill;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

// Watches the crouch key every tick, taps shorter than a tick included, and cancels a held focus skill the moment it is pressed.
// Also holds off swinging and jumping while a focus attack charges or executes.
public final class FocusCancelClient {

    // Whether the server has been told about the current lock.
    private static boolean serverLocked;

    private FocusCancelClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FocusCancelClient::tick);
        ClientTickEvents.START_CLIENT_TICK.register(FocusCancelClient::holdOffWhileExecuting);
    }

    // Whether the button bound to use is really down right now, read from the window rather than the key binding.
    private static boolean useKeyPhysicallyHeld(Minecraft client) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(client.options.keyUse);
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(client.getWindow(), key.getValue());
    }

    // Runs before the tick reads input, so no swing or jump starts while a focus attack charges or executes.
    private static void holdOffWhileExecuting(Minecraft client) {
        boolean executing = FocusSkill.tickExecuting();
        LocalPlayer player = client.player;
        boolean charging = player != null && player.isUsingItem() && FocusSkill.isSkill(player.getUseItem());
        if (!executing && !charging) {
            return;
        }
        client.options.keyAttack.setDown(false);
        while (client.options.keyAttack.consumeClick()) {
            // Drop swings queued during the attack.
        }
        client.options.keyJump.setDown(false);
    }

    private static void tick(Minecraft client) {
        // Sneaking reads the key's held state, never its clicks, so draining them here does not take anything from it.
        boolean crouched = client.options.keyShift.isDown();
        while (client.options.keyShift.consumeClick()) {
            crouched = true;
        }
        LocalPlayer player = client.player;
        if (player != null && crouched && player.isUsingItem() && FocusSkill.isSkill(player.getUseItem())) {
            // Dropped locally without a release, so no release packet goes out; the server is told to drop it too.
            player.stopUsingItem();
            ClientPlayNetworking.send(new FocusSkill.CancelPayload());
            FocusSkill.lock();
            serverLocked = true;
        }
        if (!FocusSkill.locked()) {
            return;
        }
        if (!serverLocked && client.getConnection() != null) {
            // A crouching right click locks only the client, so the server is locked too and cannot start the skill once the crouch ends.
            ClientPlayNetworking.send(new FocusSkill.CancelPayload());
            serverLocked = true;
        }
        if (useKeyPhysicallyHeld(client)) {
            // Keep the game's right click released for as long as the button is really held, so nothing restarts.
            client.options.keyUse.setDown(false);
            while (client.options.keyUse.consumeClick()) {
                // Drop presses queued while locked.
            }
        } else {
            FocusSkill.unlock();
            serverLocked = false;
            if (client.getConnection() != null) {
                ClientPlayNetworking.send(new FocusSkill.ReleasePayload());
            }
        }
    }
}
