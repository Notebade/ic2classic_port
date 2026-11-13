package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class ColossalScreen extends AbstractContainerScreen<ColossalMenu> {
    ColossalScreen(ColossalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = menu.lanes == 8 ? 199 : 184; inventoryLabelY = imageHeight - 94;
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) { event.register(ColossalMachines.MENU.get(), ColossalScreen::new); }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) { super.render(graphics, x, y, partial); renderTooltip(graphics, x, y); }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        String size = menu.lanes == 2 ? "small" : menu.lanes == 4 ? "medium" : "large";
        var texture = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui_sprites/blocks/machines/ev/" + menu.kind.id() + "/gui_" + size + ".png");
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int offset = menu.lanes == 8 ? 15 : 0;
        int water = menu.value(3) * 58 / 16000, steam = menu.value(4) * 58 / 16000;
        graphics.fill(leftPos + 9, topPos + 77 + offset - water, leftPos + 25, topPos + 77 + offset, 0xff3366cc);
        graphics.fill(leftPos + 152, topPos + 77 + offset - steam, leftPos + 168, topPos + 77 + offset, 0xffdddddd);
        for (int lane = 0; lane < menu.lanes; lane++) {
            int inputX = ColossalMenu.inputX(menu.lanes, lane), inputY = ColossalMenu.inputY(menu.lanes, lane);
            int width = Math.clamp(menu.value(6 + lane), 0, 6000) * 24 / 6000;
            graphics.blit(texture, leftPos + inputX + (menu.lanes == 2 ? 22 : 17), topPos + inputY, 176, 14, width, 16);
        }
        for (int index = menu.lanes * 2; index < menu.lanes * 2 + 3; index++) {
            var slot = menu.slots.get(index);
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
    }
}
