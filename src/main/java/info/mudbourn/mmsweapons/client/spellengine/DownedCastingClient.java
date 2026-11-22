package info.mudbourn.mmsweapons.client.spellengine;

import info.mudbourn.mmsweapons.spellengine.DownedCasting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.spell_engine.internals.casting.SpellCaster;

// Cancels the local player's spell cast the moment they are downed, including a cast already charging or channelling.
public final class DownedCastingClient {

    private DownedCastingClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player instanceof SpellCaster.Client caster && caster.isCastingSpell() && DownedCasting.downed(client.player)) {
                caster.cancelSpellCast();
            }
        });
    }
}
