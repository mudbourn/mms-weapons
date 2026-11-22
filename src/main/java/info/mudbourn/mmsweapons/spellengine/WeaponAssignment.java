package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.shieldbash.ShieldWeapons;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainers;
import net.spell_engine.internals.container.SpellAssignments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// Lays out each weapon's skills by slot: the barrage second, its own skill third, and the shield ram first.
public final class WeaponAssignment {
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons");

    private static final int OWN_SKILL_SLOT = 2;

    private WeaponAssignment() {
    }

    public static void assign() {
        int barrages = 0;
        int skills = 0;
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            SpellContainer container = SpellAssignments.containers.get(id);
            List<String> spells = new ArrayList<>(container == null ? List.of() : container.spell_ids());
            boolean changed = false;
            // A spellcaster's weapon gets Empower instead of the barrage, added by SpellContainerEmpowerMixin.
            if (Focus.spellFocus(stack)) {
                continue;
            }
            if (!spells.isEmpty() && !spells.contains(SpellEngineBarrage.SPELL_ID) && Focus.eligible(stack)) {
                spells.add(SpellEngineBarrage.SPELL_ID);
                barrages++;
                changed = true;
            }
            Identifier own = ShieldWeapons.isShield(item) ? WeaponSkills.SHIELD_RAM : ownSkill(stack);
            if (own != null) {
                place(spells, own);
                skills++;
                changed = true;
            }
            if (changed) {
                SpellAssignments.containers.put(id, build(container, spells, own == WeaponSkills.SHIELD_RAM));
            }
        }
        LOG.info("Added the barrage skill to {} and an own skill to {} Spell Engine items.", barrages, skills);
    }

    // The skill this item casts on its own, or null when it has none.
    private static Identifier ownSkill(ItemStack stack) {
        if (stack.is(MmsThrowables.THROWABLE_WEAPONS)) {
            return WeaponSkills.WEAPON_THROW;
        }
        if (SickleThrowing.isSickle(stack)) {
            return WeaponSkills.SICKLE_THROW;
        }
        if (Chainlink.throwable(stack)) {
            return WeaponSkills.CHAIN_THROW;
        }
        return null;
    }

    // Puts the skill in the third slot, padding with empty skills; the shield ram goes first.
    private static void place(List<String> spells, Identifier skill) {
        String id = skill.toString();
        spells.remove(id);
        if (skill.equals(WeaponSkills.SHIELD_RAM)) {
            spells.addFirst(id);
            return;
        }
        while (spells.size() < OWN_SKILL_SLOT) {
            spells.add(WeaponSkills.EMPTY.toString());
        }
        spells.add(OWN_SKILL_SLOT, id);
    }

    // A shield's container is a main-hand weapon container, so only a shield held in the main hand gives skills.
    private static SpellContainer build(SpellContainer existing, List<String> spells, boolean shield) {
        if (shield) {
            return SpellContainers.forWeapon(SpellContainer.ContentType.MAGIC, spells.stream().map(Identifier::parse).toList());
        }
        SpellContainer base = existing != null ? existing : SpellContainers.forMeleeWeapon();
        return base.copyWith(spells);
    }
}
