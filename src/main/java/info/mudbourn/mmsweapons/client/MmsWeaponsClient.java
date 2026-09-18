package info.mudbourn.mmsweapons.client;

import info.mudbourn.mmsweapons.client.throwable.ThrownWeaponRenderer;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class MmsWeaponsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Thrown weapons draw their own item model, so there is no model layer to
        // bake — just the renderer, which has to be present wherever the entity
        // type is, i.e. always.
        EntityRendererRegistry.register(
                MmsThrowables.THROWN_WEAPON,
                ThrownWeaponRenderer::new);
    }
}
