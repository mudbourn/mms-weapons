package info.mudbourn.mmsweapons.material;

import com.kielson.item.CustomShield;
import com.kielson.item.ShieldMaterial;
import com.kielson.item.custom.HammerItem;
import com.kielson.item.custom.TwinBladeSwordItem;
import net.minecraft.world.item.ToolMaterial;

// Expanded Weaponry weapon types in the given material.
final class ExpandedWeaponryWeapons {

    private ExpandedWeaponryWeapons() {
    }

    static void register(String name, ToolMaterial material) {
        CompatWeapons.add(CompatWeapons.EXPANDED_WEAPONRY_TAB, name + "_hammer", p -> new HammerItem(material, p));
        CompatWeapons.add(CompatWeapons.EXPANDED_WEAPONRY_TAB, name + "_twin_blade_sword", p -> new TwinBladeSwordItem(material, p));
        ShieldMaterial shield = new ShieldMaterial(ShieldTiers.of(name).roundDurability, ShieldTiers.baseBlocked(name),
            ShieldTiers.blockedShare(name), ShieldTiers.disabledSeconds(name), material.repairItems());
        CompatWeapons.add(CompatWeapons.EXPANDED_WEAPONRY_TAB, name + "_shield", p -> new CustomShield(shield, p));
    }
}
