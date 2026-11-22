package info.mudbourn.mmsweapons.paladins;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Monk trade overrides for Paladins, shipped as a built-in data pack that is on by default.
public final class PaladinsTrades {

    private PaladinsTrades() {
    }

    public static void register() {
        FabricLoader.getInstance().getModContainer("mms_weapons").ifPresent(mod -> ResourceLoader.registerBuiltinPack(
            Identifier.fromNamespaceAndPath("mms_weapons", "paladins_trades"),
            mod,
            Component.literal("MMS Monk Trades"),
            PackActivationType.DEFAULT_ENABLED
        ));
    }
}
