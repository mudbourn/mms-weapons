package info.mudbourn.mmsweapons.focus;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.throwable.sickle.SickleThrowing;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

// Focus for weapons with no right click of their own: damage dealt with one fills a bar, and a full bar unlocks the barrage.
// Also holds the shared rule that a one-handed weapon gives its right click up to a shield in the off hand.
public final class Focus {

    public static final float MAX_FOCUS = 100.0F;
    private static final Map<Class<?>, Boolean> OVERRIDES_USE = new ConcurrentHashMap<>();
    // A flat item sprite's blade, corner to corner in model space: handle bottom left, tip top right.
    private static final Blade DIAGONAL = new Blade(new Vec3(0.0, 0.0, 0.5), new Vec3(1.0, 1.0, 0.5));
    private static final Map<Item, Blade> BLADES = new ConcurrentHashMap<>();
    private static final List<Predicate<ItemStack>> SMOKE_SOURCES = new CopyOnWriteArrayList<>();

    public static final DataComponentType<Float> FOCUS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("mms_weapons", "focus"),
            DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

    private Focus() {
    }

    public static void register() {
    }

    // Adds melee damage dealt with an eligible weapon to its focus. Called for every hit, killing blows included.
    public static void onDamage(DamageSource source, float damage) {
        if (Barrage.striking() || damage <= 0.0F) {
            return;
        }
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
            ItemStack held = player.getMainHandItem();
            if (eligible(held)) {
                held.set(FOCUS, Math.min(MAX_FOCUS, focus(held) + damage));
            }
        }
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

    // Lets another mod's focused weapons smoke too, such as the Armory's Dragon Slayer.
    public static void addSmokeSource(Predicate<ItemStack> focused) {
        SMOKE_SOURCES.add(focused);
    }

    // Whether this stack should be giving off focus smoke.
    public static boolean showsSmoke(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (focused(stack) && eligible(stack)) {
            return true;
        }
        for (Predicate<ItemStack> source : SMOKE_SOURCES) {
            if (source.test(stack)) {
                return true;
            }
        }
        return false;
    }

    // A Better Combat weapon with no right click of its own: it does not block, eat, equip, throw, or override use.
    // The mace is left out because Hurl makes it throwable, and spears because they charge their own attack.
    public static boolean eligible(ItemStack stack) {
        if (stack.isEmpty() || WeaponRegistry.getAttributes(stack) == null) {
            return false;
        }
        if (stack.has(DataComponents.BLOCKS_ATTACKS) || stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.EQUIPPABLE)) {
            return false;
        }
        if (Chainlink.throwable(stack) || stack.is(MmsThrowables.THROWABLE_WEAPONS) || SickleThrowing.isSickle(stack) || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) {
            return false;
        }
        // Spears have their own charged attack, vanilla's through these components and modded ones by name.
        if (stack.has(DataComponents.KINETIC_WEAPON) || stack.has(DataComponents.PIERCING_WEAPON)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("spear")) {
            return false;
        }
        return !OVERRIDES_USE.computeIfAbsent(stack.getItem().getClass(), Focus::overridesUse);
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
        boolean weaponRightClick = attributes != null || Chainlink.throwable(weapon) || SickleThrowing.isSickle(weapon)
                || weapon.is(MmsThrowables.THROWABLE_WEAPONS) || weapon.is(Items.TRIDENT);
        return weaponRightClick && (attributes == null || !attributes.isTwoHanded());
    }


}
