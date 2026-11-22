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
            context.modify(ShieldWeapons::isBlockingWeapon, (builder, item) ->
                builder.set(DataComponents.BLOCKS_ATTACKS, weakerGuard(item.components().get(DataComponents.BLOCKS_ATTACKS))));
        });
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
