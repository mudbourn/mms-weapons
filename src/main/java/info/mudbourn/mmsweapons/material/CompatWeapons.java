package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// Aerial Hell and Enderscape material versions of Weapons Expanded and Expanded Weaponry weapons.
public final class CompatWeapons {

    private static final TagKey<Item> REPAIRS_RUBY = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_ruby_material"));
    private static final TagKey<Item> REPAIRS_AZURITE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_azurite_material"));
    private static final ToolMaterial AZURITE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 110, 12.0F, 0.0F, 22, REPAIRS_AZURITE);
    private static final ToolMaterial RUBY = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, REPAIRS_RUBY);
    private static final TagKey<Item> REPAIRS_SHADOLINE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enderscape", "repairs_shadoline_armor"));
    private static final ToolMaterial SHADOLINE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 7.0F, 2.5F, 15, REPAIRS_SHADOLINE);
    public static final Map<String, ToolMaterial> MATERIALS = Map.of("azurite", AZURITE, "ruby", RUBY, "shadoline", SHADOLINE);
    private static final Map<String, String> SOURCES = Map.of("azurite", "aerialhell", "ruby", "aerialhell", "shadoline", "enderscape");
    public static final ResourceKey<CreativeModeTab> EXPANDED_WEAPONRY_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("expanded_weaponry", "expanded_weaponry"));
    private static final Map<ResourceKey<CreativeModeTab>, List<Item>> TABS = new LinkedHashMap<>();

    private CompatWeapons() {
    }

    public static void register() {
        FabricLoader loader = FabricLoader.getInstance();
        MATERIALS.forEach((name, material) -> {
            if (!loader.isModLoaded(SOURCES.get(name))) {
                return;
            }
            if (loader.isModLoaded("weaponsexpanded")) {
                WeaponsExpandedWeapons.register(name, material);
            }
            if (loader.isModLoaded("expanded_weaponry")) {
                ExpandedWeaponryWeapons.register(name, material);
            }
        });
        TABS.forEach((tab, items) -> CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> items.forEach(output::accept)));
    }

    static void add(ResourceKey<CreativeModeTab> tab, String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", name));
        TABS.computeIfAbsent(tab, t -> new ArrayList<>()).add(Registry.register(BuiltInRegistries.ITEM, key, factory.apply(name.startsWith("shadoline_") ? NebuliteFuel.fueled(new Item.Properties().setId(key)) : new Item.Properties().setId(key))));
    }
}
