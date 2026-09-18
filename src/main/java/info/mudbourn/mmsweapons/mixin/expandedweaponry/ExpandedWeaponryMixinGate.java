package info.mudbourn.mmsweapons.mixin.expandedweaponry;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.util.List;
import java.util.Set;

/**
 * Gates the Expanded Weaponry mixins on the mod being loaded.
 *
 * <p>The draw-sound mixin needs a second guard: it calls into the Sounds mod's
 * config to reuse the same sound instance vanilla bows get, so it can only
 * apply when both mods are present.
 */
public class ExpandedWeaponryMixinGate implements IMixinConfigPlugin {

    private final boolean present = FabricLoader.getInstance().isModLoaded("expanded_weaponry");
    private final boolean soundsPresent = FabricLoader.getInstance().isModLoaded("sounds");

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!present) {
            return false;
        }
        return soundsPresent || !mixinClassName.endsWith("LongbowDrawSoundMixin");
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {}
}
