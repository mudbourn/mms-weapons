package info.mudbourn.mmsweapons.material;

import fr.factionbedrock.aerialhell.Registry.Misc.AerialHellRarities;
import info.mudbourn.mmsweapons.aerial.AerialGear;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.container.SpellContainers;
import net.spell_engine.rpg_series.item.Armor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arsonist class gear: every RPG-series class's netherite set in arsonist, Aerial Hell's best tier, at the rebalance's
 * 1.5x. Wizards' arcane, fire and frost robes with their wands and staves; Paladins' crusader armor and prior robes
 * (its holy wand, holy staff and kite shield come from {@link PaladinsWeapons}). Armor, toughness and every spell
 * bonus are 1.5x the netherite piece's; wands and staves carry 1.5x the netherite one's spell power, and AerialGear
 * gives their hit and durability the same 1.5x.
 */
public final class WizardsGear {

    private static final TagKey<Item> REPAIRS_ARSONIST = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("aerialhell", "repairs_arsonist_material"));
    private static final Identifier BONUS_ID = Identifier.fromNamespaceAndPath("mms_weapons", "equipment_bonus");
    private static final float BOOST = AerialGear.STAT_MULTIPLIER;
    private static final List<ArmorType> TYPES = List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS);

    /** One per-piece bonus of a netherite set, before the boost. */
    private record Bonus(String attribute, float value, AttributeModifier.Operation op) {
    }

    /**
     * A netherite class set: the mod it needs, our item name, the 3D model it wears, its head/chest/legs/feet armor,
     * per-piece bonuses, durability multiplier and enchantability.
     */
    private record Set(String mod, String name, String geo, int[] armor, List<Bonus> bonuses, int durability, int enchantability) {
    }

    private static Bonus mult(String attribute, float value) {
        return new Bonus(attribute, value, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static Bonus add(String attribute, float value) {
        return new Bonus(attribute, value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static final int[] ROBE = {1, 3, 2, 1};
    private static final List<Set> SETS = List.of(
        new Set("wizards", "arcane_robe", "wizards:geo/wizard_robes.geo.json", ROBE,
            List.of(mult("spell_power:arcane", 0.3F), mult("spell_power:haste", 0.03F)), 30, 15),
        new Set("wizards", "fire_robe", "wizards:geo/wizard_robes.geo.json", ROBE,
            List.of(mult("spell_power:fire", 0.3F), mult("spell_power:critical_chance", 0.03F)), 30, 15),
        new Set("wizards", "frost_robe", "wizards:geo/wizard_robes.geo.json", ROBE,
            List.of(mult("spell_power:frost", 0.3F), mult("spell_power:critical_damage", 0.06F)), 30, 15),
        new Set("paladins", "crusader_armor", "paladins:geo/paladin_armor.geo.json", new int[]{3, 8, 6, 3},
            List.of(add("minecraft:armor_toughness", 1.0F), add("spell_power:healing", 1.0F)), 37, 15),
        new Set("paladins", "prior_robe", "paladins:geo/priest_robes.geo.json", ROBE,
            List.of(mult("spell_power:healing", 0.3F), mult("spell_power:haste", 0.04F)), 30, 15)
    );

    /** Wizards wands and staves: school, wand spell, staff spell. Netherite wands give 5.5 power, staves 7. */
    private static final String[][] CASTERS = {
        {"arcane", "arcane_bolt", "arcane_blast"},
        {"fire", "fireball", "fire_blast"},
        {"frost", "frost_shard", "frostbolt"},
    };

    /** Robe pieces by the 3D model and texture they wear, for the client renderer. */
    public record Worn(Identifier geo, Identifier texture, List<Item> pieces) {
    }

    public static final List<Worn> WORN = new ArrayList<>();

    private WizardsGear() {
    }

    static void register(ToolMaterial arsonist) {
        FabricLoader loader = FabricLoader.getInstance();
        for (Set set : SETS) {
            if (loader.isModLoaded(set.mod())) {
                armorSet(set);
            }
        }
        if (loader.isModLoaded("wizards")) {
            ResourceKey<CreativeModeTab> tab = tab("wizards");
            for (String[] caster : CASTERS) {
                String power = "spell_power:" + caster[0];
                CompatWeapons.add(tab, "arsonist_" + caster[0] + "_wand", p -> staff(arsonist, p, 2.0, -2.4, power, 5.5F * BOOST, "wizards:" + caster[1]));
                CompatWeapons.add(tab, "arsonist_" + caster[0] + "_staff", p -> staff(arsonist, p, 4.0, -3.0, power, 7.0F * BOOST, "wizards:" + caster[2]));
            }
        }
        // Armor attributes go in once spell_power's attributes are surely registered.
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(SET_OF::containsKey, (builder, item) -> {
            ItemAttributeModifiers attributes = armorAttributes(item);
            ((Armor.CustomItem) item).setAttributes(attributes);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
        }));
    }

    private static final Map<Item, Set> SET_OF = new LinkedHashMap<>();

    private static void armorSet(Set set) {
        String name = "arsonist_" + set.name();
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("mms_weapons", name));
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        for (int i = 0; i < 4; i++) {
            defense.put(TYPES.get(i), Math.round(set.armor()[i] * BOOST));
        }
        ArmorMaterial material = new ArmorMaterial(Math.round(set.durability() * BOOST), defense, Math.round(set.enchantability() * BOOST),
            SoundEvents.ARMOR_EQUIP_NETHERITE, 0.0F, 0.0F, REPAIRS_ARSONIST, asset);
        List<Item> pieces = new ArrayList<>();
        for (ArmorType type : TYPES) {
            CompatWeapons.add(tab(set.mod()), name + "_" + slotName(type), p -> {
                Item piece = new Armor.CustomItem(material, type, p.durability(type.getDurability(material.durability()))
                    .rarity(AerialHellRarities.MYTHICAL).fireResistant());
                pieces.add(piece);
                SET_OF.put(piece, set);
                return piece;
            });
        }
        WORN.add(new Worn(Identifier.parse(set.geo()), Identifier.fromNamespaceAndPath("mms_weapons", "textures/armor/" + name + ".png"), pieces));
    }

    // The full boosted armor (1.5, not rounded), the boosted bonuses, on the piece's slot.
    private static ItemAttributeModifiers armorAttributes(Item item) {
        Armor.CustomItem piece = (Armor.CustomItem) item;
        Set set = SET_OF.get(item);
        ArmorType type = piece.getType();
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        Identifier id = Identifier.withDefaultNamespace("armor." + type.getName());
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(id, set.armor()[TYPES.indexOf(type)] * BOOST, AttributeModifier.Operation.ADD_VALUE), slot);
        for (Bonus bonus : set.bonuses()) {
            builder.add(attribute(bonus.attribute()), new AttributeModifier(id, bonus.value() * BOOST, bonus.op()), slot);
        }
        return builder.build();
    }

    private static String slotName(ArmorType type) {
        return switch (type) {
            case HELMET -> "head";
            case CHESTPLATE -> "chest";
            case LEGGINGS -> "legs";
            default -> "feet";
        };
    }

    private static ResourceKey<CreativeModeTab> tab(String mod) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(mod, "generic"));
    }

    private static Holder<Attribute> attribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(id)).orElseThrow();
    }

    private static Item staff(ToolMaterial material, Item.Properties p, double damage, double speed, String power, float amount, String spell) {
        p.durability(material.durability()).enchantable(material.enchantmentValue()).repairable(material.repairItems())
            .rarity(AerialHellRarities.MYTHICAL).fireResistant();
        p.delayedComponent(DataComponents.ATTRIBUTE_MODIFIERS, context -> ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(attribute(power), new AttributeModifier(BONUS_ID, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build());
        p.component(SpellDataComponents.SPELL_CONTAINER, SpellContainers.forMagicWeapon().withSpellId(Identifier.parse(spell)));
        return new StaffItem(material, p);
    }
}
