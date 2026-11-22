package info.mudbourn.mmsweapons.material;

import fr.factionbedrock.aerialhell.Item.Ability.AbilitySelector;
import fr.factionbedrock.aerialhell.Item.Ability.ItemAbility;
import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import fr.factionbedrock.aerialhell.Item.Material.AerialHellArmorMaterial;
import fr.factionbedrock.aerialhell.Item.Material.AerialHellArmorMaterials;
import fr.factionbedrock.aerialhell.Item.Material.AttributeEntry;
import fr.factionbedrock.aerialhell.Item.Material.AttributeEntryList;
import fr.factionbedrock.aerialhell.Item.Material.ExtraAttributeModifiersMaterial;
import fr.factionbedrock.aerialhell.Registry.AerialHellItemAbilities;
import fr.factionbedrock.aerialhell.Registry.Misc.AerialHellRarities;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import fr.factionbedrock.aerialhell.Item.ExtraHoverTextItem;
import fr.factionbedrock.aerialhell.Item.Material.AerialHellToolMaterials;
import com.kielson.item.custom.GreatswordItem;
import net.angelic.weaponsexpanded.item.custom.BastardSwordItem;
import net.angelic.weaponsexpanded.item.custom.HalberdItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.lang.reflect.Field;
import java.util.function.Consumer;

// Heavy armor in Aerial Hell's own materials, built as Aerial Hell items: the same rarity colour, fire resistance, set
// ability (and its SHIFT tooltip) and extra attributes (burning time, heat resistance...) as the original piece, with the
// heavy tier's stats and slowdown on top. Only loaded when Aerial Hell is.
final class AerialHellHeavy {

    private AerialHellHeavy() {
    }

    static Item create(String material, ArmorMaterial armor, ArmorType type, ResourceKey<Item> key, float slowness) {
        AerialHellArmorMaterial original = original(material);
        AerialHellArmorMaterial heavy = new AerialHellArmorMaterial(armor.durability(), armor.defense(), armor.enchantmentValue(),
            armor.equipSound(), armor.toughness(), armor.knockbackResistance(), armor.repairIngredient(), armor.assetId());
        if (original != null) for (AttributeEntry entry : extras(original).get()) heavy.addAttributeModifier(entry.attribute(), entry.value(), entry.operation());
        AttributeEntryList slow = new AttributeEntryList().add(Attributes.MOVEMENT_SPEED, slowness, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        AerialHellItem.Properties properties = new AerialHellItem.Properties().setId(key).humanoidArmor(heavy, type, slow);
        Rarity rarity = rarity(material);
        if (rarity != null) properties.rarity(rarity);
        if (material.equals("arsonist")) properties.fireResistant();
        ItemAbility ability = ability(material);
        if (ability != null) properties.abilitySelector(AbilitySelector.of(ability));
        return new AerialHellItem(properties);
    }

    private static AerialHellArmorMaterial original(String material) {
        return switch (material) {
            case "arsonist" -> AerialHellArmorMaterials.ARSONIST;
            case "lunatic" -> AerialHellArmorMaterials.LUNATIC;
            case "magmatic_gel" -> AerialHellArmorMaterials.MAGMATIC_GEL;
            case "obsidian" -> AerialHellArmorMaterials.OBSIDIAN;
            case "volucite" -> AerialHellArmorMaterials.VOLUCITE;
            case "shadow" -> AerialHellArmorMaterials.SHADOW;
            default -> null;
        };
    }

    // Matches the rarity each original helmet registers with.
    private static Rarity rarity(String material) {
        return switch (material) {
            case "arsonist" -> AerialHellRarities.MYTHICAL;
            case "lunatic" -> AerialHellRarities.LEGENDARY;
            case "volucite" -> AerialHellRarities.VIBRANT;
            case "shadow" -> AerialHellRarities.CORRUPTED;
            default -> null;
        };
    }

    private static ItemAbility ability(String material) {
        return switch (material) {
            case "arsonist" -> AerialHellItemAbilities.ARSONIST_ARMOR;
            case "magmatic_gel" -> AerialHellItemAbilities.MAGMATIC_GEL_ARMOR;
            default -> null;
        };
    }

    private static AttributeEntryList extras(AerialHellArmorMaterial material) {
        try {
            Field field = ExtraAttributeModifiersMaterial.class.getDeclaredField("extraAttributes");
            field.setAccessible(true);
            return (AttributeEntryList) field.get(material);
        } catch (ReflectiveOperationException e) {
            return new AttributeEntryList();
        }
    }

    // Aerial Hell's Heavy Axe and Heavy Sword come as a set from mud dungeon treasure; the Heavy Battleaxe, Halberd,
    // Longsword and Greatsword join them in its heavy material: slow, hard-hitting, epic, repaired with obsidian shards.
    private static final ToolMaterial HEAVY = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 6.0F, 10,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_heavy_material")));

    static void weapons() {
        // The battleaxe hits 2 harder and swings 0.2 slower than the Heavy Axe, and slows its holder the same way.
        register("heavy_battleaxe", key -> new AerialHellItem(new AerialHellItem.Properties().setId(key)
            .axe(AerialHellToolMaterials.HEAVY, 8.0F, -3.7F, AttributeEntry.movementSpeed(-0.1F)).rarity(Rarity.EPIC)));
        // The halberd keeps Weapons Expanded's slash and pierce modes, at the heavy material's damage and a slower swing.
        register("heavy_halberd", key -> new HeavyHalberd(new Item.Properties().setId(key).rarity(Rarity.EPIC)));
        // The longsword keeps Weapons Expanded's one- and two-handed grips, a step slower than an obsidian one.
        register("heavy_longsword", key -> new HeavyLongsword(new Item.Properties().setId(key).rarity(Rarity.EPIC)));
        // The greatsword is Expanded Weaponry's, in the heavy material.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("expanded_weaponry")) {
            register("heavy_greatsword", key -> new HeavyGreatsword(new Item.Properties().setId(key).rarity(Rarity.EPIC)));
        }
    }

    private static void register(String name, java.util.function.Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
        CompatOrigin.record(item, "aerialhell", "weaponsexpanded");
        WEAPONS.add(item);
    }

    static final java.util.List<Item> WEAPONS = new java.util.ArrayList<>();

    // Shows the "Slow but powerful" line the Heavy Axe has, under Weapons Expanded's mode tooltip.
    private static final class HeavyHalberd extends HalberdItem implements ExtraHoverTextItem {
        HeavyHalberd(Item.Properties properties) {
            super(HEAVY, 6.5F, -3.5F, 6.0F, -3.6F, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            appendOptionalDescriptionsHoverText(context, tooltip);
            super.appendHoverText(stack, context, display, tooltip, flag);
        }

        @Override
        public Item getSelf() {
            return this;
        }
    }

    private static final class HeavyLongsword extends BastardSwordItem implements ExtraHoverTextItem {
        HeavyLongsword(Item.Properties properties) {
            super(HEAVY, 6.0F, -3.2F, 7.0F, -3.2F, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            appendOptionalDescriptionsHoverText(context, tooltip);
            super.appendHoverText(stack, context, display, tooltip, flag);
        }

        @Override
        public Item getSelf() {
            return this;
        }
    }

    private static final class HeavyGreatsword extends GreatswordItem implements ExtraHoverTextItem {
        HeavyGreatsword(Item.Properties properties) {
            super(HEAVY, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            appendOptionalDescriptionsHoverText(context, tooltip);
            super.appendHoverText(stack, context, display, tooltip, flag);
        }

        @Override
        public Item getSelf() {
            return this;
        }
    }
}
