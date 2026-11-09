package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {
    ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = menu.isSteam() ? 250 : 238;
        inventoryLabelY = menu.isSteam() ? 157 : 145;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF333333);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, slot.isActive() ? 0xFF8B8B8B : 0xFFAAAAAA);
        }
        int heatBar = (int) (Math.min(1.0, (double) menu.heat() / menu.maxHeat()) * 160);
        graphics.fill(leftPos + 8, topPos + 30, leftPos + 168, topPos + 34, 0xFF333333);
        graphics.fill(leftPos + 8, topPos + 30, leftPos + 8 + heatBar, topPos + 34, 0xFFC34432);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable(menu.isSteam() ? "gui.ic2.steam_reactor_status" : "gui.ic2.reactor_status", menu.output(), menu.heat(),
            menu.maxHeat()), 8, 19, 0x404040, false);
        if (menu.isSteam()) {
            graphics.drawString(font, Component.translatable("gui.ic2.steam_reactor_tanks", menu.water(), menu.steam()),
                8, 147, 0x404040, false);
        }
    }
}
