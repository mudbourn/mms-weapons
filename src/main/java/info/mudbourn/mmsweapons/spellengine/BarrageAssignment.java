package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.focus.Focus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.internals.container.SpellAssignments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Adds the barrage to every Spell Engine weapon skill container whose item builds focus for it.
public final class BarrageAssignment {
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons");

    private BarrageAssignment() {
    }

    public static void assign() {
        AtomicInteger added = new AtomicInteger();
        SpellAssignments.containers.replaceAll((id, container) -> {
            if (container.spell_ids().isEmpty() || container.spell_ids().contains(SpellEngineBarrage.SPELL_ID)) {
                return container;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            if (!Focus.eligible(stack)) {
                return container;
            }
            added.incrementAndGet();
            return container.withAdditionalSpell(List.of(SpellEngineBarrage.SPELL_ID));
        });
        LOG.info("Added the barrage skill to {} Spell Engine weapons.", added.get());
    }
}
