package info.mudbourn.mmsweapons.focus;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Held focus skills (the barrage, the Dragon Slayer and Odachi) and their crouch cancel.
// Any crouch while one is held drops it on the client and tells the server to end the use without releasing it,
// so the attack cannot fire. Right click then stays ignored until it is let go and pressed again.
public final class FocusSkill {

    // Client side: set by a crouch cancel or a crouching right click, cleared once right click is let go.
    private static boolean locked;
    // Client side: ticks left in the local player's released focus attack, during which swinging and jumping are held off.
    private static int executingTicks;
    // Server side: players whose client cancelled a skill and still holds right click. The client always sends
    // its use packet even when it refuses the use itself, so without this the server restarted the skill and the arms twitched.
    private static final Set<UUID> SERVER_LOCKED = ConcurrentHashMap.newKeySet();

    private FocusSkill() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(CancelPayload.TYPE, CancelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReleasePayload.TYPE, ReleasePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReleasePayload.TYPE, (payload, context) ->
            SERVER_LOCKED.remove(context.player().getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SERVER_LOCKED.remove(handler.getPlayer().getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(CancelPayload.TYPE, (payload, context) -> {
            Player player = context.player();
            SERVER_LOCKED.add(player.getUUID());
            if (player.isUsingItem() && isSkill(player.getUseItem())) {
                // stopUsingItem ends the use without calling releaseUsing, so nothing fires.
                player.stopUsingItem();
            }
        });
    }

    // Whether this stack's right click is a held focus skill that is ready to use.
    public static boolean isSkill(ItemStack stack) {
        return Focus.focused(stack) && Focus.builds(stack);
    }

    // Whether a right click on this skill should be ignored: while airborne or crouching, or on the client until right click is let go after a cancel.
    public static boolean blocksStart(Player player, ItemStack stack, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isSkill(stack)) {
            return false;
        }
        // A charge started in the air would carry a jump's momentum into the lunge.
        if (!player.onGround()) {
            return true;
        }
        if (player.isShiftKeyDown()) {
            if (player.level().isClientSide()) {
                locked = true;
            }
            return true;
        }
        return player.level().isClientSide() ? locked : SERVER_LOCKED.contains(player.getUUID());
    }

    public static void lock() {
        locked = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static boolean locked() {
        return locked;
    }

    // Marks the local player's focus attack as executing for this many ticks.
    public static void execute(int ticks) {
        executingTicks = Math.max(executingTicks, ticks);
    }

    // Whether this player is charging a focus skill or, on the client, still executing one, so jumping and swinging are held off.
    public static boolean holdsOff(Player player) {
        return executingTicks > 0 || player.isUsingItem() && isSkill(player.getUseItem());
    }

    // Whether the local player's focus attack is still executing, counting one tick down per call.
    public static boolean tickExecuting() {
        if (executingTicks <= 0) {
            return false;
        }
        executingTicks--;
        return true;
    }

    // Client to server: right click was let go after a cancel, so skills may start again.
    public record ReleasePayload() implements CustomPacketPayload {
        public static final Type<ReleasePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mms_weapons", "release_focus_skill"));
        public static final StreamCodec<ByteBuf, ReleasePayload> CODEC = StreamCodec.unit(new ReleasePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Client to server: drop the held focus skill without firing it.
    public record CancelPayload() implements CustomPacketPayload {
        public static final Type<CancelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mms_weapons", "cancel_focus_skill"));
        public static final StreamCodec<ByteBuf, CancelPayload> CODEC = StreamCodec.unit(new CancelPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
