package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.ArrayList;
import java.util.List;

// Expanded Weaponry-style heavy armor in Aerial Hell and Enderscape materials.
public final class HeavyArmors {

    private static final double SLOWNESS = -0.04;
    private static final List<Item> ITEMS = new ArrayList<>();

    private HeavyArmors() {
    }

    public static void register() {
        if (FabricLoader.getInstance().isModLoaded("aerialhell")) {
            azurite();
            ruby();
            // Aerial Hell's own armor, one tier heavier: +1 armor per slot, 20% more durability,
            // +1 toughness and +0.1 knockback resistance.
            aerialHell("arsonist", 44, 6, 9, 10, 5, 13, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 4.0F, 0.2F);
            aerialHell("lunatic", 40, 5, 8, 9, 4, 12, 15, SoundEvents.ARMOR_EQUIP_GOLD, 2.5F, 0.1F);
            aerialHell("magmatic_gel", 8, 2, 4, 6, 3, 5, 25, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.1F);
            aerialHell("obsidian", 44, 4, 7, 9, 4, 12, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.2F);
            aerialHell("volucite", 42, 5, 8, 10, 5, 13, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 3.5F, 0.15F);
            aerialHell("shadow", 30, 5, 8, 8, 4, 12, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.1F);
            if (FabricLoader.getInstance().isModLoaded("weaponsexpanded")) {
                AerialHellHeavy.weapons();
                CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.COMBAT).register(output -> AerialHellHeavy.WEAPONS.forEach(output::accept));
                HeavyLoot.register();
            }
        }
        if (FabricLoader.getInstance().isModLoaded("enderscape")) {
            shadoline();
        }
        CreativeModeTabEvents.modifyOutputEvent(CompatWeapons.EXPANDED_WEAPONRY_TAB).register(output -> ITEMS.forEach(output::accept));
        // Heavy helmets wear their own equipment asset, heavy_<material>_helmet, whose geo
        // armor definition (assets/mms_weapons/geo_armor) draws the helmet with 3D horns.
        net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents.MODIFY.register(context ->
            context.modify(item -> hornAsset(item) != null, (builder, item) -> {
                net.minecraft.world.item.equipment.Equippable e =
                    item.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                if (e != null) {
                    builder.set(net.minecraft.core.component.DataComponents.EQUIPPABLE, new net.minecraft.world.item.equipment.Equippable(
                        e.slot(), e.equipSound(),
                        java.util.Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, id(hornAsset(item)))),
                        e.cameraOverlay(), e.allowedEntities(), e.dispensable(), e.swappable(),
                        e.damageOnHurt(), e.equipOnInteract(), e.canBeSheared(), e.shearingSound()));
                }
            }));
    }

    private static final java.util.Map<Item, String> HELMETS = new java.util.HashMap<>();

    // Expanded Weaponry's own heavy helmets get the same horns, from its textures.
    private static final java.util.Map<String, String> EXPANDED_WEAPONRY_HELMETS = java.util.Map.of(
        "heavy_copper_helmet", "copper",
        "heavy_iron_helmet", "iron",
        "heavy_golden_helmet", "gold",
        "heavy_diamond_helmet", "diamond",
        "heavy_netherite_helmet", "netherite");

    // The horned equipment asset a heavy helmet wears (assets/mms_weapons/equipment), or null.
    private static String hornAsset(Item item) {
        String ours = HELMETS.get(item);
        if (ours != null) return "heavy_" + ours + "_helmet";
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals("expanded_weaponry")) return null;
        String material = EXPANDED_WEAPONRY_HELMETS.get(id.getPath());
        return material == null ? null : "expanded_heavy_" + material + "_helmet";
    }

    private static void azurite() {
        set("azurite", new ArmorMaterial(
            11,
            ArmorMaterials.makeDefense(2, 4, 6, 3, 8),
            25,
            SoundEvents.ARMOR_EQUIP_GOLD,
            1.0F,
            0.1F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_azurite_material")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("heavy_azurite"))
        ));
    }

    // Defense values run boots, leggings, chestplate, helmet, body, like ArmorMaterials.makeDefense.
    private static void aerialHell(String material, int durability, int boots, int leggings, int chestplate, int helmet, int body,
                                   int enchantability, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float toughness, float knockback) {
        set(material, new ArmorMaterial(
            durability,
            ArmorMaterials.makeDefense(boots, leggings, chestplate, helmet, body),
            enchantability,
            sound,
            toughness,
            knockback,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_" + material + "_material")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("heavy_" + material))
        ));
    }

    private static void ruby() {
        set("ruby", new ArmorMaterial(
            18,
            ArmorMaterials.makeDefense(3, 6, 7, 3, 6),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            2.0F,
            0.1F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_ruby_material")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("heavy_ruby"))
        ));
    }

    private static void shadoline() {
        set("shadoline", new ArmorMaterial(
            30,
            ArmorMaterials.makeDefense(3, 6, 7, 3, 7),
            15,
            SoundEvents.ARMOR_EQUIP_IRON,
            2.0F,
            0.1F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enderscape", "repairs_shadoline_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("heavy_shadoline"))
        ));
    }

    private static final java.util.Set<String> AERIAL_HELL_ITEMS = java.util.Set.of("arsonist", "lunatic", "magmatic_gel", "obsidian", "volucite", "shadow");

    private static void set(String material, ArmorMaterial armor) {
        for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("heavy_" + material + "_" + type.getName()));
            Item.Properties properties = new Item.Properties()
                .setId(key)
                .humanoidArmor(armor, type)
                .attributes(attributes(armor, type));
            Item item = Registry.register(BuiltInRegistries.ITEM, key, AERIAL_HELL_ITEMS.contains(material)
                ? AerialHellHeavy.create(material, armor, type, key, (float) SLOWNESS)
                : new Item(properties));
            ITEMS.add(item);
            if (type == ArmorType.HELMET) {
                HELMETS.put(item, material);
            }
            CompatOrigin.record(item, CompatWeapons.source(material), "expanded_weaponry");
        }
    }

    // Armor, toughness and knockback resistance for the slot, plus the heavy armor slowdown.
    private static ItemAttributeModifiers attributes(ArmorMaterial armor, ArmorType type) {
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        String name = slot.getSerializedName();
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ARMOR,
                new AttributeModifier(id("heavy_armor_defense." + name), armor.defense().get(type), AttributeModifier.Operation.ADD_VALUE),
                slot
            )
            .add(
                Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(id("heavy_armor_toughness." + name), armor.toughness(), AttributeModifier.Operation.ADD_VALUE),
                slot
            )
            .add(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(id("heavy_armor_knockback_resistance." + name), armor.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE),
                slot
            )
            .add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(id("heavy_armor_slowness." + name), SLOWNESS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                slot
            )
            .build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mms_weapons", path);
    }
}
