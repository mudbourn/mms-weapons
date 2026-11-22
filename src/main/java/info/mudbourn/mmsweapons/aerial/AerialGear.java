package info.mudbourn.mmsweapons.aerial;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Aerial Hell gear is worth more than overworld gear, and works best at home.
 *
 * <ul>
 *   <li>Aerial Hell tools and weapons get 1.5x damage, mining speed, durability and enchantability.</li>
 *   <li>Against things from outside Aerial Hell they're 1.25x as effective: they mine overworld blocks
 *       1.25x faster, hit non-Aerial Hell mobs 1.25x harder, and their armor counts 1.25x against those mobs.</li>
 *   <li>Overworld gear is 0.75x as effective against Aerial Hell: slower on its blocks, weaker on its mobs,
 *       and its armor counts 0.75x against them.</li>
 * </ul>
 *
 * "Aerial Hell" means anything in the aerialhell namespace, plus the mms_weapons items made from Aerial Hell
 * materials. Everything else, other mods included, counts as overworld.
 */
public final class AerialGear {

    public static final float STAT_MULTIPLIER = 1.5F;
    public static final float HOME_ADVANTAGE = 1.25F;
    public static final float AWAY_PENALTY = 0.75F;

    private static final List<String> MATERIALS = List.of(
        "arsonist_", "azurite_", "lunatic_", "magmatic_gel_", "obsidian_", "ruby_", "shadow_", "sky_wood_", "stellar_stone_", "volucite_");
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private AerialGear() {
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context ->
            context.modify(item -> isAerial(item) && isToolOrWeapon(item), (builder, item) -> {
                Integer durability = item.components().get(DataComponents.MAX_DAMAGE);
                if (durability != null) {
                    builder.set(DataComponents.MAX_DAMAGE, Math.round(durability * STAT_MULTIPLIER));
                }
                Enchantable enchantable = item.components().get(DataComponents.ENCHANTABLE);
                if (enchantable != null) {
                    builder.set(DataComponents.ENCHANTABLE, new Enchantable(Math.round(enchantable.value() * STAT_MULTIPLIER)));
                }
                Tool tool = item.components().get(DataComponents.TOOL);
                if (tool != null) {
                    List<Tool.Rule> rules = tool.rules().stream()
                        .map(rule -> new Tool.Rule(rule.blocks(), rule.speed().map(speed -> speed * STAT_MULTIPLIER), rule.correctForDrops()))
                        .toList();
                    builder.set(DataComponents.TOOL, new Tool(rules, tool.defaultMiningSpeed() * STAT_MULTIPLIER, tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
                }
                ItemAttributeModifiers modifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (modifiers != null) {
                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS, scaledDamage(modifiers));
                }
            }));
    }

    // The item's full hit (player base 1 plus its bonus) times 1.5, so a 9.5 sword becomes 14.25.
    private static ItemAttributeModifiers scaledDamage(ItemAttributeModifiers modifiers) {
        ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID)
                && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                modifier = new AttributeModifier(modifier.id(), (1.0 + modifier.amount()) * STAT_MULTIPLIER - 1.0, modifier.operation());
            }
            rebuilt.add(entry.attribute(), modifier, entry.slot());
        }
        return rebuilt.build();
    }

    private static boolean isToolOrWeapon(Item item) {
        if (item.components().has(DataComponents.TOOL) || item.components().has(DataComponents.BLOCKS_ATTACKS)) {
            return true;
        }
        ItemAttributeModifiers modifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        return modifiers != null && modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE));
    }

    public static boolean isAerial(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getNamespace().equals("aerialhell")) {
            return true;
        }
        if (!id.getNamespace().equals("mms_weapons")) {
            return false;
        }
        String path = id.getPath().startsWith("heavy_") ? id.getPath().substring("heavy_".length()) : id.getPath();
        return MATERIALS.stream().anyMatch(path::startsWith);
    }

    public static boolean isAerial(ItemStack stack) {
        return !stack.isEmpty() && isAerial(stack.getItem());
    }

    public static boolean isAerial(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("aerialhell");
    }

    public static boolean isAerial(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("aerialhell");
    }

    // Mining: Aerial Hell tools on overworld blocks, or overworld tools on Aerial Hell blocks. Bare hands are left alone.
    public static float miningMultiplier(ItemStack held, BlockState state) {
        if (held.isEmpty() || !held.has(DataComponents.TOOL)) {
            return 1.0F;
        }
        boolean aerialTool = isAerial(held);
        boolean aerialBlock = isAerial(state);
        if (aerialTool && !aerialBlock) {
            return HOME_ADVANTAGE;
        }
        if (!aerialTool && aerialBlock) {
            return AWAY_PENALTY;
        }
        return 1.0F;
    }

    // Weapon damage against mobs. Players on either side are left alone, so PvP isn't skewed.
    public static float weaponMultiplier(ItemStack weapon, LivingEntity victim) {
        if (weapon == null || weapon.isEmpty() || victim instanceof Player) {
            return 1.0F;
        }
        boolean aerialWeapon = isAerial(weapon);
        boolean aerialVictim = isAerial(victim);
        if (aerialWeapon && !aerialVictim) {
            return HOME_ADVANTAGE;
        }
        if (!aerialWeapon && aerialVictim) {
            return AWAY_PENALTY;
        }
        return 1.0F;
    }

    /**
     * How much to add to the wearer's armor (or toughness) against this attacker: +25% of what Aerial Hell
     * pieces give against non-Aerial Hell mobs, -25% of what overworld pieces give against Aerial Hell mobs.
     * Attacks from players or with no attacker are left alone.
     */
    public static double armorAdjustment(LivingEntity wearer, Entity attacker, Holder<Attribute> attribute) {
        if (attacker == null || attacker instanceof Player) {
            return 0.0;
        }
        boolean aerialAttacker = isAerial(attacker);
        double adjustment = 0.0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = wearer.getItemBySlot(slot);
            if (piece.isEmpty()) {
                continue;
            }
            boolean aerialPiece = isAerial(piece);
            if (aerialPiece == aerialAttacker) {
                continue;
            }
            double value = armorValue(piece, attribute);
            adjustment += aerialPiece ? value * (HOME_ADVANTAGE - 1.0F) : value * (AWAY_PENALTY - 1.0F);
        }
        return adjustment;
    }

    private static double armorValue(ItemStack piece, Holder<Attribute> attribute) {
        ItemAttributeModifiers modifiers = piece.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }
        return modifiers.modifiers().stream()
            .filter(entry -> entry.attribute().equals(attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
            .mapToDouble(entry -> entry.modifier().amount())
            .sum();
    }
}
