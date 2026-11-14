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
final class ChunkloaderScreen extends AbstractContainerScreen<ChunkloaderMenu> {
    private int radius;
    private boolean processing, initialized;
    private Button processingButton;
    private Button modeButton;
    ChunkloaderScreen(ChunkloaderMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 188; imageHeight = 115; }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) { event.register(Chunkloading.MENU.get(), ChunkloaderScreen::new); }
    @Override protected void init() {
        super.init(); initialized = false;
        addRenderableWidget(Button.builder(Component.literal("−"), button -> radius = Math.max(0, radius - 1)).bounds(leftPos + 28, topPos + 40, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> radius = Math.min(3, radius + 1)).bounds(leftPos + 75, topPos + 40, 16, 16).build());
        processingButton = addRenderableWidget(Button.builder(Component.empty(), button -> { processing = !processing; updateButton(); }).bounds(leftPos + 5, topPos + 60, 110, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ic2.chunkloader.confirm"), button -> send(16 + radius + (processing ? 4 : 0))).bounds(leftPos + 5, topPos + 94, 110, 14).build());
        modeButton = addRenderableWidget(Button.builder(Component.literal("M"), button -> send(3)).bounds(leftPos + 163, topPos + 5, 16, 16).build());
    }
    private void send(int id) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    private void updateButton() { processingButton.setMessage(Component.translatable("gui.ic2.chunkloader.processing_" + (processing ? "on" : "off"))); }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        if (!initialized) { initialized = true; radius = menu.value(1); processing = menu.value(2) != 0; updateButton(); }
        modeButton.setMessage(Component.literal(switch (menu.value(3)) { case 0 -> "P"; case 1 -> "T"; default -> "O"; }));
        modeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.ic2.chunkloader.mode_" + menu.value(3))));
        super.render(graphics, x, y, partial); renderTooltip(graphics, x, y);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + 116, topPos + 37, 0xff252d37);
        RemasteredGui.lightning(graphics, leftPos + 9, topPos + 21);
        RemasteredGui.bar(graphics, leftPos + 24, topPos + 30, 85, 4, menu.value(0), 60000, RemasteredGui.GREEN);
        graphics.fill(leftPos + 122, topPos + 27, leftPos + 181, topPos + 101, 0xff252d37);
        for (int row = -3; row <= 3; row++) for (int col = -3; col <= 3; col++) {
            int px = leftPos + 127 + (col + 3) * 7;
            int py = topPos + 39 + (row + 3) * 7;
            boolean selected = Math.max(Math.abs(row), Math.abs(col)) <= radius;
            graphics.fill(px, py, px + 6, py + 6, row == 0 && col == 0 ? RemasteredGui.GREEN
                : selected ? 0xff476f41 : 0xff39434f);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 8, 7, 0x404040, false);
        graphics.drawString(font, menu.value(0) + " EU", 24, 20, RemasteredGui.TEXT, false);
        graphics.drawString(font, Integer.toString(radius), 57, 44, 0x404040, false);
        graphics.drawString(font, ChunkloaderRules.cost(radius, processing) + " EU/t", 5, 81, 0x404040, false);
    }
}
