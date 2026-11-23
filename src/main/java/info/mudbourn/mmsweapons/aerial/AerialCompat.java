package info.mudbourn.mmsweapons.aerial;

import fr.factionbedrock.aerialhell.Item.Ability.DamageUseSituationInfo;
import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import fr.factionbedrock.aerialhell.Util.EntityHelper;
import fr.factionbedrock.aerialhell.Util.EquippedItemStack;
import fr.factionbedrock.aerialhell.Util.FieldAccessor;
import fr.factionbedrock.aerialhell.Util.MutableFloat;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Makes compat gear in Aerial Hell materials act like the Aerial Hell original: rarity, fire resistance, armor extras and abilities.
public final class AerialCompat {

    private static final Map<Item, Optional<Item>> COUNTERPARTS = new ConcurrentHashMap<>();

    private AerialCompat() {
    }

    // The Aerial Hell item this compat item copies, or null when it is not an Aerial Hell material item.
    // Any Aerial Hell item: most are AerialHellItem, but shadow armor is ShadowArmorItem and
    // volucite armor is a plain Item, and those still carry the components to copy.
    public static Item counterpart(Item item) {
        return COUNTERPARTS.computeIfAbsent(item, AerialCompat::resolve).orElse(null);
    }

    // The counterpart when it has Aerial Hell abilities (use, damage, mining, ability tooltip).
    public static AerialHellItem abilities(Item item) {
        return counterpart(item) instanceof AerialHellItem aerial ? aerial : null;
    }

    // Whether the counterpart is the same kind of item (same armor slot, same tool), so its
    // own tick and tooltip apply as they are; a sword or chestplate stand-in only lends components.
    public static boolean sameKind(Item item) {
        Item original = counterpart(item);
        return original != null && kind(original, BuiltInRegistries.ITEM.getKey(original).getPath())
            .equals(kind(item, BuiltInRegistries.ITEM.getKey(item).getPath()));
    }

    private static Optional<Item> resolve(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (item instanceof AerialHellItem || id.getNamespace().equals("aerialhell") || !AerialGear.isAerial(item)) {
            return Optional.empty();
        }
        String material = AerialGear.material(id.getPath());
        if (material == null) {
            return Optional.empty();
        }
        String kind = kind(item, id.getPath());
        Item found = lookup(material + "_" + kind);
        if (found == null && !isArmor(kind)) {
            // Weapons without an Aerial Hell counterpart of their own: the material's sword,
            // else its chestplate (shadow has no sword), for rarity and fire resistance only.
            found = lookup(material + "_sword");
            if (found == null) {
                found = lookup(material + "_chestplate");
            }
        }
        return Optional.ofNullable(found);
    }

    private static String kind(Item item, String path) {
        Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            switch (equippable.slot()) {
                case HEAD -> {
                    return "helmet";
                }
                case CHEST -> {
                    return "chestplate";
                }
                case LEGS -> {
                    return "leggings";
                }
                case FEET -> {
                    return "boots";
                }
                default -> {
                }
            }
        }
        for (String tool : new String[] {"pickaxe", "axe", "shovel", "hoe"}) {
            if (path.endsWith("_" + tool)) {
                return tool;
            }
        }
        return "sword";
    }

    private static boolean isArmor(String kind) {
        return kind.equals("helmet") || kind.equals("chestplate") || kind.equals("leggings") || kind.equals("boots");
    }

    private static Item lookup(String name) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("aerialhell", name)).orElse(null);
    }

    static void registerComponents() {
        DefaultItemComponentEvents.MODIFY.register(context ->
            context.modify(item -> counterpart(item) != null, (builder, item) -> {
                Item original = counterpart(item);
                builder.set(DataComponents.RARITY, original.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON));
                DamageResistant resistant = original.components().get(DataComponents.DAMAGE_RESISTANT);
                if (resistant != null) {
                    builder.set(DataComponents.DAMAGE_RESISTANT, resistant);
                }
                String kind = kind(item, BuiltInRegistries.ITEM.getKey(item).getPath());
                ItemAttributeModifiers extras = original.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (isArmor(kind) && extras != null) {
                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS, withExtras(item.components().get(DataComponents.ATTRIBUTE_MODIFIERS), extras));
                }
            }));
    }

    // Adds the original armor's extra attributes (burning time, heat resistance...) to the piece's own.
    private static ItemAttributeModifiers withExtras(ItemAttributeModifiers own, ItemAttributeModifiers original) {
        ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();
        if (own != null) {
            for (ItemAttributeModifiers.Entry entry : own.modifiers()) {
                rebuilt.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        for (ItemAttributeModifiers.Entry entry : original.modifiers()) {
            boolean base = entry.attribute().equals(Attributes.ARMOR)
                || entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)
                || entry.attribute().equals(Attributes.KNOCKBACK_RESISTANCE);
            boolean owned = own != null && own.modifiers().stream().anyMatch(mine -> mine.attribute().equals(entry.attribute()));
            if (!base && !owned) {
                rebuilt.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        return rebuilt.build();
    }

    // Runs deal-damage and take-damage abilities of equipped compat items; returns the damage multiplier they set.
    public static float damageMultiplier(Entity attacker, LivingEntity victim, DamageSource source) {
        MutableFloat multiplier = new MutableFloat(1.0F);
        FieldAccessor<Float> accessor = new FieldAccessor<>(multiplier::get, multiplier::set);
        if (attacker instanceof LivingEntity dealer) {
            for (EquippedItemStack equipped : EntityHelper.getEquippedItemStackList(dealer)) {
                AerialHellItem original = abilities(equipped.stack().getItem());
                if (original != null) {
                    original.onDealDamage(equipped.stack(), dealer, equipped.slot(), new DamageUseSituationInfo(victim, source, accessor));
                }
            }
        }
        for (EquippedItemStack equipped : EntityHelper.getEquippedItemStackList(victim)) {
            AerialHellItem original = abilities(equipped.stack().getItem());
            if (original != null) {
                original.onTakeDamage(equipped.stack(), victim, equipped.slot(), new DamageUseSituationInfo(attacker, source, accessor));
            }
        }
        return multiplier.get();
    }
}
