package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
final class ExtractionScreen extends AbstractContainerScreen<ExtractionMenu> {
    ExtractionScreen(ExtractionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); inventoryLabelY = 73;
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ExtractionMachines.MENU.get(), ExtractionScreen::new);
        event.register(ExtractionMachines.MINER_MENU.get(), ExtractionScreen::new);
        event.register(ExtractionMachines.RANGED_MENU.get(), ExtractionScreen::new);
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
        RemasteredGui.container(graphics, leftPos, topPos, imageWidth, imageHeight, menu.slots, inventoryLabelY);
        RemasteredGui.bar(graphics, leftPos + 10, topPos + 29, 156, 3, menu.value(0), menu.value(3), RemasteredGui.GREEN);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        super.renderLabels(graphics, x, y);
        graphics.drawString(font, menu.value(0) + " / " + menu.value(3) + " EU", 10, 20, RemasteredGui.TEXT, false);
        boolean miner = menu.getType() == ExtractionMachines.MINER_MENU.get();
        Component status = Component.translatable(miner ? "gui.ic2.remaining.miner" : "gui.ic2.remaining.fluid",
            miner ? menu.value(4) : menu.value(2), miner ? menu.value(5) : menu.value(1));
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), 156), 10, 59, RemasteredGui.TEXT, false);
        if (miner && menu.value(2) != 0) graphics.drawString(font, "!", 164, 59, RemasteredGui.ORANGE, false);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick); renderTooltip(graphics, x, y);
    }
}
