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

// Aerial Hell and Enderscape material versions of Weapons Expanded, Expanded Weaponry and Paladins weapons.
public final class CompatWeapons {

    private static final TagKey<Item> REPAIRS_RUBY = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_ruby_material"));
    private static final TagKey<Item> REPAIRS_AZURITE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_azurite_material"));
    private static final ToolMaterial AZURITE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 110, 12.0F, 0.0F, 22, REPAIRS_AZURITE);
    private static final ToolMaterial RUBY = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, REPAIRS_RUBY);
    private static final TagKey<Item> REPAIRS_ARSONIST = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_arsonist_material"));
    private static final ToolMaterial ARSONIST = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 5.5F, 15, REPAIRS_ARSONIST);
    private static final TagKey<Item> REPAIRS_LUNATIC = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_lunatic_material"));
    private static final ToolMaterial LUNATIC = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1712, 8.0F, 4.0F, 15, REPAIRS_LUNATIC);
    private static final TagKey<Item> REPAIRS_MAGMATIC_GEL = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_magmatic_gel_material"));
    private static final ToolMaterial MAGMATIC_GEL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 32, 12.0F, 0.0F, 22, REPAIRS_MAGMATIC_GEL);
    private static final TagKey<Item> REPAIRS_OBSIDIAN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_obsidian_material"));
    private static final ToolMaterial OBSIDIAN = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2031, 8.0F, 3.0F, 10, REPAIRS_OBSIDIAN);
    private static final TagKey<Item> REPAIRS_VOLUCITE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_volucite_material"));
    private static final ToolMaterial VOLUCITE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1620, 8.5F, 4.5F, 10, REPAIRS_VOLUCITE);
    private static final TagKey<Item> REPAIRS_SKY_WOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "aerialhell_planks"));
    private static final ToolMaterial SKY_WOOD = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15, REPAIRS_SKY_WOOD);
    private static final TagKey<Item> REPAIRS_STELLAR_STONE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_stellar_stone_material"));
    private static final ToolMaterial STELLAR_STONE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 1.0F, 5, REPAIRS_STELLAR_STONE);
    private static final TagKey<Item> REPAIRS_SHADOLINE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enderscape", "repairs_shadoline_armor"));
    private static final ToolMaterial SHADOLINE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 7.0F, 2.5F, 15, REPAIRS_SHADOLINE);
    private static final TagKey<Item> REPAIRS_SHADOW = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_shadow_material"));
    // Aerial Hell's own SHADOW tool material, which it defines but uses for no tools of its own.
    private static final ToolMaterial SHADOW = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 112, 8.0F, 3.0F, 15, REPAIRS_SHADOW);
    public static final Map<String, ToolMaterial> MATERIALS = Map.ofEntries(Map.entry("azurite", AZURITE), Map.entry("ruby", RUBY), Map.entry("shadoline", SHADOLINE), Map.entry("arsonist", ARSONIST), Map.entry("lunatic", LUNATIC), Map.entry("magmatic_gel", MAGMATIC_GEL), Map.entry("obsidian", OBSIDIAN), Map.entry("volucite", VOLUCITE), Map.entry("sky_wood", SKY_WOOD), Map.entry("stellar_stone", STELLAR_STONE), Map.entry("shadow", SHADOW));
    private static final Map<String, String> SOURCES = Map.ofEntries(Map.entry("azurite", "aerialhell"), Map.entry("ruby", "aerialhell"), Map.entry("shadoline", "enderscape"), Map.entry("arsonist", "aerialhell"), Map.entry("lunatic", "aerialhell"), Map.entry("magmatic_gel", "aerialhell"), Map.entry("obsidian", "aerialhell"), Map.entry("volucite", "aerialhell"), Map.entry("sky_wood", "aerialhell"), Map.entry("stellar_stone", "aerialhell"), Map.entry("shadow", "aerialhell"));
    public static final ResourceKey<CreativeModeTab> EXPANDED_WEAPONRY_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("expanded_weaponry", "expanded_weaponry"));
    private static final Map<ResourceKey<CreativeModeTab>, List<Item>> TABS = new LinkedHashMap<>();

    private CompatWeapons() {
    }

    /** The mod a material comes from, by the item or material name's prefix. */
    static String source(String name) {
        return SOURCES.entrySet().stream().filter(e -> name.startsWith(e.getKey() + "_") || name.equals(e.getKey()))
            .max((a, b) -> a.getKey().length() - b.getKey().length()).map(Map.Entry::getValue).orElse("mms_weapons");
    }

    public static void register() {
        FabricLoader loader = FabricLoader.getInstance();
        MATERIALS.forEach((name, material) -> {
            if (!loader.isModLoaded(SOURCES.get(name))) {
                return;
            }
            if (loader.isModLoaded("weaponsexpanded")) {
                CompatOrigin.weaponMod("weaponsexpanded");
                WeaponsExpandedWeapons.register(name, material);
            }
            if (loader.isModLoaded("expanded_weaponry")) {
                CompatOrigin.weaponMod("expanded_weaponry");
                ExpandedWeaponryWeapons.register(name, material);
            }
            if (loader.isModLoaded("paladins") && loader.isModLoaded("spell_engine")) {
                CompatOrigin.weaponMod("paladins");
                PaladinsWeapons.register(name, material);
            }
            if (name.equals("arsonist") && (loader.isModLoaded("wizards") || loader.isModLoaded("paladins")) && loader.isModLoaded("spell_engine")) {
                CompatOrigin.weaponMod(loader.isModLoaded("wizards") ? "wizards" : "paladins");
                WizardsGear.register(material);
            }
            if (name.equals("shadow")) {
                CompatOrigin.weaponMod(null);
                ShadowTools.register(material);
            }
        });
        TABS.forEach((tab, items) -> CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> items.forEach(output::accept)));
    }

    static void add(ResourceKey<CreativeModeTab> tab, String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(name.startsWith("shadoline_") ? NebuliteFuel.fueled(new Item.Properties().setId(key)) : new Item.Properties().setId(key)));
        TABS.computeIfAbsent(tab, t -> new ArrayList<>()).add(item);
        CompatOrigin.record(item, source(name));
    }
}
