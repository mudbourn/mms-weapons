package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.casting.SpellCast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

// The weapon throws, shield ram and empty placeholder as Spell Engine skills.
public final class WeaponSkills {

    public static final Identifier EMPTY = Identifier.fromNamespaceAndPath("mms_weapons", "empty");
    public static final Identifier WEAPON_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "weapon_throw");
    public static final Identifier SICKLE_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "sickle_throw");
    public static final Identifier CHAIN_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "chain_throw");
    public static final Identifier SHIELD_RAM = Identifier.fromNamespaceAndPath("mms_weapons", "shield_ram");

    private static final Map<Identifier, Predicate<Player>> SKILLS = new HashMap<>();

    private WeaponSkills() {
    }

    public static void register() {
        define(WEAPON_THROW, player -> MmsThrowables.canThrow(player, player.getMainHandItem()),
            player -> MmsThrowables.throwWeapon(player, player.getMainHandItem()));
        define(SICKLE_THROW, player -> SickleThrowing.canThrow(player, player.getMainHandItem()),
            player -> SickleThrowing.throwSickle(player, player.getMainHandItem()));
        define(CHAIN_THROW, player -> Chainlink.canThrow(player, player.getMainHandItem()),
            player -> Chainlink.throwChain(player, player.getMainHandItem()));
        define(SHIELD_RAM, player -> ShieldRam.ramShield(player) != null,
            player -> ShieldRam.start(player, ShieldRam.ramShield(player)));
        SpellHandlers.registerCustomDelivery(EMPTY, (level, spell, caster, targets, context, position) -> false);
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            if (args.spell().is(EMPTY)) {
                return SpellCast.Attempt.none();
            }
            for (Map.Entry<Identifier, Predicate<Player>> entry : SKILLS.entrySet()) {
                if (args.spell().is(entry.getKey()) && !entry.getValue().test(args.caster())) {
                    return SpellCast.Attempt.none();
                }
            }
            return null;
        });
    }

    // Binds a skill to when it may be cast and what the cast does.
    private static void define(Identifier spell, Predicate<Player> ready, Consumer<ServerPlayer> cast) {
        SKILLS.put(spell, ready);
        SpellHandlers.registerCustomDelivery(spell, (level, data, caster, targets, context, position) -> {
            if (!(caster instanceof ServerPlayer player) || !SKILLS.get(spell).test(player)) {
                return false;
            }
            cast.accept(player);
            return true;
        });
    }
}
