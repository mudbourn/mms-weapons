package info.mudbourn.mmsweapons.material;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

// Item ids for Aerial Hell's arsonist material follow the display names: phoenix, templar and bishop, with the old ids kept as aliases.
public final class PhoenixNames {

    private static final String[][] RENAMES = {
        {"heavy_arsonist_", "heavy_phoenix_"},
        {"arsonist_", "phoenix_"}
    };
    private static final String[][] SETS = {
        {"_crusader_armor_", "_templar_armor_"},
        {"_prior_robe_", "_bishop_robe_"}
    };

    private PhoenixNames() {
    }

    // Registers the item under its display-name id and aliases the old id to it.
    static Item register(String name, Function<ResourceKey<Item>, Item> factory) {
        String path = path(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", path));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
        if (!path.equals(name)) {
            BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath("mms_weapons", name), Identifier.fromNamespaceAndPath("mms_weapons", path));
        }
        return item;
    }

    // The display-name id for an arsonist item name; other names pass through.
    static String path(String name) {
        for (String[] rename : RENAMES) {
            if (name.startsWith(rename[0])) {
                String path = rename[1] + name.substring(rename[0].length());
                for (String[] set : SETS) {
                    path = path.replace(set[0], set[1]);
                }
                return path;
            }
        }
        return name;
    }

    // The arsonist name behind a display-name id, for code that reads the material from an id.
    public static String legacy(String path) {
        for (String[] rename : RENAMES) {
            if (path.startsWith(rename[1])) {
                String name = rename[0] + path.substring(rename[1].length());
                for (String[] set : SETS) {
                    name = name.replace(set[1], set[0]);
                }
                return name;
            }
        }
        return path;
    }
}
