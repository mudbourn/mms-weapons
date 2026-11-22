package info.mudbourn.mmsweapons.spellengine;

import info.mudbourn.mmsweapons.downed.Downed;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.spell_engine.api.spell.event.SpellEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// A healing spell that lands on a downed player starts a revive that takes half as long as AbsolutRevive's crouch rescue and brings them back with more health.
public final class SpellRevive {

    // AbsolutRevive's crouch rescue takes 160 ticks and leaves 8 health.
    private static final int REVIVE_TICKS = 80;
    private static final float REVIVE_HEALTH = 14.0F;
    private static final double MAX_DISTANCE_SQR = 16.0 * 16.0;

    private record Revive(UUID caster, long startTick) {
    }

    private static final Map<UUID, Revive> REVIVES = new HashMap<>();

    private SpellRevive() {
    }

    public static void register() {
        SpellEvents.HEAL.register(args -> {
            if (args.caster() instanceof ServerPlayer caster && args.target() instanceof ServerPlayer target
                    && caster != target && !Downed.is(caster) && Downed.is(target)) {
                REVIVES.putIfAbsent(target.getUUID(), new Revive(caster.getUUID(), target.level().getGameTime()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(SpellRevive::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> REVIVES.clear());
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Revive>> it = REVIVES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Revive> entry = it.next();
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getValue().caster());
            if (target == null || !Downed.is(target) || caster == null || !caster.isAlive() || Downed.is(caster)
                    || caster.level() != target.level() || caster.distanceToSqr(target) > MAX_DISTANCE_SQR) {
                it.remove();
                continue;
            }
            long elapsed = target.level().getGameTime() - entry.getValue().startTick();
            if (elapsed < REVIVE_TICKS) {
                int percent = (int) (elapsed * 100 / REVIVE_TICKS);
                caster.sendOverlayMessage(Component.translatable("mms_weapons.spell_revive.progress", target.getDisplayName(), percent).withStyle(ChatFormatting.GREEN));
                continue;
            }
            it.remove();
            if (Downed.revive(target)) {
                target.setHealth(Math.min(REVIVE_HEALTH, target.getMaxHealth()));
                caster.sendOverlayMessage(Component.translatable("mms_weapons.spell_revive.done", target.getDisplayName()).withStyle(ChatFormatting.GREEN));
                target.sendOverlayMessage(Component.translatable("mms_weapons.spell_revive.received", caster.getDisplayName()).withStyle(ChatFormatting.GREEN));
            }
        }
    }
}
