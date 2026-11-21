package info.mudbourn.mmsweapons.client.material;

import info.mudbourn.mmsweapons.material.CompatWeapons;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.resources.Identifier;

// Model layers for the compat shields's Expanded Weaponry shield renderer.
public final class CompatShieldClient {

    private CompatShieldClient() {
    }

    public static void register() {
        for (String material : CompatWeapons.MATERIALS.keySet()) {
            ModelLayerRegistry.registerModelLayer(
                new ModelLayerLocation(Identifier.fromNamespaceAndPath("mms_weapons", material + "_shield"), "main"),
                ShieldModel::createLayer
            );
        }
    }
}
