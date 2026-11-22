package info.mudbourn.mmsweapons.material;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;

// The heavy battleaxe, halberd, longsword and greatsword turn up where the Heavy Axe and Heavy Sword do: Aerial Hell's mud dungeon
// treasure chests, in an extra roll that finds one of them in a little over one chest in four.
final class HeavyLoot {

    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> MUD_DUNGEON =
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("aerialhell", "chests/mud_dungeon_treasure"));

    private HeavyLoot() {
    }

    static void register() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!key.equals(MUD_DUNGEON) || !source.isBuiltin()) {
                return;
            }
            LootPool.Builder pool = LootPool.lootPool();
            AerialHellHeavy.WEAPONS.forEach(item -> pool.add(LootItem.lootTableItem(item).setWeight(1)));
            pool.add(EmptyLootItem.emptyItem().setWeight(10));
            table.withPool(pool);
        });
    }
}
