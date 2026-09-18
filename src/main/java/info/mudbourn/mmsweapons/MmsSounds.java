package info.mudbourn.mmsweapons;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events owned by this mod.
 *
 * <p>Better Combat resolves a {@code swing_sound} id against the sound-event
 * registry, so a bare resource pack entry isn't enough — the event has to be
 * registered. Registration happens on both sides because the registry is
 * synced to clients on join; the {@code .ogg} files behind it are client-only
 * assets and are ignored server-side.
 */
public final class MmsSounds {

    /**
     * Dry stone impact for the fossilised tuna. Three variants live under
     * {@code assets/mms_weapons/sounds.json}, so the game picks one per swing
     * and repeated hits don't sound identical.
     */
    public static final Identifier STONE_IMPACT_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "stone_impact");

    public static final SoundEvent STONE_IMPACT = SoundEvent.createVariableRangeEvent(STONE_IMPACT_ID);

    /**
     * Per-swing tuna sounds. One event per attack, because Better Combat picks a
     * random variant from an event's pool and has no way to say "this file on
     * this attack" — the swing→sound binding has to live in which event the
     * attack references, not in the pool.
     *
     * <p>These MUST be registered here and not only declared in
     * {@code sounds.json}. A resource pack entry maps an id to files for the
     * client; it does not create the registry entry Better Combat resolves
     * {@code swing_sound} against. Adding the pack entries alone in 0.9.53
     * silenced the tuna outright, because every attack then pointed at an id
     * with no sound event behind it.
     */
    public static final Identifier SWING_LEFT_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "swing_left");
    public static final Identifier SWING_RIGHT_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "swing_right");
    public static final Identifier SLAM_ID =
            Identifier.fromNamespaceAndPath("mms_weapons", "slam");

    public static final SoundEvent SWING_LEFT = SoundEvent.createVariableRangeEvent(SWING_LEFT_ID);
    public static final SoundEvent SWING_RIGHT = SoundEvent.createVariableRangeEvent(SWING_RIGHT_ID);
    public static final SoundEvent SLAM = SoundEvent.createVariableRangeEvent(SLAM_ID);

    private MmsSounds() {}

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, STONE_IMPACT_ID, STONE_IMPACT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SWING_LEFT_ID, SWING_LEFT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SWING_RIGHT_ID, SWING_RIGHT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, SLAM_ID, SLAM);
    }
}
