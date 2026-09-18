package info.mudbourn.mmsweapons.throwable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    /** Fraction of full draw, 0 at {@link #THROW_THRESHOLD_TIME}, 1 at {@link #FULL_CHARGE_TIME}. */
    public static float chargeScale(int charge) {
        float span = FULL_CHARGE_TIME - THROW_THRESHOLD_TIME;
        float t = (charge - THROW_THRESHOLD_TIME) / span;
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}
