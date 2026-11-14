package ic2.core.equipment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class ArmorConfiguratorScreen extends AbstractContainerScreen<ArmorConfiguratorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("ic2:textures/gui_sprites/blocks/machines/stone/gui_armor_configurator.png");
    private boolean scrolling;
    ArmorConfiguratorScreen(ArmorConfiguratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 194;
        imageHeight = 222;
        inventoryLabelY = 129;
    }
    @SubscribeEvent static void register(RegisterMenuScreensEvent event) {
        event.register(ArmorConfiguratorStorage.MENU.get(), ArmorConfiguratorScreen::new);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int row = 0; row < 3; row++) for (int column = 0; column < menu.rowSize(row); column++)
            graphics.blit(TEXTURE, leftPos + 7 + column * 18, topPos + 28 + row * 30, 17, 238, 18, 18);
        int slider = menu.maxOffset() == 0 ? 0 : Math.round(71F * menu.offset() / menu.maxOffset());
        graphics.blit(TEXTURE, leftPos + 172, topPos + 21 + slider, 0, 241, 12, 15);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        for (int row = 0; row < 3; row++) {
            var category = menu.category(row);
            if (category == null) continue;
            String key = category.getPath().equals("charge") ? "charger" : category.getPath();
            graphics.drawString(font, Component.translatable("misc." + category.getNamespace() + ".module." + key), 7, 19 + row * 30, 4210752, false);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
    private void scrollTo(int value) {
        int offset = net.minecraft.util.Mth.clamp(value, 0, menu.maxOffset());
        if (offset == menu.offset()) return;
        menu.setData(0, offset);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, offset);
    }
    private void drag(double y) { scrollTo((int) Math.round((y - topPos - 28.5) / 71 * menu.maxOffset())); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && menu.maxOffset() > 0 && x >= leftPos + 172 && x < leftPos + 184 && y >= topPos + 21 && y < topPos + 107) {
            scrolling = true; drag(y); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (scrolling) { drag(y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        scrolling = false; return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (isHovering(7, 19, 177, 88, x, y)) { scrollTo(menu.offset() - (int) Math.signum(vertical)); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
}
