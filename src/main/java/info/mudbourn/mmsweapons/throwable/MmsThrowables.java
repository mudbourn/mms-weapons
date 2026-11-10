package info.mudbourn.mmsweapons.throwable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import info.mudbourn.mmsweapons.throwable.sickle.ThrownSickleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Registry and shared rules for weapons MMS makes throwable.
 *
 * <p>Neither Basic Weapons nor Expanded Weaponry ships a projectile, so the
 * throw is entirely ours: a tag decides which items can be thrown, an item
 * mixin gives those items trident-style use behaviour, and {@link
 * ThrownWeaponEntity} carries them through the air.
 *
 * <p>The membership tag is deliberately data-driven rather than an
 * {@code instanceof PikeItem} check. Basic Weapons generates its pike variants
 * from material packs — tin and bronze exist only because a pack added them —
 * so a class check would be right today and wrong the moment a pack lands. It
 * also means adding hammers later is a one-line data change with no rebuild.
 */
public final class MmsThrowables {

    private MmsThrowables() {
    }

    /**
     * Items that can be wound up and thrown. Populated with the Basic Weapons
     * pikes; see {@code data/mms_weapons/tags/item/throwable_weapons.json}.
     */
    public static final TagKey<Item> THROWABLE_WEAPONS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", "throwable_weapons"));

    /**
     * Items that perform the mace's falling smash attack. This is what makes
     * Density mean anything: the enchantment only feeds
     * {@code smash_damage_per_fallen_block}, which nothing reads unless the
     * weapon actually has a smash.
     */
    public static final TagKey<Item> MACE_SMASH_WEAPONS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", "mace_smash_weapons"));

