package info.mudbourn.mmsweapons.mixin.spellengine;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import info.mudbourn.mmsweapons.spellengine.SkillLimits;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.internals.container.SpellContainerSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Resolves a player's usable skills without the off hand and within the skill cap, on both client and server.
@Mixin(value = SpellContainerSource.class, remap = false)
public abstract class SpellActivesMixin {

    @WrapOperation(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/spell_engine/internals/container/SpellContainerSource;mergedContainerSources1(Ljava/util/List;Lnet/spell_engine/api/spell/container/SpellContainer;Lnet/spell_engine/api/spell/Spell$Type;Lnet/minecraft/world/level/Level;)Lnet/spell_engine/internals/container/SpellContainerSource$MergeResult;"
        )
    )
    private static SpellContainerSource.MergeResult mmsWeapons$limitActives(
        List<SpellContainerSource.SourcedContainer> sources,
        SpellContainer held,
        Spell.Type type,
        Level world,
        Operation<SpellContainerSource.MergeResult> original,
        @Local(argsOnly = true) Player player
    ) {
        if (type != Spell.Type.ACTIVE) {
            return original.call(sources, held, type, world);
        }
        List<SpellContainerSource.SourcedContainer> usable = SkillLimits.withoutOffHand(sources);
        return SkillLimits.cap(player, usable, original.call(usable, held, type, world));
    }
}
