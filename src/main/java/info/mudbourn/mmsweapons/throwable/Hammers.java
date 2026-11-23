package info.mudbourn.mmsweapons.throwable;

import info.mudbourn.mmsweapons.aerial.AerialGear;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Map;

/**
 * Hammers and the mace on one curve, driven by weight.
 *
 * <p>Weight runs from 0 (a pike) through 1 (the mace). It sets how slowly a hammer swings, how much of the mace's
 * falling smash it carries, and how badly it throws (see {@link MmsThrowables}). Each material's hit follows its tier.
 * The top Aerial Hell materials, and the heavy hammer, weigh more than the mace, so they smash harder than it
 * but swing slower, throw worse and slow their holder.
 *
 * <p>Aerial Hell hammers then get the rebalance's 1.5x on their hit, as all Aerial Hell weapons do. The smash share
 * is weight alone, so only hammers heavier than the mace out-smash it.
 */
public final class Hammers {

    /** weight: 0 pike to 1 mace. hit: the full swing (player base 1 included) before the Aerial Hell boost. */
    record Stat(float weight, float hit) {
    }

    /** The mace's own numbers: weight 1, a 9 hit (raised from 6), 0.6 swings a second. */
    public static final float MACE_HIT = 9.0F;

    private static final Map<String, Stat> STATS = Map.ofEntries(
        // Vanilla tiers
        Map.entry("wooden", new Stat(0.25F, 5.0F)),
        Map.entry("stone", new Stat(0.4F, 6.0F)),
        Map.entry("copper", new Stat(0.5F, 6.5F)),
        Map.entry("iron", new Stat(0.6F, 7.0F)),
        Map.entry("golden", new Stat(0.75F, 6.0F)),
        Map.entry("diamond", new Stat(0.8F, 8.0F)),
        Map.entry("netherite", new Stat(0.85F, 9.0F)),
        // Aerial Hell and companion materials, by their tool tier
        Map.entry("sky_wood", new Stat(0.25F, 5.0F)),
        Map.entry("magmatic_gel", new Stat(0.3F, 5.0F)),
        Map.entry("stellar_stone", new Stat(0.4F, 6.0F)),
        Map.entry("azurite", new Stat(0.45F, 6.0F)),
        Map.entry("ruby", new Stat(0.6F, 7.0F)),
        Map.entry("shadoline", new Stat(0.6F, 7.5F)),
        Map.entry("shadow", new Stat(0.85F, 8.0F)),
        Map.entry("obsidian", new Stat(0.9F, 8.5F)),
        // Past the mace: heavier than it, so harder smashes and steeper drawbacks
        Map.entry("volucite", new Stat(1.05F, 9.5F)),
        Map.entry("lunatic", new Stat(1.1F, 9.5F)),
        Map.entry("arsonist", new Stat(1.2F, 10.0F)),
        Map.entry("heavy", new Stat(1.3F, 11.7F)), // 1.3x the netherite hammer, as the Heavy Axe is to a netherite axe
        // Mutant Monsters' hulk hammer: mace weight, a step under the heavy hammer
        Map.entry("hulk", new Stat(1.0F, 9.0F))
    );

    private static final Stat DEFAULT = STATS.get("iron");

    /** Swings a second at weight 0 and the drop per point of weight: 1.0 down to the mace's 0.6. */
    private static final float LIGHT_SPEED = 1.0F;
    private static final float SPEED_PER_WEIGHT = 0.4F;

    /** Movement lost per point of weight past the mace, while held. */
    private static final float SLOW_PER_EXTRA_WEIGHT = 0.25F;

    /** Extra knockback on the hulk hammer's swings, about three Knockback enchantment levels' worth past one. */
    private static final float HULK_KNOCKBACK = 1.5F;
    /** The hulk hammer's durability, raised from Mutant Monsters' 150 to the heavy hammer's 1561. */
    private static final int HULK_DURABILITY = 1561;

