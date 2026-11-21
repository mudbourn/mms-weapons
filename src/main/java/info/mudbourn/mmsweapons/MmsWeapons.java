package info.mudbourn.mmsweapons;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.focus.FocusSkill;
import info.mudbourn.mmsweapons.material.CompatWeapons;
import info.mudbourn.mmsweapons.material.HeavyArmors;
import info.mudbourn.mmsweapons.material.MaterialpackTab;
import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import info.mudbourn.mmsweapons.shieldbash.ShieldWeapons;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.spellengine.SpellEngineBarrage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MmsWeapons implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons");

    @Override
    public void onInitialize() {
        // Tuna swing/slam sound events, referenced by Better Combat weapon_attributes
        MmsSounds.register();

        // Anchor/tuna swing cooldown, anchor and tuna knockback, glaive blocking, longbow damage
        WeaponTuning.register();

        // Thrown-weapon projectile. Registered unconditionally: the entity type
        // has to exist on both sides regardless of which weapon mods are present,
        // or a saved projectile comes back as an unknown entity.
        MmsThrowables.register();

        // Chain throw for the Bountiful Fish anchor.
        Chainlink.register();

        // Focus and the barrage for weapons with no right click, guard breaks, throw interrupts, shield priority.
        Focus.register();
        Barrage.register();
        FocusSkill.register();

        // Shields as main-hand weapons, and the guard ram on the combat roll key.
        ShieldWeapons.register();
        ShieldRam.register();

        // Heavy armor in other mods' materials
        HeavyArmors.register();

        // Azurite, ruby and shadoline weapons from Weapons Expanded and Expanded Weaponry
        CompatWeapons.register();
        info.mudbourn.mmsweapons.material.VoidCharge.register();
        MaterialpackTab.register();

        // The barrage as a Spell Engine skill, only where Spell Engine is installed.
        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
            SpellEngineBarrage.register();
        }

        LOG.info("MMS Weapons loaded — weapon tuning, throwable melee, anchor chain throw, focus barrage, mace smash, longbow tuning.");
    }
}
