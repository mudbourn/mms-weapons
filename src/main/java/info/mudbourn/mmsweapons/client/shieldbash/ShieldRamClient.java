package info.mudbourn.mmsweapons.client.shieldbash;

import info.mudbourn.mmsweapons.shieldbash.ShieldWeapons;

import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import net.combat_roll.CombatRollMod;
import net.combat_roll.client.Keybindings;
import net.combat_roll.internals.RollManager;
import net.combat_roll.internals.RollingEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

// Turns a combat roll pressed while blocking into a shield ram, spending the same roll charge and cooldown.
public final class ShieldRamClient {

    private ShieldRamClient() {
    }

    public static void tryRam(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.isPaused() || client.gui.screen() != null || !Keybindings.roll.isDown() || !player.isBlocking()
            || player.getItemBlockingWith() == null || !ShieldWeapons.isShield(player.getItemBlockingWith().getItem())) {
            return;
        }
        RollManager roll = ((RollingEntity) player).getRollManager();
        if (!roll.isRollAvailable(player) || !canRoll(player)) {
            return;
        }
        roll.onRoll(player);
        Vec3 direction = Vec3.directionFromRotation(0.0F, player.getYRot());
        player.setDeltaMovement(direction.x * ShieldRam.RAM_IMPULSE, player.getDeltaMovement().y, direction.z * ShieldRam.RAM_IMPULSE);
        ClientPlayNetworking.send(new ShieldRam.RamPayload());
    }

    private static boolean canRoll(LocalPlayer player) {
        return (CombatRollMod.config.allow_rolling_while_airborn || player.onGround())
            && player.getFoodData().getFoodLevel() > CombatRollMod.config.food_level_required
            && !player.isSwimming()
            && !player.isVisuallyCrawling()
            && player.getVehicle() == null;
    }
}
