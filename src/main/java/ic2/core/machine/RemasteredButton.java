package ic2.core.machine;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Native button interaction/narration with the machine's own industrial skin. */
class RemasteredButton extends Button {
    private final BooleanSupplier selected;

    RemasteredButton(int x, int y, int width, int height, Component label, OnPress action, BooleanSupplier selected) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.selected = selected;
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        int x = getX();
        int y = getY();
        boolean chosen = selected.getAsBoolean();
        int edge = chosen ? RemasteredGui.ORANGE : isHoveredOrFocused() && active ? 0xffa5b1be : 0xff67717d;
        graphics.fill(x, y, x + width, y + height, 0xff10151b);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, edge);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2,
            isHoveredOrFocused() && active ? 0xff454e59 : 0xff303842);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xff515b67);
        if (chosen) graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 1, RemasteredGui.ORANGE);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width / 2,
            y + (height - 8) / 2, !active ? 0xff75808c : chosen ? RemasteredGui.ORANGE : RemasteredGui.TEXT);
    }
}
