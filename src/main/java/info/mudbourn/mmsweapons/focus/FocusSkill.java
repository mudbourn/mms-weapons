package info.mudbourn.mmsweapons.focus;

// Tracks the local player's executing focus attack, such as the barrage, so swinging and jumping are held off until it ends.
public final class FocusSkill {

    // Client side: ticks left in the local player's released focus attack.
    private static int executingTicks;

    private FocusSkill() {
    }

    // Marks the local player's focus attack as executing for this many ticks.
    public static void execute(int ticks) {
        executingTicks = Math.max(executingTicks, ticks);
    }

    // Whether the local player is still executing a focus attack, so jumping and swinging are held off.
    public static boolean holdsOff() {
        return executingTicks > 0;
    }

    // Whether the local player's focus attack is still executing, counting one tick down per call.
    public static boolean tickExecuting() {
        if (executingTicks <= 0) {
            return false;
        }
        executingTicks--;
        return true;
    }
}
