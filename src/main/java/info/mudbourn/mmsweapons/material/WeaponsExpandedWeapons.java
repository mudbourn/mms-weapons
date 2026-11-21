package info.mudbourn.mmsweapons.material;

import net.angelic.weaponsexpanded.item.ModItemProperties;
import net.angelic.weaponsexpanded.item.custom.BastardSwordItem;
import net.angelic.weaponsexpanded.item.custom.HalberdItem;
import net.angelic.weaponsexpanded.item.custom.WarhammerItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

// Weapons Expanded weapon types in the given material.
final class WeaponsExpandedWeapons {

    private WeaponsExpandedWeapons() {
    }

    static void register(String name, ToolMaterial material) {
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_rapier", p -> new Item(ModItemProperties.pierce(material, 1.5F, -2.2F, p)));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_sickle", p -> new Item(p.sword(material, 4.0F, -2.6F)));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_longsword", p -> new BastardSwordItem(material, 6.0F, -3.0F, 7.0F, -3.0F, p));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_katana", p -> new Item(p.sword(material, 3.0F, -2.2F)));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_battleaxe", p -> new Item(p.axe(material, 8.0F, -3.3F)));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_warhammer", p -> new WarhammerItem(material, 5.0F, -3.0F, 5.0F, -2.8F, p));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_morningstar", p -> new Item(ModItemProperties.blunt(material, 5.0F, -2.9F, p)));
        CompatWeapons.add(CreativeModeTabs.COMBAT, name + "_halberd", p -> new HalberdItem(material, 6.5F, -3.1F, 6.0F, -3.2F, p));
    }
}
