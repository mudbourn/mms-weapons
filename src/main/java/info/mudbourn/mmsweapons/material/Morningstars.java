package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

// Paladins maces and Weapons Expanded morningstars as one Morningstar line.
public final class Morningstars {

    private static final List<String> MACE_TIERS = List.of("iron", "golden", "diamond", "netherite", "ruby", "aether", "aeternium");
    private static final Set<String> FLAT_MODELS = Set.of("wooden", "stone", "copper");

    private Morningstars() {
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (String tier : MACE_TIERS) {
                Identifier mace = Identifier.fromNamespaceAndPath("paladins", tier + "_mace");
                context.modify(item -> BuiltInRegistries.ITEM.getKey(item).equals(mace), (builder, item) ->
                    builder.set(DataComponents.ITEM_NAME, Component.translatable("item.mms_weapons.paladins_" + tier + "_morningstar")));
            }
            for (String tier : FLAT_MODELS) {
                Identifier star = Identifier.fromNamespaceAndPath("weaponsexpanded", tier + "_morningstar");
                context.modify(item -> BuiltInRegistries.ITEM.getKey(item).equals(star), (builder, item) ->
                    builder.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("mms_weapons", tier + "_morningstar")));
            }
            Identifier netherite = Identifier.fromNamespaceAndPath("weaponsexpanded", "netherite_morningstar");
            context.modify(item -> BuiltInRegistries.ITEM.getKey(item).equals(netherite), (builder, item) ->
                builder.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("mms_weapons", "netherite_morningstar")));
        });
    }
}
