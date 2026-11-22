package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.casting.SpellCast;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Focus for spellcasters' weapons: casts fill the bar, and Empower spends it on a 20% stronger next spell.
public final class WizardFocus {

    public static final Identifier SPELL = Identifier.fromNamespaceAndPath("mms_weapons", "empower");
    public static final String SPELL_ID = SPELL.toString();
    public static final double POWER_BONUS = 0.2;
    // How long after the empowered spell's release its hits still count as empowered.
    private static final int WINDOW_TICKS = 60;
    // Casts of the same channelled spell closer than this count as one cast.
    private static final int CHANNEL_GAP_TICKS = 20;

    // Players whose next spell is empowered.
    private static final Map<UUID, Boolean> ARMED = new ConcurrentHashMap<>();
    // Players inside an empowered spell's window, by the spell and the tick the window closes.
    private static final Map<UUID, Window> WINDOWS = new ConcurrentHashMap<>();
    // The last counted cast per player, so a channelled spell counts once.
    private static final Map<UUID, Window> LAST_CAST = new ConcurrentHashMap<>();

    private WizardFocus() {
    }

    public static void register() {
        SpellHandlers.registerCustomDelivery(SPELL, (level, spell, caster, targets, context, position) -> {
            if (!(caster instanceof ServerPlayer player) || !ready(player.getMainHandItem())) {
                return false;
            }
            player.getMainHandItem().set(Focus.FOCUS, 0.0F);
            ARMED.put(player.getUUID(), true);
            ServerLevel server = player.level();
            server.playSound(null, player, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
            server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(0.6), player.getZ(), 40, 0.5, 0.7, 0.5, 0.4);
            return true;
        });
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            if (args.spell().is(SPELL) && (!ready(args.caster().getMainHandItem()) || ARMED.containsKey(args.caster().getUUID()))) {
                return SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo());
            }
            return null;
        });
        SpellEvents.SPELL_CAST.register(args -> onCast(args.caster(), args.spell().unwrapKey().map(key -> key.identifier()).orElse(null), args.action()));
    }

    private static boolean ready(ItemStack stack) {
        return Focus.spellFocus(stack) && Focus.focused(stack);
    }

    // A spell was cast: it fills the bar, and it uses up an armed empowerment.
    private static void onCast(Player caster, Identifier spell, SpellCast.Action action) {
        if (spell == null || caster.level().isClientSide() || spell.getNamespace().equals("mms_weapons") || action == SpellCast.Action.TRIGGER) {
            return;
        }
        long now = caster.level().getGameTime();
        UUID id = caster.getUUID();
        Window last = LAST_CAST.get(id);
        boolean sameCast = action == SpellCast.Action.CHANNEL && last != null && last.spell().equals(spell) && now - last.until() < CHANNEL_GAP_TICKS;
        LAST_CAST.put(id, new Window(spell, now));
        if (sameCast) {
            return;
        }
        Window window = WINDOWS.get(id);
        if (window != null && !window.spell().equals(spell)) {
            WINDOWS.remove(id);
        }
        if (ARMED.remove(id) != null) {
            WINDOWS.put(id, new Window(spell, now + WINDOW_TICKS));
            if (caster.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY(0.6), caster.getZ(), 16, 0.4, 0.6, 0.4, 0.08);
            }
            return;
        }
        Focus.onSpellCast(caster.getMainHandItem());
    }

    // The spell power multiplier for this caster right now: 1.2 inside an empowered spell's window, otherwise 1.
    public static double multiplier(LivingEntity caster) {
        Window window = WINDOWS.get(caster.getUUID());
        if (window == null) {
            return 1.0;
        }
        if (caster.level().getGameTime() > window.until()) {
            WINDOWS.remove(caster.getUUID());
            return 1.0;
        }
        return 1.0 + POWER_BONUS;
    }

    // Whether this player's next spell is empowered.
    public static boolean armed(Player player) {
        return ARMED.containsKey(player.getUUID());
    }

    private record Window(Identifier spell, long until) {
    }
}
