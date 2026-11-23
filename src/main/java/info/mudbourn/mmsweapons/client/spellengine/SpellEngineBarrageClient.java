package info.mudbourn.mmsweapons.client.spellengine;

import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.spellengine.SpellEngineBarrage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

// Plays the local player's barrage swings when the Spell Engine skill starts one.
public final class SpellEngineBarrageClient {

    private SpellEngineBarrageClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
            SpellEngineBarrage.StartPayload.TYPE,
            (payload, context) -> Barrage.start(context.player(), context.player().getMainHandItem()));
        ClientPlayNetworking.registerGlobalReceiver(
            SpellEngineBarrage.WhirlwindPayload.TYPE,
            (payload, context) -> info.mudbourn.mmsweapons.focus.Whirlwind.start(context.player(), context.player().getMainHandItem()));
    }
}
