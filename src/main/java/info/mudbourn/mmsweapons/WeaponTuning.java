package info.mudbourn.mmsweapons;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Map;

/**
 * Per-item combat tuning that can't be expressed as data.
 *
 * <p>Better Combat's {@code weapon_attributes} files cover reach, hitboxes,
 * animations and sounds, but attack speed and knockback are vanilla item
 * attributes and blocking is a vanilla component — none of which a datapack
 * can change on another mod's item. Fabric's {@code DefaultItemComponentEvents}
 * rewrites the item's default components at load instead, which keeps this to
 * plain API calls with no mixins.
 *
 * <p>Five changes, all requested for MMS:
 * <ul>
 *   <li>Anchor swings on a 2.35-second cooldown, large tuna on 1.8.</li>
 *   <li>Large tuna gets a small knockback bonus over a plain weapon.</li>
 *   <li>Basic Weapons glaives gain shield blocking.</li>
 *   <li>The endersoul hand swings faster than an Expanded Weaponry dagger.</li>
 *   <li>The longbow hits for 12 instead of 8.5.</li>
 * </ul>
 *
 * <p>Attack speed is expressed as attacks per second off a player base of
 * {@value #BASE_ATTACK_SPEED}, so the {@code ADD_VALUE} deltas below read as
 * "this much slower than bare-handed" — {@code -3.45} leaves 0.55 swings a
 * second. Existing attack-speed entries are dropped rather than stacked with,
 * so the result is exact regardless of what the mod shipped; every other
 * modifier the item had (damage in particular) is copied through untouched.
 */
public final class WeaponTuning {

    /** Player base attack speed, in attacks per second. */
    private static final double BASE_ATTACK_SPEED = 4.0;

    /**
     * Swing speed for the large tuna, as an {@code ADD_VALUE} delta off the
     * player's base — 0.55 attacks/sec, or a 1.8-second cooldown.
     */
    private static final double HEAVY_SWING_ATTACK_SPEED = -3.45;

    /**
     * Swing speed for the anchor — 0.4255 attacks/sec, or a 2.35-second
     * cooldown, matching MMS Arsenal's Dragon Slayer and Crucible.
     */
    private static final double ANCHOR_ATTACK_SPEED = -3.5745;

    /** Knockback bonus on the large tuna. Vanilla weapons sit at 0. */
    private static final double TUNA_KNOCKBACK_BONUS = 0.5;

    /** Player base attack damage, bare-handed. */
    private static final double BASE_ATTACK_DAMAGE = 1.0;

    private static final Identifier ANCHOR = Identifier.fromNamespaceAndPath("bountiful-fish", "anchor");
    private static final Identifier LARGE_TUNA = Identifier.fromNamespaceAndPath("bountiful-fish", "large_tuna");

    /**
     * Tooltip attack damage, replacing whatever the mod shipped. Better
     * Combat's per-attack multipliers apply on top, and those are tuned in the
     * matching {@code weapon_attributes} file to land each weapon in its
     * intended band — 14-17 for the anchor, 9-10 for the tuna.
     */
    private static final Map<Identifier, Double> ATTACK_DAMAGE = Map.of(
            ANCHOR, 16.0,
            LARGE_TUNA, 10.0);

    /**
     * The endersoul hand's swing speed, as an {@code ADD_VALUE} delta off the
     * player's base. Mutant Monsters ships {@code -2.4} (1.6 attacks/sec); MMS
     * wants it a step quicker than an Expanded Weaponry dagger, which sits at
     * {@code -1.8} (2.2/sec). Damage is left at the mod's 6.
     */
    private static final double ENDERSOUL_HAND_ATTACK_SPEED = -1.5;

    /**
     * Longbow full-draw damage, replacing Kielson's 8.5.
     *
     * <p>{@code CustomBow} divides the shooter's {@code RANGED_DAMAGE} by the
     * bow's projectile velocity and hands the result to
     * {@code AbstractArrow#setBaseDamage}, which vanilla then multiplies back
     * out by the arrow's speed. The two cancel, so this number is the damage a
     * full-draw hit actually deals and is independent of the velocity bump in
     * {@code LongbowVelocityMixin}.
     */
    private static final double LONGBOW_RANGED_DAMAGE = 12.0;

    private static final Identifier ENDERSOUL_HAND =
            Identifier.fromNamespaceAndPath("mutantmonsters", "endersoul_hand");
    private static final Identifier LONGBOW =
            Identifier.fromNamespaceAndPath("expanded_weaponry", "longbow");

    /**
     * Kielson's API attribute the longbow's damage rides on. Referenced by id
     * rather than by class so this stays loadable without Expanded Weaponry;
     * with the mod absent the item never resolves and none of it runs.
     */
    private static final Identifier RANGED_DAMAGE_ATTRIBUTE =
            Identifier.fromNamespaceAndPath("kielsonsapi", "ranged_damage");

