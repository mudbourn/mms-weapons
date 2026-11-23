package info.mudbourn.mmsweapons;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.focus.Barrage;
import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.material.CompatWeapons;
import info.mudbourn.mmsweapons.material.HeavyArmors;
import info.mudbourn.mmsweapons.material.MaterialpackTab;
import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import info.mudbourn.mmsweapons.shieldbash.ShieldWeapons;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import info.mudbourn.mmsweapons.spellengine.SpellEngineBarrage;
import info.mudbourn.mmsweapons.spellengine.WeaponSkills;
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
        info.mudbourn.mmsweapons.downed.Downed.register();

        // Anchor/tuna swing cooldown, anchor and tuna knockback, glaive blocking, longbow damage
        WeaponTuning.register();
        info.mudbourn.mmsweapons.aerial.AerialGear.register();
        // Hammer and mace weight curve, and the heavy hammers' forced smash.
        info.mudbourn.mmsweapons.throwable.Hammers.register();

        // Thrown-weapon projectile. Registered unconditionally: the entity type
        // has to exist on both sides regardless of which weapon mods are present,
        // or a saved projectile comes back as an unknown entity.
        MmsThrowables.register();

        // Chain throw state for the Bountiful Fish anchor.
        Chainlink.register();

        // Focus and the barrage, guard breaks, throw interrupts, shield priority.
        Focus.register();
        Barrage.register();
        info.mudbourn.mmsweapons.focus.DashStep.register();

        // Shields as main-hand weapons, and the guard ram.
        ShieldWeapons.register();
        ShieldRam.register();

        // Heavy armor in other mods' materials
        HeavyArmors.register();

        // Aerial Hell and Enderscape material weapons from Weapons Expanded, Expanded Weaponry and Paladins
        CompatWeapons.register();
        info.mudbourn.mmsweapons.material.VoidCharge.register();
        MaterialpackTab.register();

        // The weapon skills, only where Spell Engine is installed.
        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
            SpellEngineBarrage.register();
            info.mudbourn.mmsweapons.spellengine.WizardFocus.register();
            info.mudbourn.mmsweapons.spellengine.DownedCasting.register();
            info.mudbourn.mmsweapons.spellengine.SpellRevive.register();
            WeaponSkills.register();
        }

        if (FabricLoader.getInstance().isModLoaded("paladins")) {
            info.mudbourn.mmsweapons.paladins.PaladinsTrades.register();
        }

        LOG.info("MMS Weapons loaded: weapon tuning, throwable melee, anchor chain throw, focus barrage, mace smash, longbow tuning.");
    }
}
