package info.mudbourn.mmsweapons.shieldbash;

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
    private static final double KITE_SLOW = -0.05;

    private ShieldWeapons() {
    }

    // A weapon that can also block gets a weaker guard than a real shield, so a shield stays
    // worth carrying: it lets more damage through, covers a narrower arc, takes longer to raise
    // and stays disabled longer when an axe knocks it down.
    private static final float WEAPON_BLOCK_SHARE = 0.6F;
    private static final float WEAPON_GUARD_ARC = 0.75F;
    private static final float WEAPON_EXTRA_RAISE_SECONDS = 0.15F;
    private static final float WEAPON_DISABLE_SCALE = 1.5F;

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(ShieldWeapons::isShield, (builder, item) ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, stats(item)));
            // Weapons WeaponTuning gives blocking are weakened there, from their material's shield.
            context.modify(item -> isBlockingWeapon(item) && !info.mudbourn.mmsweapons.WeaponTuning.blocksAttacks(item), (builder, item) ->
                builder.set(DataComponents.BLOCKS_ATTACKS, weakerGuard(item.components().get(DataComponents.BLOCKS_ATTACKS))));
            // Paladin kite shields (Paladins' own and the pack-material ones) block like their
            // material's round shield instead of Spell Engine's full vanilla block.
            context.modify(ShieldWeapons::isKiteShield, (builder, item) -> {
                BlocksAttacks guard = materialGuard(item);
                if (guard != null) {
                    builder.set(DataComponents.BLOCKS_ATTACKS, guard);
                }
            });
        });
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

    // The blocking of the round shield in the item's material: Expanded Weaponry's wooden to
    // netherite shields, or the pack-material shields. The material is the longest leading run of
    // the item's id that names a round shield; null when none does.
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
        return weakerGuard(guard != null ? guard : fallback);
    }

    // A weapon that blocks on its own: it has attack damage and blocks, but is not a shield.
    public static boolean isBlockingWeapon(Item item) {
        return item.components().has(DataComponents.BLOCKS_ATTACKS) && !isShield(item);
    }

    private static BlocksAttacks weakerGuard(BlocksAttacks guard) {
        List<BlocksAttacks.DamageReduction> reductions = guard.damageReductions().stream()
            .map(r -> new BlocksAttacks.DamageReduction(r.horizontalBlockingAngle() * WEAPON_GUARD_ARC, r.type(),
                r.base() * WEAPON_BLOCK_SHARE, r.factor() * WEAPON_BLOCK_SHARE))
            .toList();
        return new BlocksAttacks(guard.blockDelaySeconds() + WEAPON_EXTRA_RAISE_SECONDS,
            guard.disableCooldownScale() * WEAPON_DISABLE_SCALE, reductions, guard.itemDamage(),
            guard.bypassedBy(), guard.blockSound(), guard.disableSound());
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
