package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class MonitorScreen extends AbstractContainerScreen<MonitorMenu> {
    private Button redstone;

    MonitorScreen(MonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageHeight = 185;
        inventoryLabelY = 91;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(NuclearInfrastructure.MONITOR_MENU.get(), MonitorScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(Component.empty(), (b)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 8, topPos + 74, 160, 14).build());
        updateButton();
    }

    private void updateButton() {
        redstone.setMessage(Component.translatable("gui.ic2.monitor.redstone_" + (menu.redstone() ? "on" : "off")));
    }

    @Override
    protected void containerTick() {
        updateButton();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float tick) {
        super.render(g, x, y, tick);
        renderTooltip(g, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics g, float tick, int x, int y) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, -3750202);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, -11184811);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, -7631989);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        super.renderLabels(g, x, y);
        g.drawString(font, menu.energy() + " / 1000 EU", 8, 21, 4210752, false);
    }
}
