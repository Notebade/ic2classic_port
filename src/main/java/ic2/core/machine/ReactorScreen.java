package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {
    ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 282;
        inventoryLabelX = 60;
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
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 8, topPos + 25, leftPos + 105, topPos + 142, 0xff252d37);
        graphics.fill(leftPos + 108, topPos + 25, leftPos + 276, topPos + 142, 0xffc5c1b2);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (slot.isActive()) RemasteredGui.slot(graphics, x, y);
            else graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff606571);
        }
        metricBar(graphics, 61, menu.heat(), menu.maxHeat(), RemasteredGui.ORANGE);
        if (menu.isSteam()) {
            metricBar(graphics, 92, menu.water(), SteamReactorTanks.WATER_CAPACITY, 0xff439fdf);
            metricBar(graphics, 123, menu.steam(), SteamReactorTanks.STEAM_CAPACITY, 0xff53d9e8);
        }
    }

    private void metricBar(GuiGraphics graphics, int y, int value, int capacity, int color) {
        RemasteredGui.bar(graphics, leftPos + 13, topPos + y, 86, 4, value, capacity, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        metric(graphics, "gui.ic2.reactor_heat_label", menu.heat() + " / " + menu.maxHeat(), 35, RemasteredGui.TEXT);
        if (menu.isSteam()) {
            metric(graphics, "gui.ic2.reactor_water_label",
                menu.water() + " / " + SteamReactorTanks.WATER_CAPACITY, 66, RemasteredGui.TEXT);
            metric(graphics, "gui.ic2.reactor_steam_label",
                menu.steam() + " / " + SteamReactorTanks.STEAM_CAPACITY, 97, RemasteredGui.TEXT);
        } else {
            metric(graphics, "gui.ic2.reactor_output_label",
                Component.translatable("gui.ic2.turbine_output", menu.output()).getString(), 76, RemasteredGui.GREEN);
        }
    }

    private void metric(GuiGraphics graphics, String key, String value, int y, int color) {
        graphics.drawString(font, Component.translatable(key), 13, y, RemasteredGui.TEXT, false);
        graphics.drawString(font, value, 13, y + 11, color, false);
    }
}
