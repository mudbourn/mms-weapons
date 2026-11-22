package info.mudbourn.mmsweapons.material;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Which mods a compat item stands for: the mod its material comes from and the mod whose weapon or armor type it is.
 * Tooltip mod-name lines (WTHIT, JEI) show "Aerial Hell × Expanded Weaponry" instead of "MMS Weapons".
 */
public final class CompatOrigin {
    private static final Map<Item, String> LABELS = new IdentityHashMap<>();
    private static String weaponMod;

    private CompatOrigin() {
    }

    /** Sets the weapon mod for items registered until the next call (null for the material mod's own item types). */
    static void weaponMod(String modId) {
        weaponMod = modId;
    }

    static void record(Item item, String materialMod) {
        record(item, materialMod, weaponMod);
    }

    static void record(Item item, String materialMod, String typeMod) {
        String label = name(materialMod);
        if (typeMod != null && !typeMod.equals(materialMod)) label += " × " + name(typeMod);
        LABELS.put(item, label);
    }

    /** The mod-name line for this stack, or null when it isn't a compat item. */
    public static String label(ItemStack stack) {
        return LABELS.get(stack.getItem());
    }

    private static String name(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId);
    }
}
