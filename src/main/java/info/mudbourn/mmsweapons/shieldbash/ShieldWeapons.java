package info.mudbourn.mmsweapons.shieldbash;

import info.mudbourn.mmsweapons.WeaponTuning;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.Set;

// Gives every shield a main-hand sword's attack damage, attack speed and double its knockback.
public final class ShieldWeapons {

    private static final double BASE_ATTACK_DAMAGE = 1.0;
    private static final double SWORD_ATTACK_SPEED = -2.4;
    private static final double KNOCKBACK_BONUS = 0.8;

    private static final Set<Holder<Attribute>> REPLACED = Set.of(
        Attributes.ATTACK_DAMAGE,
        Attributes.ATTACK_SPEED,
        Attributes.ATTACK_KNOCKBACK
    );

    private static final Identifier KNOCKBACK_ID = Identifier.fromNamespaceAndPath("mms_weapons", "shield_knockback");

    // Kite shields are heavier than round shields: held in either hand, they slow the user a little.
    private static final Identifier KITE_SLOW_ID = Identifier.fromNamespaceAndPath("mms_weapons", "kite_shield_slow");
    private static final double KITE_SLOW = -0.15;

    private ShieldWeapons() {
    }

    // A blocking weapon covers a narrower arc, takes longer to raise and stays disabled longer than a shield.
    private static final float WEAPON_GUARD_ARC = 0.75F;
    private static final float WEAPON_EXTRA_RAISE_SECONDS = 0.15F;
    private static final float WEAPON_DISABLE_SCALE = 1.5F;

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(ShieldWeapons::isShield, (builder, item) ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, stats(item)));
            context.modify(item -> isBlockingWeapon(item) && !WeaponTuning.blocksAttacks(item), (builder, item) ->
                builder.set(DataComponents.BLOCKS_ATTACKS, weaponGuard(item, item.components().get(DataComponents.BLOCKS_ATTACKS))));
            context.modify(ShieldWeapons::isShield, (builder, item) -> {
                BlocksAttacks guard = shieldGuard(item);
                if (guard != null) {
                    builder.set(DataComponents.BLOCKS_ATTACKS, guard);
                }
            });
        });
    }

    // A shield's guard with its material's share; a kite shield takes its round shield's guard first.
    private static BlocksAttacks shieldGuard(Item item) {
        boolean kite = isKiteShield(item);
        BlocksAttacks base = kite ? materialGuard(item) : null;
        if (base == null) {
            base = item.components().get(DataComponents.BLOCKS_ATTACKS);
        }
        return base == null ? null : withShare(base, ShieldGuards.shieldShare(item, kite), 1.0F, 0.0F, 1.0F);
    }

    // Paladins tiers with no round shield of their own, matched to the nearest vanilla tier.
    private static final java.util.Map<String, String> SHIELD_ALIASES = java.util.Map.of(
        "wood", "wooden",
        "gold", "golden",
        "aeternium", "netherite",
        "aether", "diamond"
    );

    private static boolean isKiteShield(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getPath().endsWith("_kite_shield")
            && (id.getNamespace().equals("paladins") || id.getNamespace().equals("mms_weapons"));
    }

    // The guard of the round shield in the item's material, or null when its id names none.
    public static BlocksAttacks materialGuard(Item item) {
        String[] parts = BuiltInRegistries.ITEM.getKey(item).getPath().split("_");
        for (int end = parts.length - 1; end >= 1; end--) {
            String material = String.join("_", java.util.Arrays.copyOfRange(parts, 0, end));
            material = SHIELD_ALIASES.getOrDefault(material, material);
            for (String namespace : new String[] {"expanded_weaponry", "mms_weapons"}) {
                Identifier shieldId = Identifier.fromNamespaceAndPath(namespace, material + "_shield");
                Item shield = BuiltInRegistries.ITEM.getOptional(shieldId).orElse(null);
                if (shield != null && shield != item) {
                    BlocksAttacks guard = shield.components().get(DataComponents.BLOCKS_ATTACKS);
                    if (guard != null) {
                        return guard;
                    }
                }
            }
        }
        return null;
    }

    // A blocking weapon: its material's round shield, weakened; the fallback guard when it has no material.
    public static BlocksAttacks weaponGuard(Item item, BlocksAttacks fallback) {
        BlocksAttacks guard = materialGuard(item);
        return withShare(guard != null ? guard : fallback, ShieldGuards.weaponShare(item),
            WEAPON_GUARD_ARC, WEAPON_EXTRA_RAISE_SECONDS, WEAPON_DISABLE_SCALE);
    }

    // A weapon that blocks on its own: it has attack damage and blocks, but is not a shield.
    public static boolean isBlockingWeapon(Item item) {
        return item.components().has(DataComponents.BLOCKS_ATTACKS) && !isShield(item);
    }

    private static BlocksAttacks withShare(BlocksAttacks guard, float share, float arc, float extraRaise, float disableScale) {
        List<BlocksAttacks.DamageReduction> reductions = guard.damageReductions().stream()
            .map(r -> new BlocksAttacks.DamageReduction(r.horizontalBlockingAngle() * arc, r.type(), 0.0F, share))
            .toList();
        return new BlocksAttacks(guard.blockDelaySeconds() + extraRaise, guard.disableCooldownScale() * disableScale,
            reductions, guard.itemDamage(), guard.bypassedBy(), guard.blockSound(), guard.disableSound());
    }

    // A ShieldItem, or any item that blocks attacks without being a weapon already.
    public static boolean isShield(Item item) {
        if (item instanceof ShieldItem) {
            return true;
        }
        // Shields get attack damage from stats(), so a shield is recognised by the knockback
        // modifier only shields receive, or, before that runs, by having no attack damage.
        return item.components().has(DataComponents.BLOCKS_ATTACKS) && (hasShieldStats(item) || !hasAttackDamage(item));
    }

    private static boolean hasShieldStats(Item item) {
        return modifiers(item).modifiers().stream().anyMatch(entry -> entry.modifier().id().equals(KNOCKBACK_ID));
    }

    private static boolean hasAttackDamage(Item item) {
        return modifiers(item).modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE));
    }

    private static ItemAttributeModifiers modifiers(Item item) {
        return item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
    }

    private static ItemAttributeModifiers stats(Item item) {
        ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : modifiers(item).modifiers()) {
            if (!REPLACED.contains(entry.attribute())) {
                rebuilt.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        rebuilt.add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage(item) - BASE_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        );
        rebuilt.add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, SWORD_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        );
        rebuilt.add(
            Attributes.ATTACK_KNOCKBACK,
            new AttributeModifier(KNOCKBACK_ID, KNOCKBACK_BONUS, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        );
        if (isKiteShield(item)) {
            rebuilt.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(KITE_SLOW_ID, KITE_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.HAND
            );
        }
        return rebuilt.build();
    }

    // Attack damage by material, read from the item id.
    private static double damage(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.contains("netherite")) {
            return 7.5;
        }
        if (path.contains("diamond")) {
            return 7.0;
        }
        if (path.contains("shadoline")) {
            return 6.5;
        }
        if (path.contains("iron")) {
            return 6.0;
        }
        if (path.contains("copper") || path.contains("gold") || path.contains("azurite")) {
            return 5.5;
        }
        if (path.contains("wood") || path.contains("stone") || item == Items.SHIELD) {
            return 5.0;
        }
        return 6.0;
    }
}
