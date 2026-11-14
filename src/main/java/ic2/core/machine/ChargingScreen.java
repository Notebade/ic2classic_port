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
    ChargingScreen(ChargingMenu menu, Inventory player, Component title) {
        super(menu, player, title);
        if (menu.styled()) imageWidth = 232;
        imageHeight = menu.kind.pad() ? 169 : 242;
        inventoryLabelY = menu.kind.pad() ? 76 : 148;
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        for (var type : ChargingDevices.MENU.values()) event.register(type.get(), ChargingScreen::new);
    }
    @Override protected void init() {
        super.init();
        if (menu.kind == ChargingDevices.Kind.MASTER) {
            for (int side = 0; side < 6; side++) { button(side * 2, "−", 120, 36 + side * 16, 20); button(side * 2 + 1, "+", 146, 36 + side * 16, 20); }
            button(12, "↔", 113, 134, 56);
        } else if (menu.kind.family == ChargingDevices.Family.BENCH) {
            button(0, Component.translatable("gui.ic2.charging_sort").getString(), 8, 122, 45);
            button(1, "⇄", 57, 122, 32); button(2, "←", 93, 122, 32); button(3, "→", 129, 122, 32);
        }
    }
    private void button(int id, String text, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal(text), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
            .bounds(leftPos + x, topPos + y, width, 16)
            .tooltip(menu.kind == ChargingDevices.Kind.MASTER && id == 12
                ? net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.ic2.charging_auto")) : null).build());
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick);
        renderTooltip(graphics, x, y);
        if (menu.kind.pad() && isHovering(8, 43, 98, 30, x, y)) {
            graphics.renderTooltip(font, Component.translatable("gui.ic2.charging_pad"), x, y);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
        if (menu.styled()) {
            int bottom = menu.kind.pad() ? 75 : 116;
            RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
            graphics.fill(leftPos + 7, topPos + 18, leftPos + 108, topPos + bottom, 0xff252d37);
            graphics.fill(leftPos + 112, topPos + 18, leftPos + 225, topPos + bottom, 0xff737e8b);
            RemasteredGui.lightning(graphics, leftPos + 10, topPos + 22);
            RemasteredGui.bar(graphics, leftPos + 11, topPos + 36, 93, 4, menu.value(0), menu.value(1), RemasteredGui.GREEN);
            for (var slot : menu.slots) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            return;
        }
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        boolean master = menu.kind == ChargingDevices.Kind.MASTER;
        graphics.fill(leftPos + 7, topPos + 18, leftPos + (master ? 108 : 169), topPos + 145, 0xff252d37);
        if (master) {
            graphics.fill(leftPos + 112, topPos + 18, leftPos + 169, topPos + 132, 0xff737e8b);
            for (int side = 0; side < 6; side++) {
                RemasteredGui.bar(graphics, leftPos + 11, topPos + 49 + side * 16, 92, 2,
                    menu.value(4 + side), 100, 0xff53d9e8);
            }
        }
        for (var slot : menu.slots) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        if (menu.styled()) {
            var energy = Component.translatable("gui.ic2.storage_energy", menu.value(0), menu.value(1));
            graphics.pose().pushPose();
            graphics.pose().translate(23, 22, 0);
            float scale = Math.min(1F, 81F / Math.max(1, font.width(energy)));
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, energy, 0, 0, RemasteredGui.TEXT, false);
            graphics.pose().popPose();
            String key = menu.kind.pad() ? "pad" : menu.kind.family == ChargingDevices.Family.BENCH ? "bench" : "station";
            if (menu.kind.family == ChargingDevices.Family.BENCH && menu.value(2) == 1) {
                for (int row = 0; row < 4; row++) {
                    graphics.drawString(font, Component.translatable("gui.ic2.charging_armor_" + row),
                        11, 47 + row * 18, RemasteredGui.TEXT, false);
                }
            } else {
                Component hint = Component.translatable(menu.kind.pad()
                    ? "gui.ic2.charging_pad_hint" : "gui.ic2.charging_" + key);
                graphics.drawWordWrap(font, hint, 11, 45, 93, RemasteredGui.TEXT);
            }
            graphics.drawString(font, playerInventoryTitle, 36, inventoryLabelY, 0x404040, false);
            return;
        }
        if (!menu.kind.pad()) graphics.drawString(font, menu.value(0) + " / " + menu.value(1) + " EU", 11, 22, RemasteredGui.TEXT, false);
        if (menu.kind == ChargingDevices.Kind.MASTER) {
            for (Direction side : Direction.values()) {
                int i = side.ordinal(); boolean present = (menu.value(3) & 1 << i) != 0;
                graphics.drawString(font, Component.translatable("gui.ic2.charging_" + side.getName()).append(" " + menu.value(4 + i) + "%"), 11, 38 + i * 16, present ? 0xff53d9e8 : RemasteredGui.MUTED, false);
            }
        } else if (!menu.kind.pad()) {
            String key = switch (menu.kind.family) { case STATION -> "station"; case PAD -> "pad"; case SLAVE -> "slave"; default -> "bench"; };
            graphics.drawWordWrap(font, Component.translatable("gui.ic2.charging_" + key), 11, 45, 154, RemasteredGui.TEXT);
        }
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
