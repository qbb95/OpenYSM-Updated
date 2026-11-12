package rip.ysm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class OptionRow<T> extends AbstractWidget {
    protected final Option<T> option;

    /**
     * Rows that depend on another option can be greyed out. A disabled row keeps
     * rendering (so the label and value stay readable) but ignores all input and
     * paints its control darker.
     */
    private boolean rowEnabled = true;

    protected OptionRow(int x, int y, int width, int height, Option<T> option) {
        super(x, y, width, height, option == null ? Component.empty() : option.getLabel());
        this.option = option;
    }

    public Option<T> getOption() {
        return option;
    }

    public boolean isRowEnabled() {
        return rowEnabled;
    }

    public void setRowEnabled(boolean enabled) {
        if (this.rowEnabled == enabled) return;
        this.rowEnabled = enabled;
        if (!enabled) closeOverlay();
    }

    public void refresh() {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return rowEnabled && super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return rowEnabled && super.mouseDragged(event, dx, dy);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean dirty = option != null && option.isDirty();
        boolean disabled = !rowEnabled;
        int bg = disabled ? 0x90000000 : (isHovered() ? 0x90171717 : (dirty ? 0x90060606 : 0x90000000));
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);

        Component label = getMessage();
        int textColor = disabled ? 0xFF6B6B6B : (dirty ? -1 : 0x90FFFFFF);
        int textY = getY() + (height - 8) / 2;
        g.drawString(Minecraft.getInstance().font, label, getX() + 8, textY, textColor, false);

        renderControl(g, mouseX, mouseY, partialTick);

        if (disabled) {
            g.fill(controlX(), controlY(), controlX() + controlWidth(), controlY() + controlHeight(), 0xA0101010);
        }
    }

    protected abstract void renderControl(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    protected int controlX() {
        return getX() + width - controlWidth() - 6;
    }

    protected int controlY() {
        return getY() + (height - controlHeight()) / 2;
    }

    protected int controlWidth() {
        return 90;
    }

    protected int controlHeight() {
        return Math.min(height - 4, 16);
    }

    protected boolean isMouseOverControl(double mx, double my) {
        if (!rowEnabled) return false;
        int cx = controlX();
        int cy = controlY();
        return mx >= cx && mx < cx + controlWidth() && my >= cy && my < cy + controlHeight();
    }

    public boolean isOverlayOpen() {
        return false;
    }

    public void closeOverlay() {
    }

    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick, float scrollDisplay) {
    }

    public boolean overlayMouseClicked(double mouseX, double mouseY, int button, float scrollDisplay) {
        return false;
    }

    public boolean overlayMouseScrolled(double mouseX, double mouseY, double delta, float scrollDisplay) {
        return false;
    }

    protected static int blendBg(boolean hover, int base) {
        if (!hover) return base;
        int a = (base >>> 24) & 0xFF;
        int r = Mth.clamp(((base >> 16) & 0xFF) + 40, 0, 255);
        int gn = Mth.clamp(((base >> 8) & 0xFF) + 40, 0, 255);
        int b = Mth.clamp((base & 0xFF) + 40, 0, 255);
        return (a << 24) | (r << 16) | (gn << 8) | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
