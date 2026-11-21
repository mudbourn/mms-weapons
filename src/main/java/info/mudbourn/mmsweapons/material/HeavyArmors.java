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
        }
        if (FabricLoader.getInstance().isModLoaded("enderscape")) {
            shadoline();
        }
        CreativeModeTabEvents.modifyOutputEvent(CompatWeapons.EXPANDED_WEAPONRY_TAB).register(output -> ITEMS.forEach(output::accept));
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

    private static void set(String material, ArmorMaterial armor) {
        for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("heavy_" + material + "_" + type.getName()));
            Item.Properties properties = new Item.Properties()
                .setId(key)
                .humanoidArmor(armor, type)
                .attributes(attributes(armor, type));
            ITEMS.add(Registry.register(BuiltInRegistries.ITEM, key, new Item(properties)));
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
