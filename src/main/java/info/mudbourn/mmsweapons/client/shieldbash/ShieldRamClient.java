package info.mudbourn.mmsweapons.client.shieldbash;

import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.phys.Vec3;

// Dashes the local player when their shield ram skill starts.
public final class ShieldRamClient {

    // How long the ram holds the guard up without right click being held: the dash plus a few ticks.
    private static final int GUARD_HOLD_TICKS = 14;
    private static int guardTicks;

    private ShieldRamClient() {
    }

    // Whether the ram is still holding the local player's guard up, so letting go of right click does not drop it.
    public static boolean holdsGuard() {
        return guardTicks > 0;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ShieldRam.DashPayload.TYPE, (payload, context) -> {
            // A ram cast with the guard down puts the shield up and holds it there for the dash.
            ShieldRam.raiseGuard(context.player());
            guardTicks = GUARD_HOLD_TICKS;
            Vec3 direction = Vec3.directionFromRotation(0.0F, context.player().getYRot());
            Vec3 motion = context.player().getDeltaMovement();
            context.player().setDeltaMovement(direction.x * ShieldRam.RAM_IMPULSE, motion.y, direction.z * ShieldRam.RAM_IMPULSE);
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (guardTicks > 0 && --guardTicks == 0 && client.player != null && client.gameMode != null
                && client.player.isUsingItem() && !client.options.keyUse.isDown()) {
                client.gameMode.releaseUsingItem(client.player);
            }
        });
    }
}
