package info.mudbourn.mmsweapons.focus;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// A dash pushes the player sideways at the normal 0.6 step height, so a one-block ledge or the
// lip of a one-block hole stops it dead. While a dash runs, the player steps up a full block.
// The modifier is set on the server and syncs to the client, which moves the player.
public final class DashStep {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("mms_weapons", "dash_step");
    // Added to the 0.6 base, for 1.1 in all: one full block with room to spare.
    private static final double EXTRA_STEP = 0.5;

    private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();

    private DashStep() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DashStep::tick);
    }

    // Lets the player step up a full block for the next `ticks` ticks, extending any boost already running.
    public static void boost(ServerPlayer player, int ticks) {
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) {
            return;
        }
        long until = player.level().getGameTime() + ticks;
        UNTIL.merge(player.getUUID(), until, Math::max);
        if (!step.hasModifier(ID)) {
            step.addTransientModifier(new AttributeModifier(ID, EXTRA_STEP, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> it = UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            if (player.level().getGameTime() >= entry.getValue()) {
                AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
                if (step != null) {
                    step.removeModifier(ID);
                }
                it.remove();
            }
        }
    }
}
