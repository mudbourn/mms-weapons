package info.mudbourn.mmsweapons.material;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

// A shadow sword and tools, which Aerial Hell doesn't have, with vanilla's damage and speed per tool type.
final class ShadowTools {

    private ShadowTools() {
    }

    static void register(ToolMaterial material) {
        CompatWeapons.add(CreativeModeTabs.COMBAT, "shadow_sword", p -> new Item(p.sword(material, 3.0F, -2.4F)));
        CompatWeapons.add(CreativeModeTabs.TOOLS_AND_UTILITIES, "shadow_shovel", p -> new Item(p.shovel(material, 1.5F, -3.0F)));
        CompatWeapons.add(CreativeModeTabs.TOOLS_AND_UTILITIES, "shadow_pickaxe", p -> new Item(p.pickaxe(material, 1.0F, -2.8F)));
        CompatWeapons.add(CreativeModeTabs.TOOLS_AND_UTILITIES, "shadow_axe", p -> new Item(p.axe(material, 5.0F, -3.0F)));
        CompatWeapons.add(CreativeModeTabs.TOOLS_AND_UTILITIES, "shadow_hoe", p -> new Item(p.hoe(material, -3.0F, 0.0F)));
    }
}
