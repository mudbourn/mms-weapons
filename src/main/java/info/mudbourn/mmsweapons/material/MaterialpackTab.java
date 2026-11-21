package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.stream.Collectors;

// Folds Basic Weapons' material pack tab into its main tab.
public final class MaterialpackTab {

    private static final ResourceKey<CreativeModeTab> MAIN = tab("main");
    private static final ResourceKey<CreativeModeTab> MATERIALPACK = tab("materialpack");

    private MaterialpackTab() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(MATERIALPACK).register(output -> output.getDisplayStacks().clear());
        CreativeModeTabEvents.modifyOutputEvent(MAIN).register(output -> {
            Set<Item> shown = output.getDisplayStacks().stream().map(ItemStack::getItem).collect(Collectors.toSet());
            BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> entry.getKey().identifier().getNamespace().equals("basicweapons") && !shown.contains(entry.getValue()))
                .forEach(entry -> output.accept(new ItemStack(entry.getValue()), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
        });
    }

    private static ResourceKey<CreativeModeTab> tab(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("basicweapons", name));
    }
}
