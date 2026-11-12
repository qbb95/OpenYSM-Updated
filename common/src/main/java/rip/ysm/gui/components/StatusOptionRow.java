package rip.ysm.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import rip.ysm.gui.Option;
import rip.ysm.gui.OptionRow;

/**
 * Read-only row that reports a state which is not configurable in game (for
 * example "this feature is on because mod X is installed"). It never changes
 * its value and never marks itself dirty, so it can live inside an OptionGroup
 * without affecting apply/undo.
 */
public class StatusOptionRow extends OptionRow<Boolean> {
    private final int onColor;
    private final int offColor;

    public StatusOptionRow(int x, int y, int width, int height, Option<Boolean> option) {
        this(x, y, width, height, option, 0xFF7CD67C, 0xFF8A8A8A);
    }

    public StatusOptionRow(int x, int y, int width, int height, Option<Boolean> option, int onColor, int offColor) {
        super(x, y, width, height, option);
        this.onColor = onColor;
        this.offColor = offColor;
    }

    @Override
    protected int controlWidth() {
        return Mth.clamp(width / 2, 100, 220);
    }

    @Override
    protected void renderControl(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean on = Boolean.TRUE.equals(option.get());
        Component text = on
                ? Component.translatable("gui.yes_steve_model.config.status.enabled")
                : Component.translatable("gui.yes_steve_model.config.status.disabled");
        int tw = Minecraft.getInstance().font.width(text);
        g.drawString(Minecraft.getInstance().font, text, controlX() + controlWidth() - tw,
                controlY() + (controlHeight() - 8) / 2, on ? onColor : offColor, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        // Read-only: the value mirrors an external condition.
    }
}
