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
final class ElectricEnchanterScreen extends AbstractContainerScreen<ElectricEnchanterMenu> {
    private RemasteredButton mode, start;
    ElectricEnchanterScreen(ElectricEnchanterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 256; imageHeight = 240;
        inventoryLabelX = 47; inventoryLabelY = 147;
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ElectricEnchanting.MENU.get(), ElectricEnchanterScreen::new);
    }
    private RemasteredButton button(int id, int x, int y, int width, Component label) {
        return addRenderableWidget(new RemasteredButton(leftPos + x, topPos + y, width, 16, label,
            ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id), () -> false));
    }
    @Override protected void init() {
        super.init();
        mode = button(0, 108, 94, 138, Component.empty());
        start = button(7, 108, 74, 64, Component.translatable("gui.ic2.remaining.start"));
        for (int i = 0; i < 3; i++) {
            int amount = i == 0 ? 1 : i == 1 ? 10 : 100;
            button(1 + i, 108 + i * 46, 113, 44, Component.literal("+" + amount + " XP"));
            button(4 + i, 108 + i * 46, 131, 44, Component.literal("-" + amount + " XP"));
        }
    }
    @Override protected void containerTick() {
        super.containerTick();
        mode.setMessage(Component.translatable("gui.ic2.remaining.enchant_mode." + menu.value(1)));
        start.active = menu.value(5) == 0 && menu.value(8) != 0 && menu.value(2) >= menu.value(3) && menu.value(3) > 0;
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
        RemasteredGui.container(graphics, leftPos, topPos, imageWidth, imageHeight, menu.slots, inventoryLabelY);
        RemasteredGui.bar(graphics, leftPos + 12, topPos + 124, 82, 6, menu.value(6), menu.value(7), RemasteredGui.ORANGE);
        RemasteredGui.slot(graphics, leftPos + 224, topPos + 74);
        graphics.renderItem(menu.preview(), leftPos + 224, topPos + 74);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        super.renderLabels(graphics, x, y);
        graphics.drawString(font, menu.value(0) + " EU", 12, 29, RemasteredGui.TEXT, false);
        graphics.drawString(font, menu.value(2) + " / 1000 XP", 12, 47, RemasteredGui.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.ic2.remaining.cost", menu.value(3)), 12, 65, RemasteredGui.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.ic2.remaining.level", menu.value(4)), 12, 83, RemasteredGui.TEXT, false);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick); renderTooltip(graphics, x, y);
        if (!menu.preview().isEmpty() && isHovering(224, 74, 16, 16, x, y)) graphics.renderTooltip(font, menu.preview(), x, y);
    }
}
