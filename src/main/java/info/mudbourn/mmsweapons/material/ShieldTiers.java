package info.mudbourn.mmsweapons.material;

import info.mudbourn.mmsweapons.aerial.AerialGear;

import java.util.Map;

// Each pack material's shields are matched to the vanilla-material shield of the same tier: round shields take
// Expanded Weaponry's blocking values, kite shields take Paladins' durability, toughness and health. Aerial Hell
// shields then get the 1.5x stat boost on top (durability is scaled later by AerialGear with the other gear).
final class ShieldTiers {

    // round: Expanded Weaponry durability, flat damage blocked, share of the rest blocked, seconds disabled by an axe.
    // kite: Paladins durability, toughness and max health while held.
    enum Tier {
        WOOD(140, 1.5F, 0.35F, 8.0F, 168, 0.0F, 0.0F),
        STONE(230, 2.5F, 0.55F, 7.0F, 168, 0.0F, 0.0F),
        IRON(380, 5.0F, 0.85F, 5.0F, 336, 1.0F, 2.0F),
        DIAMOND(520, 6.5F, 0.95F, 4.0F, 672, 1.0F, 2.0F),
        NETHERITE(800, 8.0F, 0.98F, 3.0F, 1344, 1.0F, 4.0F);

        final int roundDurability;
        final float baseBlocked;
        final float blockedShare;
        final float disabledSeconds;
        final int kiteDurability;
        final float kiteToughness;
        final float kiteHealth;

        Tier(int roundDurability, float baseBlocked, float blockedShare, float disabledSeconds, int kiteDurability, float kiteToughness, float kiteHealth) {
            this.roundDurability = roundDurability;
            this.baseBlocked = baseBlocked;
            this.blockedShare = blockedShare;
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

    static float baseBlocked(String material) {
        return of(material).baseBlocked * boost(material);
    }

    // The boost shrinks the share that gets through: netherite's 2% becomes 1.33%.
    static float blockedShare(String material) {
        return 1.0F - (1.0F - of(material).blockedShare) / boost(material);
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
