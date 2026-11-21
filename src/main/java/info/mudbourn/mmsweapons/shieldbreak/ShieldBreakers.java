package info.mudbourn.mmsweapons.shieldbreak;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Weapon;

// Heavy weapons that disable a shield they hit, like an axe.
public final class ShieldBreakers {

    public static final TagKey<Item> TAG =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mms_weapons", "shield_breakers"));

    public static final float SECONDS = Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS;

    private ShieldBreakers() {
    }
}
