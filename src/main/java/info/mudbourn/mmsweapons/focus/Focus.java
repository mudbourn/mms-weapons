package info.mudbourn.mmsweapons.focus;

import info.mudbourn.mmsweapons.chainlink.Chainlink;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// The focus engine for every weapon: damage dealt with a focus weapon fills its bar, and a full bar unlocks its barrage or own focus attack.
public final class Focus {

    public static final float MAX_FOCUS = 100.0F;
    private static final Map<Class<?>, Boolean> OVERRIDES_USE = new ConcurrentHashMap<>();
    // A flat item sprite's blade, corner to corner in model space: handle bottom left, tip top right.
    private static final Blade DIAGONAL = new Blade(new Vec3(0.0, 0.0, 0.5), new Vec3(1.0, 1.0, 0.5));
    private static final Map<Item, Blade> BLADES = new ConcurrentHashMap<>();
    private static final Set<Item> OWN_ATTACKS = ConcurrentHashMap.newKeySet();
    private static boolean spending;

    public static final DataComponentType<Float> FOCUS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("mms_weapons", "focus"),
            DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .ignoreSwapAnimation()
                    .build());

    private Focus() {
    }

    public static void register() {
    }

    // Every focus weapon fills its bar in the same number of swings, whatever its damage.
    // A weapon can ask for a different count when it registers (the Dragon Slayer fills in 4).
    public static final int DEFAULT_SWINGS_TO_FILL = 6;
    private static final Map<Item, Integer> SWINGS_TO_FILL = new ConcurrentHashMap<>();
    // Game time of each player's last counted swing, so a sweep hitting several targets counts once.
    private static final Map<UUID, Long> LAST_SWING = new ConcurrentHashMap<>();

    // Adds one swing's share of the bar for a melee hit with a focus weapon. Called for every hit, killing blows included.
    public static void onDamage(DamageSource source, float damage) {
        if (spending || damage <= 0.0F) {
            return;
        }
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
            ItemStack held = player.getMainHandItem();
            // A spellcaster's weapon fills from its spells, not its melee hits.
            if (!builds(held) || spellFocus(held)) {
                return;
            }
            long now = player.level().getGameTime();
            Long last = LAST_SWING.put(player.getUUID(), now);
            if (last != null && last == now) {
                return;
            }
            float share = MAX_FOCUS / SWINGS_TO_FILL.getOrDefault(held.getItem(), DEFAULT_SWINGS_TO_FILL);
            held.set(FOCUS, Math.min(MAX_FOCUS, focus(held) + share));
        }
    }

    // RPG Series archetypes that mark spellcasters' weapons, such as the Wizards and Paladins staves and wands.
    private static final List<TagKey<Item>> SPELL_FOCUS_TAGS = List.of(
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("rpg_series", "archetype/magic_damage_weapon")),
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("rpg_series", "archetype/healing_weapon")));

    // Whether this is a spellcaster's weapon, which fills its focus from spell casts and spends it on an empowered spell.
    public static boolean spellFocus(ItemStack stack) {
        return !stack.isEmpty() && SPELL_FOCUS_TAGS.stream().anyMatch(stack::is);
    }

    // Adds one cast's share of the bar to a spellcaster's weapon.
    public static void onSpellCast(ItemStack held) {
        if (!spellFocus(held) || !builds(held)) {
            return;
        }
        float share = MAX_FOCUS / SWINGS_TO_FILL.getOrDefault(held.getItem(), DEFAULT_SWINGS_TO_FILL);
        held.set(FOCUS, Math.min(MAX_FOCUS, focus(held) + share));
    }

    // Deals a focus attack's hits without letting them refill the focus that paid for them.
    public static void spend(Runnable hits) {
        spending = true;
        try {
            hits.run();
        } finally {
            spending = false;
        }
    }

    // Marks a weapon that spends focus on its own attack instead of the barrage, such as the Armory's Dragon Slayer.
    public static void registerFocusWeapon(Item item) {
        OWN_ATTACKS.add(item);
    }

    // Same, with its own number of swings to fill the bar.
    public static void registerFocusWeapon(Item item, int swingsToFill) {
        OWN_ATTACKS.add(item);
        SWINGS_TO_FILL.put(item, swingsToFill);
    }

    // Whether damage dealt with this stack builds focus, for the barrage or for its own focus attack.
    public static boolean builds(ItemStack stack) {
        return !stack.isEmpty() && (OWN_ATTACKS.contains(stack.getItem()) || eligible(stack) || spellFocus(stack) || Whirlwind.guardWeapon(stack));
    }

    public static float focus(ItemStack stack) {
        return Math.min(MAX_FOCUS, stack.getOrDefault(FOCUS, 0.0F));
    }

    public static boolean focused(ItemStack stack) {
        return focus(stack) >= MAX_FOCUS;
    }

    // Where a weapon's blade runs in item model space, where the model fills the 0 to 1 block once its display transform is applied.
    // A line through the points in order, so a curved blade can bend with its model.
    public record Blade(List<Vec3> points) {
        public Blade(Vec3 start, Vec3 end) {
            this(List.of(start, end));
        }
    }

    // Sets the blade segment for a weapon whose model is not a corner-to-corner sprite, such as a geo model.
    public static void registerBlade(Item item, Blade blade) {
        BLADES.put(item, blade);
    }

    public static Blade blade(ItemStack stack) {
        return BLADES.getOrDefault(stack.getItem(), DIAGONAL);
    }

    // Whether this stack should be giving off focus smoke.
    public static boolean showsSmoke(ItemStack stack) {
        return focused(stack) && builds(stack);
    }

    // A Better Combat weapon with no right click of its own: it does not block, eat, equip, throw, or override use.
    public static boolean eligible(ItemStack stack) {
        if (stack.isEmpty() || WeaponRegistry.getAttributes(stack) == null) {
            return false;
        }
        if (stack.has(DataComponents.BLOCKS_ATTACKS) || stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.EQUIPPABLE)) {
            return false;
        }
        if (Chainlink.throwable(stack) && !Chainlink.keepsBarrage(stack)) {
            return false;
        }
        if (stack.is(MmsThrowables.THROWABLE_WEAPONS) || SickleThrowing.isSickle(stack) || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) {
            return false;
        }
        // Spears have their own charged attack, vanilla's through the kinetic component and modded ones by name. A
        // piercing component alone is a left-click jab (Weapons Expanded's halberd pierce mode), which leaves the skill free.
        if (stack.has(DataComponents.KINETIC_WEAPON)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("spear")) {
            return false;
        }
        if (AERIAL_IDLE_USE.computeIfAbsent(stack.getItem(), Focus::aerialIdleUse)) {
            return true;
        }
        if (isPaladinsMace(stack)) {
            return true;
        }
        return !OVERRIDES_USE.computeIfAbsent(stack.getItem().getClass(), Focus::overridesUse);
    }

    // Paladins maces override use only for their Smash skill, so the barrage can sit beside it.
    private static boolean isPaladinsMace(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("paladins") && id.getPath().endsWith("_mace");
    }

    private static final Map<Item, Boolean> AERIAL_IDLE_USE = new java.util.concurrent.ConcurrentHashMap<>();

    // Aerial Hell's item class overrides right click for its abilities, but an item with no ability and no use
    // duration (the Heavy Axe, the heavy battleaxe, most of its weapons) does nothing with it, so it can still take
    // the focus skill. Read by reflection, since Aerial Hell is optional.
    private static boolean aerialIdleUse(Item item) {
        for (Class<?> c = item.getClass(); c != null && c != Item.class; c = c.getSuperclass()) {
            if (!c.getName().equals("fr.factionbedrock.aerialhell.Item.AerialHellItem")) {
                continue;
            }
            try {
                Object selector = c.getField("abilitySelector").get(item);
                int duration = c.getField("maxUseDuration").getInt(item);
                return selector == null && duration <= 0;
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
        return false;
    }

    // Matched on the parameter types, not the name, so it still works once the game's names are remapped.
    private static boolean overridesUse(Class<?> type) {
        for (Class<?> c = type; c != null && c != Item.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 3 && params[0] == Level.class && params[1] == Player.class
                        && params[2] == InteractionHand.class && method.getReturnType() == InteractionResult.class) {
                    return true;
                }
            }
        }
        return false;
    }

    // Whether a shield in the off hand takes the right click from this one-handed weapon.
    public static boolean shieldTakesRightClick(Player player, ItemStack weapon, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || weapon.has(DataComponents.BLOCKS_ATTACKS)) {
            return false;
        }
        if (!player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return false;
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(weapon);
        if (attributes == null) {
            return weapon.is(Items.TRIDENT);
        }
        return !attributes.isTwoHanded();
    }
}
