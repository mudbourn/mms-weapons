package info.mudbourn.mmsweapons.shieldbash;

import info.mudbourn.mmsweapons.material.PhoenixNames;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;

// Share of each hit a shield stops, by material. The one table every shield, kite shield and blocking weapon reads.
public final class ShieldGuards {

    private static final float DEFAULT_SHARE = 0.7F;
    private static final float KITE_BONUS = 0.05F;
    private static final float MAX_SHARE = 0.95F;
    public static final float WEAPON_SCALE = 0.6F;

    private static final Map<String, Float> SHARES = Map.ofEntries(
        Map.entry("wooden", 0.5F),
        Map.entry("sky_wood", 0.5F),
        Map.entry("magmatic_gel", 0.5F),
        Map.entry("stone", 0.6F),
        Map.entry("stellar_stone", 0.6F),
        Map.entry("azurite", 0.6F),
        Map.entry("copper", 0.6F),
        Map.entry("golden", 0.6F),
        Map.entry("iron", 0.7F),
        Map.entry("ruby", 0.7F),
        Map.entry("shadoline", 0.7F),
        Map.entry("diamond", 0.8F),
        Map.entry("aether", 0.8F),
        Map.entry("obsidian", 0.8F),
        Map.entry("shadow", 0.8F),
        Map.entry("netherite", 0.85F),
        Map.entry("aeternium", 0.9F),
        Map.entry("arsonist", 0.9F),
        Map.entry("lunatic", 0.9F),
        Map.entry("volucite", 0.9F)
    );

    private static final Map<String, String> ALIASES = Map.of(
        "wood", "wooden",
        "gold", "golden"
    );

    private ShieldGuards() {
    }

    // What a round shield of the named pack material stops.
    public static float roundShare(String material) {
        return SHARES.getOrDefault(material, DEFAULT_SHARE);
    }

    // What a round shield of the item's material stops; kite shields stop a little more.
    public static float shieldShare(Item item, boolean kite) {
        float share = materialShare(item);
        return kite ? Math.min(share + KITE_BONUS, MAX_SHARE) : share;
    }

    // What a blocking weapon stops: its material's round shield, scaled down.
    public static float weaponShare(Item item) {
        return materialShare(item) * WEAPON_SCALE;
    }

    // The longest leading run of the item's id that names a material in the table.
    private static float materialShare(Item item) {
        if (item == Items.SHIELD) {
            return SHARES.get("wooden");
        }
        String[] parts = PhoenixNames.legacy(BuiltInRegistries.ITEM.getKey(item).getPath()).split("_");
        for (int end = parts.length - 1; end >= 1; end--) {
            String material = String.join("_", Arrays.copyOfRange(parts, 0, end));
            Float share = SHARES.get(ALIASES.getOrDefault(material, material));
            if (share != null) {
                return share;
            }
        }
        return DEFAULT_SHARE;
    }
}
