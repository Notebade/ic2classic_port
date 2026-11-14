package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
final class StorageScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    StorageScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        int firstPlayerSlot = menu.slots.size() - 36;
        inventoryLabelY = menu.slots.get(firstPlayerSlot).y - 11;
        imageHeight = menu.slots.get(menu.slots.size() - 1).y + 24;
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(StorageInfrastructure.BUFFER_MENU.get(), StorageScreen<net.minecraft.world.inventory.ChestMenu>::new);
        event.register(StorageInfrastructure.PERSONAL_MENU.get(), StorageScreen<net.minecraft.world.inventory.ChestMenu>::new);
        event.register(StorageInfrastructure.TOOLBOX_MENU.get(), StorageScreen<ToolboxMenu>::new);
        event.register(StorageInfrastructure.CARBON_MENU.get(), StorageScreen<ToolboxMenu>::new);
        event.register(StorageInfrastructure.IRIDIUM_MENU.get(), StorageScreen<ToolboxMenu>::new);
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
        RemasteredGui.container(graphics, leftPos, topPos, imageWidth, imageHeight, menu.slots, inventoryLabelY);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick); renderTooltip(graphics, x, y);
    }
}
