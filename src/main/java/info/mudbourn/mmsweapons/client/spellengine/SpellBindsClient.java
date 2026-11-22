package info.mudbourn.mmsweapons.client.spellengine;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

// The key (O by default) that opens the screen for swapping spell hotbar keys.
public final class SpellBindsClient {

    private SpellBindsClient() {
    }

    public static void register() {
        SpellBinds.load();
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mms_weapons.arrange_spell_binds",
            InputConstants.KEY_O,
            mmsCategory()
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new SpellBindsScreen());
                }
            }
        });
    }

    private static KeyMapping.Category mmsCategory() {
        Identifier id = Identifier.fromNamespaceAndPath("mms", "main");
        try {
            return KeyMapping.Category.register(id);
        } catch (IllegalArgumentException alreadyRegistered) {
            return new KeyMapping.Category(id);
        }
    }
}