    private static final Identifier THROWN_WEAPON_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "thrown_weapon");

    public static final ResourceKey<EntityType<?>> THROWN_WEAPON_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, THROWN_WEAPON_ID);

    /**
     * Sized and tracked exactly like {@code minecraft:trident}. The update
     * interval matters: a Loyalty return re-aims every tick server-side, and a
     * shorter interval just spends bandwidth re-sending a path the client
     * already extrapolates.
     */
    public static final EntityType<ThrownWeaponEntity> THROWN_WEAPON =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    THROWN_WEAPON_KEY,
                    EntityType.Builder.<ThrownWeaponEntity>of(ThrownWeaponEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .eyeHeight(0.13F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(THROWN_WEAPON_KEY));

    private static final ResourceKey<EntityType<?>> THROWN_SICKLE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("mms_weapons", "thrown_sickle"));

    /** The thrown sickle, for any sickle whose item id ends in "_sickle". */
    public static final EntityType<ThrownSickleEntity> THROWN_SICKLE =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    THROWN_SICKLE_KEY,
                    EntityType.Builder.<ThrownSickleEntity>of(ThrownSickleEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(THROWN_SICKLE_KEY));

    /** Minimum ticks a throw has to be wound up before it will release. */
    public static final int THROW_THRESHOLD_TIME = 10;

    /**
     * Wind-up, in ticks, at which a throw counts as fully drawn. Matches a bow's
     * time to full pull, so a maxed pike and a maxed bow feel like the same
     * commitment. Beyond this the charge scale is clamped to 1.
     */
    public static final int FULL_CHARGE_TIME = 20;

    /** Launch speed. A trident leaves the hand at 2.5; a pike is longer and heavier. */
    public static final float PROJECTILE_SHOOT_POWER = 2.3F;

    /** Launch speed of Hurl's mace, the heavy end hammer weights scale toward. */
    private static final float MACE_SHOOT_POWER = 0.95F;

    /** Extra wind-up, in ticks, a weight-1 weapon needs over a pike. */
    private static final int WEIGHT_THRESHOLD_TICKS = 6;

    /** Extra ticks to full charge a weight-1 weapon needs over a pike. */
    private static final int WEIGHT_CHARGE_TICKS = 10;

    /** Extra durability a weight-1 throw costs over a pike's one point. */
    private static final int WEIGHT_DURABILITY_COST = 2;

    /** Arrow gravity, what a pike falls at. */
    private static final double BASE_GRAVITY = 0.05;

    /** Extra gravity a weight-1 weapon falls with. */
    private static final double WEIGHT_GRAVITY = 0.03;

    /** Hammer weight by material, 0 a pike and 1 the mace: netherite coats diamond, gold sits near diamond, the rest lighten down to wood. */
    private static final Map<String, Float> HAMMER_WEIGHTS = Map.of(
            "netherite", 0.85F,
            "diamond", 0.8F,
            "golden", 0.75F,
            "iron", 0.6F,
            "copper", 0.5F,
            "stone", 0.4F,
            "wooden", 0.25F);

    /** Hammer weight for a material missing from the table, iron's. */
    private static final float DEFAULT_HAMMER_WEIGHT = 0.6F;

    /** Called from the mod initializer purely to force the class — and the registration — to load. */
    public static void register() {
    }

    public static boolean isThrowable(ItemStack stack) {
        return stack.is(THROWABLE_WEAPONS);
    }

    /**
     * Damage a thrown weapon deals on impact, taken from the weapon's own
     * main-hand attack damage.
     *
     * <p>Vanilla's trident throws for 8 and swings for 9 — the difference is
     * the player's own base attack damage of 1, which a projectile has no claim
     * to. Reading the item's modifiers instead of hard-coding a number keeps
     * every material tier in step, including any a material pack adds later.
     */
    public static float throwDamage(ItemStack stack) {
        float[] damage = {0.0F};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (Holder<Attribute> attribute, AttributeModifier modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage[0] += (float) modifier.amount();
            }
        });
        // A weapon with no attack damage at all still has to hurt for something.
        return Math.max(damage[0], 1.0F);
    }

    /**
     * Impact damage scaled by how long the throw was wound up.
     *
     * <p>The weak end is {@link #throwDamage(ItemStack)} — the weapon's own
     * attack-damage modifier, the same figure a barely-past-threshold throw dealt
     * before charge mattered. The strong end, at full draw, is {@code 1.25×} the
     * weapon's displayed melee damage, where "melee damage" is the modifier plus
     * the player's base attack of 1 that a swing carries. A committed throw is
     * meant to hit harder than a swing; a flicked one still hits for the old,
     * unimpressive number, so nothing regresses for a panic toss.
     *
     * @param chargeScale 0 at the throw threshold, 1 at {@link #FULL_CHARGE_TIME}.
     */
    public static float throwDamage(ItemStack stack, float chargeScale) {
        float base = throwDamage(stack);
        float fullDraw = (base + 1.0F) * 1.25F;
        float t = Math.max(0.0F, Math.min(1.0F, chargeScale));
        return base + (fullDraw - base) * t;
    }

    /** Hammers, the mace-smash weapons, throw worse than pikes by their material weight. */
    public static boolean isHeavy(ItemStack stack) {
        return stack.is(MACE_SMASH_WEAPONS);
    }

    /** How heavy a throw this weapon is, 0 for a pike up to 1 for Hurl's mace. */
    public static float weight(ItemStack stack) {
        if (!isHeavy(stack)) {
            return 0.0F;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int split = path.indexOf('_');
        String material = split < 0 ? path : path.substring(0, split);
        return HAMMER_WEIGHTS.getOrDefault(material, DEFAULT_HAMMER_WEIGHT);
    }

    /** Minimum wind-up, in ticks, before this weapon will release. */
    public static int throwThreshold(ItemStack stack) {
        return THROW_THRESHOLD_TIME + Math.round(weight(stack) * WEIGHT_THRESHOLD_TICKS);
    }

    /** Launch speed for this weapon. */
    public static float shootPower(ItemStack stack) {
        return Mth.lerp(weight(stack), PROJECTILE_SHOOT_POWER, MACE_SHOOT_POWER);
    }

    /** Durability one throw of this weapon costs. */
    public static int throwDurabilityCost(ItemStack stack) {
        return 1 + Math.round(weight(stack) * WEIGHT_DURABILITY_COST);
    }

    /** Gravity this weapon falls with in flight. */
    public static double gravity(ItemStack stack) {
        return BASE_GRAVITY + weight(stack) * WEIGHT_GRAVITY;
    }

    /** Throw sound pitch, lower for heavier weapons the way Hurl pitches its mace throw down. */
    public static float throwPitch(ItemStack stack) {
        return 1.0F - weight(stack) * 0.4F;
    }

    /** Fraction of full draw for this weapon, 0 at its threshold and 1 at full charge. */
    public static float chargeScale(ItemStack stack, int charge) {
        int threshold = throwThreshold(stack);
        int full = FULL_CHARGE_TIME + Math.round(weight(stack) * WEIGHT_CHARGE_TICKS);
        float t = (charge - threshold) / (float) (full - threshold);
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}
