package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.throwable.Hammers;
import info.mudbourn.mmsweapons.throwable.MaceSmash;

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

// The weapon throws, shield ram, hammer slam and empty placeholder as Spell Engine skills.
public final class WeaponSkills {

    public static final Identifier EMPTY = Identifier.fromNamespaceAndPath("mms_weapons", "empty");
    public static final Identifier WEAPON_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "weapon_throw");
    public static final Identifier DAGGER_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "dagger_throw");
    public static final Identifier SICKLE_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "sickle_throw");
    public static final Identifier CHAIN_THROW = Identifier.fromNamespaceAndPath("mms_weapons", "chain_throw");
    public static final Identifier SHIELD_RAM = Identifier.fromNamespaceAndPath("mms_weapons", "shield_ram");
    public static final Identifier HAMMER_SLAM = Identifier.fromNamespaceAndPath("mms_weapons", "hammer_slam");
    public static final Identifier HULK_SMASH = Identifier.fromNamespaceAndPath("mms_weapons", "hulk_smash");
    public static final Identifier HULK_HAMMER = Identifier.fromNamespaceAndPath("mutantmonsters", "hulk_hammer");

    private static final Map<Identifier, Predicate<Player>> SKILLS = new HashMap<>();

    private WeaponSkills() {
    }

    public static void register() {
        define(WEAPON_THROW, player -> MmsThrowables.canThrow(player, player.getMainHandItem()),
            player -> MmsThrowables.throwWeapon(player, player.getMainHandItem()));
        // Daggers throw exactly as other throwables do; the skill is separate only for its icon and name.
        define(DAGGER_THROW, player -> MmsThrowables.canThrow(player, player.getMainHandItem()),
            player -> MmsThrowables.throwWeapon(player, player.getMainHandItem()));
        define(SICKLE_THROW, player -> SickleThrowing.canThrow(player, player.getMainHandItem()),
            player -> SickleThrowing.throwSickle(player, player.getMainHandItem()));
        define(CHAIN_THROW, player -> Chainlink.canThrow(player, player.getMainHandItem()),
            player -> Chainlink.throwChain(player, player.getMainHandItem()));
        define(SHIELD_RAM, player -> ShieldRam.ramShield(player) != null,
            player -> ShieldRam.start(player, ShieldRam.ramShield(player)));
        define(HAMMER_SLAM, player -> Hammers.slamFall(player.getMainHandItem().getItem()) > 0.0F,
            player -> MaceSmash.slam(player, Hammers.slamFall(player.getMainHandItem().getItem())));
        // The mutant zombie's smash: Mutant Monsters' own hulk hammer ground use, a line of seismic waves ahead.
        define(HULK_SMASH, player -> player.onGround() && isHulkHammer(player.getMainHandItem()), WeaponSkills::hulkSmash);
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

    public static boolean isHulkHammer(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(HULK_HAMMER);
    }

    // Runs the hammer's own use on the block under the player's feet, as if they'd right-clicked its top.
    private static void hulkSmash(ServerPlayer player) {
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.1, player.getZ());
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(below).add(0.0, 0.5, 0.0), net.minecraft.core.Direction.UP, below, false);
        player.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
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
