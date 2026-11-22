package info.mudbourn.mmsweapons.client.shieldbash;

import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.phys.Vec3;

// Dashes the local player when their shield ram skill starts.
public final class ShieldRamClient {

    private ShieldRamClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ShieldRam.DashPayload.TYPE, (payload, context) -> {
            Vec3 direction = Vec3.directionFromRotation(0.0F, context.player().getYRot());
            Vec3 motion = context.player().getDeltaMovement();
            context.player().setDeltaMovement(direction.x * ShieldRam.RAM_IMPULSE, motion.y, direction.z * ShieldRam.RAM_IMPULSE);
        });
    }
}
