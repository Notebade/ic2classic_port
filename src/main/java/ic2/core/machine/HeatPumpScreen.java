package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class HeatPumpScreen extends AbstractContainerScreen<HeatPumpMenu> {
    private static final String[] DIRECTIONS = {"up", "down", "left", "right"};
    private final Button[] buttons = new Button[DIRECTIONS.length];

    HeatPumpScreen(HeatPumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 128;
    }

    @Override
    protected void init() {
        super.init();
        for (int index = 0; index < buttons.length; index++) {
            int direction = index;
            buttons[index] = addRenderableWidget(Button.builder(label(index), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction))
                .bounds(leftPos + 8, topPos + 20 + index * 24, imageWidth - 16, 20).build());
        }
    }

    @Override
    protected void containerTick() {
        for (int index = 0; index < buttons.length; index++) {
            buttons[index].setMessage(label(index));
        }
    }

    private Component label(int index) {
        return Component.translatable(menu.pulls(index) ? "gui.ic2.heat_pump.pull" : "gui.ic2.heat_pump.push",
            Component.translatable("gui.ic2.heat_pump." + DIRECTIONS[index]));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 116, 0xff252d37);
    }
}
