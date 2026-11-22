package info.mudbourn.mmsweapons.spellengine;

import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.internals.casting.SpellCast;

// A downed player cannot cast spells. "Downed" is AbsolutRevive's unconscious state, read by reflection so the mod is optional.
public final class DownedCasting {

    private DownedCasting() {
    }

    public static void register() {
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> downed(args.caster())
                ? SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo())
                : null);
    }

    public static boolean downed(Player player) {
        return info.mudbourn.mmsweapons.downed.Downed.is(player);
    }
}
