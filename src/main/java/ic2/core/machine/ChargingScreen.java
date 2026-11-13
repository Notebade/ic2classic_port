package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class ChargingScreen extends AbstractContainerScreen<ChargingMenu> {
    ChargingScreen(ChargingMenu menu, Inventory player, Component title) { super(menu, player, title); imageHeight = 242; inventoryLabelY = 148; }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        for (var type : ChargingDevices.MENU.values()) event.register(type.get(), ChargingScreen::new);
    }
    @Override protected void init() {
        super.init();
        if (menu.kind == ChargingDevices.Kind.MASTER) {
            for (int side = 0; side < 6; side++) { button(side * 2, "−", 118, 36 + side * 16, 20); button(side * 2 + 1, "+", 142, 36 + side * 16, 20); }
            button(12, Component.translatable("gui.ic2.charging_auto").getString(), 8, 136, 154);
        } else if (menu.kind.family == ChargingDevices.Family.BENCH) {
            button(0, Component.translatable("gui.ic2.charging_sort").getString(), 8, 122, 45);
            button(1, "⇄", 57, 122, 32); button(2, "←", 93, 122, 32); button(3, "→", 129, 122, 32);
        }
    }
    private void button(int id, String text, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal(text), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
            .bounds(leftPos + x, topPos + y, width, 16).build());
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float tick) { super.render(graphics, x, y, tick); renderTooltip(graphics, x, y); }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
        if (menu.kind.family == ChargingDevices.Family.BENCH && menu.value(2) == 1) {
            for (int row = 0; row < 4; row++) graphics.drawString(font, Component.translatable("gui.ic2.charging_armor_" + row), leftPos + 8, topPos + 47 + row * 18, 0x404040, false);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, menu.value(0) + " / " + menu.value(1) + " EU", 8, 22, 0x404040, false);
        if (menu.kind == ChargingDevices.Kind.MASTER) {
            for (Direction side : Direction.values()) {
                int i = side.ordinal(); boolean present = (menu.value(3) & 1 << i) != 0;
                graphics.drawString(font, Component.translatable("gui.ic2.charging_" + side.getName()).append(" " + menu.value(4 + i) + "%"), 8, 40 + i * 16, present ? 0x205020 : 0x666666, false);
            }
        } else {
            String key = switch (menu.kind.family) { case STATION -> "station"; case PAD -> "pad"; case SLAVE -> "slave"; default -> "bench"; };
            graphics.drawWordWrap(font, Component.translatable("gui.ic2.charging_" + key), 8, menu.kind.inventoryGrid() ? 34 : 45, 160, 0x404040);
        }
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
