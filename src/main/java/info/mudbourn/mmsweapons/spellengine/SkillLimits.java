package info.mudbourn.mmsweapons.spellengine;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Which skills a player may use: none from the off hand, at most four, or six in a full wizard robe.
public final class SkillLimits {

    public static final int MAX_SKILLS = 4;
    // The most the spell hotbar shows at once, even in a full wizard robe.
    public static final int MAX_SKILLS_ROBE = 6;

    public static final TagKey<Item> RELICS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("relics_rpgs", "all"));
    public static final TagKey<Item> WIZARD_ROBE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("rpg_series", "armor_type/magic"));

    private static final String OFF_HAND = "off_hand";

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    );

    private SkillLimits() {
    }

    public static boolean fullRobe(Player player) {
        return ARMOR_SLOTS.stream().allMatch(slot -> player.getItemBySlot(slot).is(WIZARD_ROBE));
    }

    // The containers that may give usable skills: everything except the off hand.
    public static List<SpellContainerSource.SourcedContainer> withoutOffHand(List<SpellContainerSource.SourcedContainer> sources) {
        return sources.stream().filter(source -> !OFF_HAND.equals(source.name())).toList();
    }

    // The spell ids a relic grants, plus the wizard focus spell.
    public static Set<String> mouseSpellIds(List<SpellContainerSource.SourcedContainer> sources) {
        Set<String> ids = new HashSet<>();
        ids.add(WizardFocus.SPELL_ID);
        for (SpellContainerSource.SourcedContainer source : sources) {
            if (source.itemStack() != null && source.itemStack().is(RELICS)) {
                ids.addAll(source.container().spell_ids());
            }
        }
        return ids;
    }

    public static boolean isEmpty(Holder<Spell> spell) {
        return spell.is(WeaponSkills.EMPTY);
    }

    public static String idOf(Holder<Spell> spell) {
        return spell.unwrapKey().orElseThrow().identifier().toString();
    }

    // Keeps the mouse spells first and then the rest in order until the cap, dropping placeholders that no kept spell follows.
    public static SpellContainerSource.MergeResult cap(
        Player player,
        List<SpellContainerSource.SourcedContainer> sources,
        SpellContainerSource.MergeResult merged
    ) {
        List<Holder<Spell>> spells = merged.spells();
        int max = fullRobe(player) ? MAX_SKILLS_ROBE : MAX_SKILLS;
        if (spells.stream().filter(spell -> !isEmpty(spell)).count() <= max) {
            return merged;
        }
        Set<String> mouse = mouseSpellIds(sources);
        boolean[] kept = new boolean[spells.size()];
        int count = 0;
        for (boolean mousePass : new boolean[] {true, false}) {
            for (int i = 0; i < spells.size() && count < max; i++) {
                Holder<Spell> spell = spells.get(i);
                if (!kept[i] && !isEmpty(spell) && mouse.contains(idOf(spell)) == mousePass) {
                    kept[i] = true;
                    count++;
                }
            }
        }
        int last = -1;
        for (int i = 0; i < kept.length; i++) {
            if (kept[i]) {
                last = i;
            }
        }
        List<Holder<Spell>> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i <= last; i++) {
            if (kept[i] || isEmpty(spells.get(i))) {
                result.add(spells.get(i));
                ids.add(idOf(spells.get(i)));
            }
        }
        return new SpellContainerSource.MergeResult(merged.container().copyWith(new ArrayList<>(ids)), result);
    }
}
