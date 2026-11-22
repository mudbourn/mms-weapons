package info.mudbourn.mmsweapons.client.spellengine;

import info.mudbourn.mmsweapons.spellengine.SkillLimits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.client.input.WrappedKeybinding;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

import java.util.ArrayList;
import java.util.List;

// Lists the hotbar spells next to their keys; click one row and then another to swap them.
public final class SpellBindsScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int ROW_WIDTH = 220;
    private static final int TOP = 40;

    private record Row(int slot, Component key, SpellCast.Option option) {
    }

    private final List<Row> rows = new ArrayList<>();
    private int selected = -1;
    private final Screen parent;

    public SpellBindsScreen() {
        this(null);
    }

    // Opened from the MMS Settings hub, which passes itself so Done goes back to it.
    public SpellBindsScreen(Screen parent) {
        super(Component.translatable("mms_weapons.spell_binds.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        refresh();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
            .build());
    }

    private void refresh() {
        rows.clear();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<SpellCast.Option> declared = ((SpellCaster.Client) player).getCastController().displayOptions();
        List<SpellCast.Option> laidOut = MouseSlots.arrange(player, this.minecraft.options, declared);
        List<WrappedKeybinding> bindings = Keybindings.Wrapped.all();
        for (int i = 0; i < bindings.size(); i++) {
            WrappedKeybinding.Unwrapped bound = bindings.get(i).get(this.minecraft.options);
            if (bound == null) {
                continue;
            }
            SpellCast.Option option = i < laidOut.size() && !SkillLimits.isEmpty(laidOut.get(i).spell()) ? laidOut.get(i) : null;
            rows.add(new Row(i, bound.keyBinding().getTranslatedKeyMessage(), option));
        }
    }

    private int left() {
        return this.width / 2 - ROW_WIDTH / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("mms_weapons.spell_binds.hint"), this.width / 2, 26, 0xFFAAAAAA);
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = TOP + i * ROW_HEIGHT;
            boolean hovered = mouseX >= left() && mouseX < left() + ROW_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            int background = i == selected ? 0xAA3A7A3A : hovered ? 0x66FFFFFF : 0x44000000;
            graphics.fill(RenderPipelines.GUI, left(), y, left() + ROW_WIDTH, y + ROW_HEIGHT - 2, background);
            graphics.text(this.font, row.key(), left() + 6, y + 6, 0xFFFFFFFF);
            if (row.option() == null) {
                graphics.text(this.font, Component.translatable("mms_weapons.spell_binds.empty"), left() + 70, y + 6, 0xFF777777);
                continue;
            }
            Identifier id = row.option().id();
            graphics.blit(RenderPipelines.GUI_TEXTURED, SpellRender.iconTexture(id), left() + 70, y + 1, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.text(this.font, Component.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".name"), left() + 92, y + 6, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int clicked = (int) Math.floor((event.y() - TOP) / ROW_HEIGHT);
        boolean inside = event.x() >= left() && event.x() < left() + ROW_WIDTH && clicked >= 0 && clicked < rows.size();
        if (!inside) {
            return super.mouseClicked(event, doubleClick);
        }
        if (selected < 0 || selected == clicked) {
            selected = selected == clicked ? -1 : clicked;
        } else {
            swap(rows.get(selected), rows.get(clicked));
            selected = -1;
            refresh();
        }
        return true;
    }

    private static void swap(Row first, Row second) {
        if (first.option() != null) {
            SpellBinds.choose(first.option().id().toString(), second.slot());
        }
        if (second.option() != null) {
            SpellBinds.choose(second.option().id().toString(), first.slot());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
