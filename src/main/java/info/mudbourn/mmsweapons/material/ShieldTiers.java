package info.mudbourn.mmsweapons.material;

import info.mudbourn.mmsweapons.aerial.AerialGear;

import java.util.Map;

// Each pack material's shields are matched to the vanilla-material shield of the same tier. Blocking comes from ShieldGuards.
final class ShieldTiers {

    // round: durability and seconds disabled by an axe; kite: durability, toughness and max health while held.
    enum Tier {
        WOOD(140, 8.0F, 168, 0.0F, 0.0F),
        STONE(230, 7.0F, 168, 0.0F, 0.0F),
        IRON(380, 5.0F, 336, 1.0F, 2.0F),
        DIAMOND(520, 4.0F, 672, 1.0F, 2.0F),
        NETHERITE(800, 3.0F, 1344, 1.0F, 4.0F);

        final int roundDurability;
        final float disabledSeconds;
        final int kiteDurability;
        final float kiteToughness;
        final float kiteHealth;

        Tier(int roundDurability, float disabledSeconds, int kiteDurability, float kiteToughness, float kiteHealth) {
            this.roundDurability = roundDurability;
            this.disabledSeconds = disabledSeconds;
            this.kiteDurability = kiteDurability;
            this.kiteToughness = kiteToughness;
            this.kiteHealth = kiteHealth;
        }
    }

    // Matched on each material's mining tier.
    private static final Map<String, Tier> TIERS = Map.ofEntries(
        Map.entry("sky_wood", Tier.WOOD),
        Map.entry("magmatic_gel", Tier.WOOD),
        Map.entry("stellar_stone", Tier.STONE),
        Map.entry("azurite", Tier.STONE),
        Map.entry("ruby", Tier.IRON),
        Map.entry("shadoline", Tier.IRON),
        Map.entry("obsidian", Tier.DIAMOND),
        Map.entry("shadow", Tier.DIAMOND),
        Map.entry("arsonist", Tier.NETHERITE),
        Map.entry("lunatic", Tier.NETHERITE),
        Map.entry("volucite", Tier.NETHERITE)
    );

    private ShieldTiers() {
    }

    static Tier of(String material) {
        return TIERS.getOrDefault(material, Tier.IRON);
    }

    // 1.5 for Aerial Hell materials, 1 for the rest (shadoline is Enderscape's).
    static float boost(String material) {
        return material.equals("shadoline") ? 1.0F : AerialGear.STAT_MULTIPLIER;
    }

    static float disabledSeconds(String material) {
        return of(material).disabledSeconds / boost(material);
    }

    static float kiteToughness(String material) {
        return of(material).kiteToughness * boost(material);
    }

    static float kiteHealth(String material) {
        return of(material).kiteHealth * boost(material);
    }
}
