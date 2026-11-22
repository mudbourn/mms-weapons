package info.mudbourn.mmsweapons.client.origin;

import info.mudbourn.mmsweapons.material.CompatOrigin;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEventListener;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.item.ItemStack;

// WTHIT's item tooltip mod-name line: compat items name the mods they stand for, not MMS Weapons.
public final class OriginWthitPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.eventListener(new IEventListener() {
            @Override
            public String getHoveredItemModName(ItemStack stack, IPluginConfig config) {
                return CompatOrigin.label(stack);
            }
        }, 100);
    }
}