    private static final Identifier SLOW_ID = Identifier.fromNamespaceAndPath("mms_weapons", "hammer_weight");

    private Hammers() {
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(item -> item == Items.MACE, (builder, item) ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes(MACE_HIT, 1.0F, false)));
            context.modify(Hammers::isHammer, (builder, item) -> {
                Stat stat = stat(item);
                float hit = AerialGear.isAerial(item) ? stat.hit() * AerialGear.STAT_MULTIPLIER : stat.hit();
                ItemAttributeModifiers attributes = attributes(hit, stat.weight(), isHeavyHammer(item));
                if (BuiltInRegistries.ITEM.getKey(item).equals(Identifier.fromNamespaceAndPath("mutantmonsters", "hulk_hammer"))) {
                    // The hulk hammer's swings knock back like a mutant zombie's fists.
                    attributes = attributes.withModifierAdded(Attributes.ATTACK_KNOCKBACK,
                        new AttributeModifier(SLOW_ID.withPath("hulk_knockback"), HULK_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
                }
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
            });
            // The hulk hammer throws on a chain; our item model hides it in hand while it's out (see Chainlink).
            context.modify(item -> BuiltInRegistries.ITEM.getKey(item).equals(Identifier.fromNamespaceAndPath("mutantmonsters", "hulk_hammer")),
                (builder, item) -> {
                    builder.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("mms_weapons", "hulk_hammer"));
                    // 150 uses is too few for a hammer that slams, smashes and throws; it lasts as long as the heavy hammer.
                    builder.set(DataComponents.MAX_DAMAGE, HULK_DURABILITY);
                });
        });
    }

    // By id rather than the smash tag: default components are set before tags load. Warhammers are swords, not these.
    public static boolean isHammer(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String ns = id.getNamespace();
        return id.getPath().endsWith("_hammer") && !id.getPath().endsWith("_warhammer")
            && (ns.equals("expanded_weaponry") || ns.equals("mms_weapons") || ns.equals("mutantmonsters"));
    }

    /** Weight of a hammer, or 0 for anything that isn't one. */
    public static float weight(Item item) {
        return isHammer(item) ? stat(item).weight() : 0.0F;
    }

    /** Share of the mace's falling smash bonus this weapon deals: its weight. */
    public static float smashScale(Item item) {
        return isHammer(item) ? stat(item).weight() : 1.0F;
    }

    /**
     * Fall, in blocks, the slam skill makes this weapon's next swing smash as, or 0 if it has no slam. The heavy
     * hammer slams as a 5-block fall (16 before its weight), the hulk hammer as a 3-block one (12).
     */
    public static float slamFall(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id.equals(Identifier.fromNamespaceAndPath("mms_weapons", "heavy_hammer"))) {
            return 5.0F;
        }
        if (id.equals(Identifier.fromNamespaceAndPath("mutantmonsters", "hulk_hammer"))) {
            return 3.0F;
        }
        return 0.0F;
    }

    private static Stat stat(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        String material = path.endsWith("_hammer") ? path.substring(0, path.length() - "_hammer".length()) : path;
        return STATS.getOrDefault(material, DEFAULT);
    }

    private static boolean isHeavyHammer(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).equals(Identifier.fromNamespaceAndPath("mms_weapons", "heavy_hammer"));
    }

    // The heavy hammer takes the Heavy Axe's flat -10% (total) like the other heavy weapons, in place of the weight slowdown.
    private static ItemAttributeModifiers attributes(float hit, float weight, boolean heavy) {
        float speed = LIGHT_SPEED - SPEED_PER_WEIGHT * weight;
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, hit - 1.0F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - 4.0F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (heavy) {
            builder.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(SLOW_ID, -0.1F,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        } else if (weight > 1.0F) {
            builder.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(SLOW_ID, -(weight - 1.0F) * SLOW_PER_EXTRA_WEIGHT,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }
}
