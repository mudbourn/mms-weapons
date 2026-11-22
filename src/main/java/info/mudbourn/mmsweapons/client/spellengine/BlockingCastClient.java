package info.mudbourn.mmsweapons.client.spellengine;

import info.mudbourn.mmsweapons.shieldbash.ShieldRam;
import info.mudbourn.mmsweapons.spellengine.WeaponSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.spell_engine.client.input.SpellHotbar;

import java.util.List;

// Lets the shield ram be cast while the shield is raised, which Spell Engine's own key handling skips for any item in use.
public final class BlockingCastClient {

    private BlockingCastClient() {
    }

    public static void handle(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.options == null || !player.isUsingItem() || ShieldRam.ramShield(player) == null) {
            return;
        }
        for (SpellHotbar.Slot slot : SpellHotbar.INSTANCE.slots) {
            boolean ram = slot.spell() != null && slot.spell().is(WeaponSkills.SHIELD_RAM);
            if (ram && slot.getKeyBinding(client.options) != client.options.keyUse) {
                SpellHotbar.INSTANCE.handleSome(player, slot, client.options, List.of());
                return;
            }
        }
    }
}
