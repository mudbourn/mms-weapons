package info.mudbourn.mmsweapons;

import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MmsWeapons implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons");

    @Override
    public void onInitialize() {
        // Tuna swing/slam sound events, referenced by Better Combat weapon_attributes
        MmsSounds.register();

        // Anchor/tuna swing cooldown, tuna knockback, glaive blocking, longbow damage
        WeaponTuning.register();

        // Thrown-weapon projectile. Registered unconditionally: the entity type
        // has to exist on both sides regardless of which weapon mods are present,
        // or a saved projectile comes back as an unknown entity.
        MmsThrowables.register();

        LOG.info("MMS Weapons loaded — weapon tuning, throwable melee, mace smash, longbow tuning.");
    }
}
