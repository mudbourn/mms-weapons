package info.mudbourn.mmsweapons.material;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.container.SpellContainers;
import net.spell_engine.rpg_series.item.Shield;

import java.util.List;
import java.util.Map;

// Holy wands, holy staves and kite shields in the pack materials, built the way Paladins builds its own.
final class PaladinsWeapons {

    private static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("paladins", "generic"));
    private static final Identifier HEALING_POWER = Identifier.fromNamespaceAndPath("spell_power", "healing");
    private static final Identifier HEAL = Identifier.fromNamespaceAndPath("paladins", "heal");
    private static final Identifier HOLY_SHOCK = Identifier.fromNamespaceAndPath("paladins", "holy_shock");
    private static final Identifier BONUS_ID = Identifier.fromNamespaceAndPath("mms_weapons", "equipment_bonus");
    private static final Identifier TOUGHNESS_ID = Identifier.fromNamespaceAndPath("mms_weapons", "kite_shield_toughness");
    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath("mms_weapons", "kite_shield_health");

    // The Paladins tier each pack material is matched to: its wand and staff healing power, and its kite shield toughness and health.
    private record Tier(float wandPower, float staffPower, float toughness, float health) {
    }

    private static final Map<String, Tier> TIERS = Map.ofEntries(
        Map.entry("azurite", new Tier(3.0F, 4.5F, 0.0F, 0.0F)),
        Map.entry("ruby", new Tier(4.0F, 5.0F, 1.0F, 2.0F)),
        Map.entry("shadoline", new Tier(5.0F, 6.0F, 1.0F, 2.0F)),
        Map.entry("magmatic_gel", new Tier(3.0F, 4.5F, 0.0F, 0.0F)),
        Map.entry("sky_wood", new Tier(3.0F, 4.5F, 0.0F, 0.0F)),
        Map.entry("stellar_stone", new Tier(3.0F, 4.5F, 0.0F, 0.0F)),
        Map.entry("obsidian", new Tier(4.0F, 5.0F, 1.0F, 2.0F)),
        Map.entry("arsonist", new Tier(5.0F, 6.0F, 1.0F, 2.0F)),
        Map.entry("lunatic", new Tier(5.0F, 6.0F, 1.0F, 2.0F)),
        Map.entry("volucite", new Tier(5.0F, 6.0F, 1.0F, 2.0F)),
        Map.entry("shadow", new Tier(4.0F, 5.0F, 1.0F, 2.0F))
    );

    private PaladinsWeapons() {
    }

    static void register(String name, ToolMaterial material) {
        Tier tier = TIERS.get(name);
        CompatWeapons.add(TAB, name + "_holy_wand", p -> staff(material, p, 2.0, -2.4, tier.wandPower(), HEAL));
        CompatWeapons.add(TAB, name + "_holy_staff", p -> staff(material, p, 4.0, -3.0, tier.staffPower(), HOLY_SHOCK));
        CompatWeapons.add(TAB, name + "_kite_shield", p -> kiteShield(material, p, tier));
    }

    private static Item staff(ToolMaterial material, Item.Properties p, double damage, double speed, float power, Identifier spell) {
        p.durability(material.durability()).enchantable(material.enchantmentValue()).repairable(material.repairItems());
        p.delayedComponent(DataComponents.ATTRIBUTE_MODIFIERS, context -> ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(BuiltInRegistries.ATTRIBUTE.get(HEALING_POWER).orElseThrow(), new AttributeModifier(BONUS_ID, power, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build());
        p.component(SpellDataComponents.SPELL_CONTAINER, SpellContainers.forMagicWeapon().withSpellId(spell));
        return new StaffItem(material, p);
    }

    private static Item kiteShield(ToolMaterial material, Item.Properties p, Tier tier) {
        List<Shield.AttributeEntry> attributes = tier.toughness() > 0.0F
            ? List.of(
                new Shield.AttributeEntry(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_ID, tier.toughness(), AttributeModifier.Operation.ADD_VALUE)),
                new Shield.AttributeEntry(Attributes.MAX_HEALTH, new AttributeModifier(HEALTH_ID, tier.health(), AttributeModifier.Operation.ADD_VALUE))
            )
            : List.of();
        p.durability(material.durability()).repairable(material.repairItems());
        return new Item(Shield.applyVanillaComponents(p, null, attributes));
    }
}
