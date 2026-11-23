package info.mudbourn.mmsweapons.downed;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

// "Downed" is AbsolutRevive's unconscious state, read by reflection so the mod is optional.
public final class Downed {

    private static final Method GET_MODEL;
    private static final Method IS_UNCONSCIOUS;
    private static final Method RESCUE;

    static {
        Method getModel = null;
        Method isUnconscious = null;
        Method rescue = null;
        try {
            Class<?> holder = Class.forName("goetic.mods.absolutrevive.common.AbsolutReviveDamageModelHolder");
            getModel = holder.getMethod("absolutrevive$getDamageModelNullable");
            isUnconscious = getModel.getReturnType().getMethod("isUnconscious");
            rescue = Class.forName("goetic.mods.absolutrevive.common.damagesystem.PlayerDamageModel").getMethod("rescueFromCriticalState", Player.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // AbsolutRevive is not installed, so nobody is ever downed.
        }
        GET_MODEL = getModel;
        IS_UNCONSCIOUS = isUnconscious;
        RESCUE = rescue;
    }

    private Downed() {
    }

    // A downed player can't keep riding: each server tick, anyone downed is thrown off their mount (a dragon, a
    // horse, a boat), and so can't climb back on until revived.
    public static void register() {
        if (GET_MODEL == null) {
            return;
        }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isPassenger() && is(player)) {
                    player.stopRiding();
                }
            }
        });
    }

    public static boolean is(Player player) {
        if (GET_MODEL == null || !GET_MODEL.getDeclaringClass().isInstance(player)) {
            return false;
        }
        try {
            Object model = GET_MODEL.invoke(player);
            return model != null && (boolean) IS_UNCONSCIOUS.invoke(model);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    // Revives a downed player the way AbsolutRevive's own rescue does; returns false if they couldn't be revived.
    public static boolean revive(Player player) {
        if (RESCUE == null || !is(player)) {
            return false;
        }
        try {
            Object model = GET_MODEL.invoke(player);
            return RESCUE.getDeclaringClass().isInstance(model) && (boolean) RESCUE.invoke(model, player);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
