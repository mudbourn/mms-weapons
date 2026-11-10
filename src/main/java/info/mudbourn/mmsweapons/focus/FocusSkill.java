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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

// Held focus skills (the barrage, the Dragon Slayer and Odachi) and their crouch cancel.
// Any crouch while one is held drops it on the client and tells the server to end the use without releasing it,
// so the attack cannot fire. Right click then stays ignored until it is let go and pressed again.
public final class FocusSkill {

    private static final List<Predicate<ItemStack>> SKILLS = new CopyOnWriteArrayList<>();
    // Client side: set by a crouch cancel or a crouching right click, cleared once right click is let go.
    private static boolean locked;
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

    // Lets another mod mark its held skills as crouch-cancellable, such as the Armory's Dragon Slayer.
    public static void addSkill(Predicate<ItemStack> skill) {
        SKILLS.add(skill);
    }

    // Whether this stack's right click is a held focus skill that is ready to use.
    public static boolean isSkill(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (Focus.focused(stack) && Focus.eligible(stack)) {
            return true;
        }
        for (Predicate<ItemStack> skill : SKILLS) {
            if (skill.test(stack)) {
                return true;
            }
        }
        return false;
    }

    // Whether a right click on this skill should be ignored: while crouching, or on the client until right click is let go after a cancel.
    public static boolean blocksStart(Player player, ItemStack stack, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isSkill(stack)) {
            return false;
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
