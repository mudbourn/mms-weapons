package info.mudbourn.mmsweapons.client.mixin.spellengine;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.spell_engine.client.gui.HudRenderHelper;
import net.spell_engine.client.input.SpellHotbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

// Shows a skill on Spell Engine's hotbar HUD only while its key is bound and the slot holds something. Unbinding a spell hotbar key hides that
// slot's icon, and binding it again brings the icon back, with nothing to toggle by hand. The morningstar's chain
// throw also stays hidden until a full shadoline set is worn, and every skill hides while Spell Engine isn't reading
// its keys (see bypassed).
@Mixin(value = HudRenderHelper.class, remap = false)
public abstract class HideWeaponSkillsMixin {

    @Redirect(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FZ)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;", ordinal = 0))
    private static Stream<SpellHotbar.Slot> mms$hideUnboundSkills(List<SpellHotbar.Slot> slots) {
        if (bypassed()) {
            return Stream.empty();
        }
        java.util.Set<Object> shown = new java.util.HashSet<>();
        return slots.stream().filter(HideWeaponSkillsMixin::filled).filter(HideWeaponSkillsMixin::bound)
            .filter(slot -> slot.spell() == null || shown.add(slot.spell().unwrapKey().orElse(null)));
    }

    // Whether Spell Engine is ignoring its hotbar keys right now, so the skills hide as a cue: mms-compat's spell
    // hotbar toggle (Left Alt) is off, Spell Engine's own bypass key is held, or crouch is held with its
    // crouch-bypasses setting on.
    private static boolean bypassed() {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping bypass = net.spell_engine.client.input.Keybindings.bypass_spell_hotbar;
        return !compatToggleOn()
            || bypass != null && bypass.isDown()
            || net.spell_engine.client.SpellEngineClient.config.sneakingByPassSpellHotbar && mc.options.keyShift.isDown();
    }

    private static java.lang.invoke.MethodHandle toggleOn;
    private static boolean toggleLooked;

    // mms-compat's SpellHotbarToggle.on(), read by reflection since it's another mod; on when it isn't installed.
    private static boolean compatToggleOn() {
        if (!toggleLooked) {
            toggleLooked = true;
            try {
                toggleOn = java.lang.invoke.MethodHandles.publicLookup().findStatic(
                    Class.forName("info.mudbourn.mmscompat.client.spellengine.SpellHotbarToggle"), "on",
                    java.lang.invoke.MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        try {
            return toggleOn == null || (boolean) toggleOn.invoke();
        } catch (Throwable e) {
            return true;
        }
    }

    // Whether the slot holds a real spell or a non-blocking item; placeholders and blocking, always on right-click, are left off.
    private static boolean filled(SpellHotbar.Slot slot) {
        if (slot.spell() != null) {
            return !slot.spell().is(info.mudbourn.mmsweapons.spellengine.WeaponSkills.EMPTY);
        }
        var stack = slot.itemStack();
        return stack != null && !stack.isEmpty() && !stack.has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
    }

    private static boolean bound(SpellHotbar.Slot slot) {
        // The shadoline morningstar's chain throw only appears in a full shadoline set.
        var player = Minecraft.getInstance().player;
        if (player != null && slot.spell() != null && slot.spell().is(info.mudbourn.mmsweapons.spellengine.WeaponSkills.CHAIN_THROW)
                && !info.mudbourn.mmsweapons.chainlink.Chainlink.offered(player, player.getMainHandItem())) {
            return false;
        }
        KeyMapping key = slot.getKeyBinding(Minecraft.getInstance().options);
        return key == null || !key.isUnbound();
    }
}
