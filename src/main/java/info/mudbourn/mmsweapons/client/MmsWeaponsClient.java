package info.mudbourn.mmsweapons.client;

import info.mudbourn.mmsweapons.chainlink.Chainlink;
import info.mudbourn.mmsweapons.client.chainlink.ThrownChainRenderer;
import info.mudbourn.mmsweapons.client.focus.BarrageClient;
import info.mudbourn.mmsweapons.client.focus.FocusHoldOffClient;
import info.mudbourn.mmsweapons.client.spellengine.SpellEngineBarrageClient;
import info.mudbourn.mmsweapons.client.material.CompatShieldClient;
import info.mudbourn.mmsweapons.client.shieldbash.ShieldRamClient;
import info.mudbourn.mmsweapons.client.throwable.ThrownSickleRenderer;
import info.mudbourn.mmsweapons.client.throwable.ThrownWeaponRenderer;
import info.mudbourn.mmsweapons.throwable.MmsThrowables;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MmsWeaponsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Thrown weapons draw their own item model, so there is no model layer to
        // bake — just the renderer, which has to be present wherever the entity
        // type is, i.e. always.
        EntityRendererRegistry.register(
                MmsThrowables.THROWN_WEAPON,
                ThrownWeaponRenderer::new);
        EntityRendererRegistry.register(
                MmsThrowables.THROWN_SICKLE,
                ThrownSickleRenderer::new);
        EntityRendererRegistry.register(
                Chainlink.THROWN_CHAIN,
                ThrownChainRenderer::new);

        BarrageClient.register();
        info.mudbourn.mmsweapons.client.focus.WhirlwindClient.register();
        info.mudbourn.mmsweapons.client.focus.FocusAuraClient.register();
        FocusHoldOffClient.register();
        ShieldRamClient.register();
        if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
            SpellEngineBarrageClient.register();
            info.mudbourn.mmsweapons.client.spellengine.DownedCastingClient.register();
            info.mudbourn.mmsweapons.client.spellengine.SpellBindsClient.register();
        }

        // Arsonist class sets wear their netherite set's 3D model with an arsonist texture.
        for (info.mudbourn.mmsweapons.material.WizardsGear.Worn worn : info.mudbourn.mmsweapons.material.WizardsGear.WORN) {
            net.rpg_foundation.armor_api.client.ArmorRenderers.register(
                net.rpg_foundation.armor_api.client.GeoArmorRenderer.of(worn.geo(), worn.texture()),
                worn.pieces().toArray(new net.minecraft.world.level.ItemLike[0]));
        }

        if (FabricLoader.getInstance().isModLoaded("aerialhell")
                && FabricLoader.getInstance().isModLoaded("expanded_weaponry")) {
            CompatShieldClient.register();
        }

        // 3D models for other mods' weapons, shipped as a built-in pack that is on by default.
        FabricLoader.getInstance().getModContainer("mms_weapons").ifPresent(mod -> ResourceLoader.registerBuiltinPack(
                Identifier.fromNamespaceAndPath("mms_weapons", "weapon_models"),
                mod,
                Component.literal("MMS 3D Weapon Models"),
                PackActivationType.DEFAULT_ENABLED));
    }
}
