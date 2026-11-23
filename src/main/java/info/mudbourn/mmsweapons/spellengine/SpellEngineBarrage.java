package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.focus.FocusStun;

import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.Focus;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.casting.SpellCast;

// The focus skills as Spell Engine skills, castable only on a full focus bar: mms_weapons:barrage spends it on three
// dashing swings, and the heavy weapons' mms_weapons:shockwave on a stunning shockwave (FocusStun).
public final class SpellEngineBarrage {

    public static final Identifier SPELL = Identifier.fromNamespaceAndPath("mms_weapons", "barrage");
    public static final String SPELL_ID = SPELL.toString();
    /** The heavy weapons' focus skill in the barrage's place: a stunning shockwave (FocusStun). */
    public static final Identifier SHOCKWAVE = Identifier.fromNamespaceAndPath("mms_weapons", "shockwave");
    public static final String SHOCKWAVE_ID = SHOCKWAVE.toString();

    private SpellEngineBarrage() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(StartPayload.TYPE, StartPayload.CODEC);
        SpellHandlers.registerCustomDelivery(SPELL, (level, spell, caster, targets, context, position) -> {
            if (!(caster instanceof ServerPlayer player) || !ready(player.getMainHandItem())) {
                return false;
            }
            Barrage.start(player, player.getMainHandItem());
            ServerPlayNetworking.send(player, new StartPayload());
            return true;
        });
        SpellHandlers.registerCustomDelivery(SHOCKWAVE, (level, spell, caster, targets, context, position) -> {
            if (!(caster instanceof ServerPlayer player) || !ready(player.getMainHandItem())) {
                return false;
            }
            FocusStun.unleash(player, player.getMainHandItem());
            return true;
        });
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            if ((args.spell().is(SPELL) || args.spell().is(SHOCKWAVE)) && !ready(args.caster().getMainHandItem())) {
                return SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo());
            }
            return null;
        });
    }

    // Whether this stack holds a full focus bar on a weapon that builds focus.
    private static boolean ready(ItemStack stack) {
        return Focus.focused(stack) && Focus.builds(stack);
    }

    // Server to client: the caster's own barrage started, so the local swings play.
    public record StartPayload() implements CustomPacketPayload {
        public static final Type<StartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mms_weapons", "start_barrage"));
        public static final StreamCodec<ByteBuf, StartPayload> CODEC = StreamCodec.unit(new StartPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