    private static final Identifier SWING_COOLDOWN_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "heavy_swing_cooldown");
    private static final Identifier KNOCKBACK_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "tuna_knockback");
    private static final Identifier DAMAGE_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "weapon_damage");

    private WeaponTuning() {}

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(WeaponTuning::isHeavySwinger, (builder, item) -> {
                ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();

                Double damage = ATTACK_DAMAGE.get(BuiltInRegistries.ITEM.getKey(item));

                // Attack speed and damage are replaced outright; everything
                // else the mod shipped is kept.
                for (ItemAttributeModifiers.Entry entry : currentModifiers(item).modifiers()) {
                    boolean replaced = entry.attribute().equals(Attributes.ATTACK_SPEED)
                            || (damage != null && entry.attribute().equals(Attributes.ATTACK_DAMAGE));
                    if (!replaced) {
                        rebuilt.add(entry.attribute(), entry.modifier(), entry.slot());
                    }
                }

                if (damage != null) {
                    rebuilt.add(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(DAMAGE_ID, damage - BASE_ATTACK_DAMAGE,
                                    AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND);
                }

                rebuilt.add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(SWING_COOLDOWN_ID,
                                is(item, ANCHOR) ? ANCHOR_ATTACK_SPEED : HEAVY_SWING_ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);

                if (is(item, LARGE_TUNA)) {
                    rebuilt.add(Attributes.ATTACK_KNOCKBACK,
                            new AttributeModifier(KNOCKBACK_ID, TUNA_KNOCKBACK_BONUS,
                                    AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND);
                }

                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, rebuilt.build());
            });

            // The endersoul hand keeps its 6 damage and only loses the mod's
            // sluggish swing. Better Combat handles its reach separately, in
            // data/mutantmonsters/weapon_attributes/endersoul_hand.json.
            context.modify(item -> is(item, ENDERSOUL_HAND), (builder, item) ->
                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                            replacing(item, Attributes.ATTACK_SPEED,
                                    new AttributeModifier(SWING_COOLDOWN_ID, ENDERSOUL_HAND_ATTACK_SPEED,
                                            AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)));

            // Longbow damage. The attribute is looked up rather than imported
            // so this class stays loadable with Expanded Weaponry absent.
            BuiltInRegistries.ATTRIBUTE.get(RANGED_DAMAGE_ATTRIBUTE).ifPresent(rangedDamage ->
                    context.modify(item -> is(item, LONGBOW), (builder, item) ->
                            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                                    replacing(item, rangedDamage,
                                            new AttributeModifier(DAMAGE_ID, LONGBOW_RANGED_DAMAGE,
                                                    AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.HAND))));

            // Glaives and the large tuna block. Lifted from the vanilla shield
            // rather than hand-rolled so the delay, damage reduction and sounds
            // match what players already expect from blocking.
            //
            // The tuna shipped a blocking *model* (large_tuna_blocking.json in the
            // bountifulfish-tuna-3d pack) and Better Combat plays the blocking pose
            // for it, but without this component nothing intercepts the damage —
            // it was purely cosmetic until 0.9.56.
            BlocksAttacks shieldBlocking = Items.SHIELD.components().get(DataComponents.BLOCKS_ATTACKS);
            if (shieldBlocking != null) {
                context.modify(WeaponTuning::blocksAttacks,
                        (builder, item) -> builder.set(DataComponents.BLOCKS_ATTACKS, shieldBlocking));
            }
        });
    }

    /**
     * The item's modifiers with every entry for {@code attribute} dropped and
     * {@code replacement} put in their place, so the result is exact no matter
     * what the mod shipped. Everything else is copied through untouched.
     */
    private static ItemAttributeModifiers replacing(Item item,
                                                    Holder<Attribute> attribute,
                                                    AttributeModifier replacement,
                                                    EquipmentSlotGroup slot) {
        ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : currentModifiers(item).modifiers()) {
            if (!entry.attribute().equals(attribute)) {
                rebuilt.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        rebuilt.add(attribute, replacement, slot);
        return rebuilt.build();
    }

    private static ItemAttributeModifiers currentModifiers(Item item) {
        ItemAttributeModifiers current = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        return current == null ? ItemAttributeModifiers.EMPTY : current;
    }

    /** Items given vanilla shield blocking: every glaive, plus the large tuna. */
    private static boolean blocksAttacks(Item item) {
        return isGlaive(item) || is(item, LARGE_TUNA);
    }

    private static boolean isHeavySwinger(Item item) {
        return is(item, ANCHOR) || is(item, LARGE_TUNA);
    }

    /**
     * Basic Weapons registers a glaive per material, so match on the id suffix
     * rather than naming all nine.
     */
    private static boolean isGlaive(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return "basicweapons".equals(id.getNamespace()) && id.getPath().endsWith("_glaive");
    }

    private static boolean is(Item item, Identifier id) {
        return id.equals(BuiltInRegistries.ITEM.getKey(item));
    }
}
