package info.mudbourn.mmsweapons.client.spellengine;

import info.mudbourn.mmsweapons.spellengine.SkillLimits;
import info.mudbourn.mmsweapons.spellengine.WeaponSkills;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.client.input.WrappedKeybinding;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

// Lays out the spell hotbar: the player's saved slots first, then relic and focus spells on the mouse keys, then the rest in order.
public final class MouseSlots {

    private static final String MOUSE_KEY_PREFIX = "key.mouse.";

    private MouseSlots() {
    }

    // The options by hotbar slot index, with placeholder options in the gaps.
    public static List<SpellCast.Option> arrange(LocalPlayer player, Options options, List<SpellCast.Option> declared) {
        int slotCount = Keybindings.Wrapped.all().size();
        Set<String> mouseIds = SkillLimits.mouseSpellIds(SpellContainerSource.getSpellsOf(player).sources());
        List<Integer> freeMouse = mouseSlots(options);
        SpellCast.Option[] slots = new SpellCast.Option[Math.max(declared.size(), slotCount)];
        List<SpellCast.Option> unplaced = new ArrayList<>();
        for (SpellCast.Option option : declared) {
            OptionalInt chosen = SkillLimits.isEmpty(option.spell()) ? OptionalInt.empty() : SpellBinds.slotOf(option.id().toString());
            if (chosen.isPresent() && chosen.getAsInt() >= 0 && chosen.getAsInt() < slotCount && slots[chosen.getAsInt()] == null) {
                slots[chosen.getAsInt()] = option;
                freeMouse.remove(Integer.valueOf(chosen.getAsInt()));
            } else {
                unplaced.add(option);
            }
        }
        List<SpellCast.Option> others = new ArrayList<>();
        for (SpellCast.Option option : unplaced) {
            boolean mouse = !SkillLimits.isEmpty(option.spell()) && mouseIds.contains(option.id().toString());
            if (mouse && !freeMouse.isEmpty()) {
                slots[freeMouse.removeFirst()] = option;
            } else {
                others.add(option);
            }
        }
        int next = 0;
        for (SpellCast.Option option : others) {
            while (slots[next] != null) {
                next++;
            }
            slots[next] = option;
        }
        int last = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                last = i;
            }
        }
        SpellCast.Option filler = null;
        List<SpellCast.Option> result = new ArrayList<>();
        for (int i = 0; i <= last; i++) {
            if (slots[i] == null && filler == null) {
                filler = emptyOption(player);
            }
            result.add(slots[i] != null ? slots[i] : filler);
        }
        return result;
    }

    // The hotbar slot indexes whose key is currently bound to a mouse button, in slot order.
    private static List<Integer> mouseSlots(Options options) {
        List<Integer> slots = new ArrayList<>();
        List<WrappedKeybinding> bindings = Keybindings.Wrapped.all();
        for (int i = 0; i < bindings.size(); i++) {
            WrappedKeybinding.Unwrapped bound = bindings.get(i).get(options);
            if (bound != null && bound.keyBinding().saveString().startsWith(MOUSE_KEY_PREFIX)) {
                slots.add(i);
            }
        }
        return slots;
    }

    private static SpellCast.Option emptyOption(Player player) {
        Holder<Spell> empty = SpellRegistry.from(player.level()).get(WeaponSkills.EMPTY).orElseThrow();
        return SpellCast.Option.of(player, empty);
    }
}
