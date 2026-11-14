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
    private final java.util.Map<Integer, Button> settings = new java.util.HashMap<>();

    MonitorScreen(MonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageHeight = 219;
        inventoryLabelY = 125;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(NuclearInfrastructure.MONITOR_MENU.get(), MonitorScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(Component.empty(), (b)->minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 8, topPos + 74, 160, 14).build());
        String[] keys = {"color", "smaller", "larger", "alignment", "join"};
        for (int i = 0; i < keys.length; i++) {
            int button = i + 1;
            settings.put(button, addRenderableWidget(Button.builder(Component.translatable("gui.ic2.monitor." + keys[i]),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button))
                .bounds(leftPos + 8 + i % 3 * 54, topPos + 90 + i / 3 * 16, 52, 14).build()));
        }
        updateButton();
    }

    private void updateButton() {
        redstone.setMessage(Component.translatable("gui.ic2.monitor.redstone_" + (menu.redstone() ? "on" : "off")));
        settings.get(1).setMessage(Component.translatable("color.minecraft." + net.minecraft.world.item.DyeColor.byId(menu.setting(2)).getName()));
        settings.get(2).setMessage(Component.literal("− " + menu.setting(3)));
        settings.get(3).setMessage(Component.literal("+ " + menu.setting(3)));
        settings.get(4).setMessage(Component.translatable("gui.ic2.monitor.align_" + menu.setting(4)));
        settings.get(5).setMessage(Component.translatable("gui.ic2.monitor." + (menu.setting(5) == 0 ? "lock" : "unlock")));
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
        RemasteredGui.frame(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 7, topPos + 20, leftPos + 87, topPos + 69, 0xff252d37);
        g.fill(leftPos + 92, topPos + 20, leftPos + 169, topPos + 69, 0xff737e8b);
        RemasteredGui.lightning(g, leftPos + 12, topPos + 26);
        RemasteredGui.bar(g, leftPos + 12, topPos + 56, 70, 4, menu.energy(), 1000, RemasteredGui.GREEN);
        for (var slot : menu.slots) RemasteredGui.slot(g, leftPos + slot.x, topPos + slot.y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        super.renderLabels(g, x, y);
        g.drawString(font, menu.energy() + " / 1000 EU", 12, 43, RemasteredGui.TEXT, false);
    }
}
