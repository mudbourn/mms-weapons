package info.mudbourn.mmsweapons.client.mixin.combatamenities;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Frees a key: crouch + the belt slot key holsters to the back slot, so the back slot needs no bind of its own.
// Combat Amenities reads both keys each client tick, back first, then belt; while crouch is held the belt key
// counts as the back key instead. Its own back key still works if bound.
@Pseudo
@Mixin(targets = "net.hollowed.combatamenities.networking.KeybindEventHandler", remap = false)
public abstract class CombatAmenitiesKeysMixin {

    @Redirect(method = "lambda$register$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z", ordinal = 0))
    private static boolean mms$backSlotDown(KeyMapping back) {
        Minecraft mc = Minecraft.getInstance();
        return back.isDown() || mc.options.keyShift.isDown() && beltKey().isDown();
    }

    @Redirect(method = "lambda$register$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z", ordinal = 1))
    private static boolean mms$beltSlotDown(KeyMapping belt) {
        return belt.isDown() && !Minecraft.getInstance().options.keyShift.isDown();
    }

    private static KeyMapping beltKey() {
        try {
            return (KeyMapping) Class.forName("net.hollowed.combatamenities.index.CAKeyBindings").getField("beltSlotBinding").get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
