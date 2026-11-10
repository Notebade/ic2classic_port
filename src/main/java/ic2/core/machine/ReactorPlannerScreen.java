package ic2.core.machine;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class ReactorPlannerScreen extends AbstractContainerScreen<ReactorPlannerMenu> {
    private Button run;
    private Button mode;
    private Button limit;
    private Button speed;
    private int displayedSpeed = -1;
    private int displayedLimit = -1;

    ReactorPlannerScreen(ReactorPlannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 278;
        inventoryLabelY = 185;
    }

    @Override
    protected void init() {
        super.init();
        button(0, 8, 20, "-");
        button(1, 30, 20, "+");
        button(2, 52, 32, "step");
        run = button(3, 86, 42, "run");
        button(4, 130, 38, "reset");
        mode = addRenderableWidget(Button.builder(label("electric"), b -> send(5))
            .bounds(leftPos + 8, topPos + 165, 64, 16).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> send(6))
            .bounds(leftPos + 74, topPos + 165, 14, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> send(7))
            .bounds(leftPos + 107, topPos + 165, 14, 16).build());
        addRenderableWidget(Button.builder(Component.literal("H"), b -> send(Screen.hasShiftDown() ? 9 : 8))
            .bounds(leftPos + 123, topPos + 165, 14, 16)
            .tooltip(Tooltip.create(Component.translatable("gui.ic2.reactor_planner.heat_adjust", ReactorPlannerState.HEAT_STEP)))
            .build());
        limit = addRenderableWidget(Button.builder(Component.literal("T"), b -> send(Screen.hasShiftDown() ? 11 : 10))
            .bounds(leftPos + 139, topPos + 165, 14, 16).build());
        speed = addRenderableWidget(Button.builder(Component.literal("S"), b -> send(Screen.hasShiftDown() ? 13 : 12))
            .bounds(leftPos + 155, topPos + 165, 14, 16).build());
        displayedLimit = -1;
        displayedSpeed = -1;
        updateTooltips();
    }

    private Button button(int command, int x, int width, String key) {
        Component title = key.length() == 1 ? Component.literal(key) : label(key);
        return addRenderableWidget(Button.builder(title, b -> send(command))
            .bounds(leftPos + x, topPos + 17, width, 16).build());
    }

    private void send(int command) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, command);
    }

    private static Component label(String key) {
        return Component.translatable("gui.ic2.reactor_planner." + key);
    }

    @Override
    protected void containerTick() {
        run.setMessage(label(menu.running() ? "pause" : "run"));
        mode.setMessage(label(menu.steam() ? "steam" : "electric"));
        updateTooltips();
    }

    private void updateTooltips() {
        int currentLimit = menu.cycleLimit();
        if (currentLimit != displayedLimit) {
            Component value = currentLimit == 0 ? label("unlimited") : Component.literal(Integer.toString(currentLimit));
            limit.setTooltip(Tooltip.create(Component.translatable("gui.ic2.reactor_planner.cycle_limit", value)));
            displayedLimit = currentLimit;
        }
        int currentSpeed = menu.cyclesPerTick();
        if (currentSpeed != displayedSpeed) {
            speed.setTooltip(Tooltip.create(Component.translatable("gui.ic2.reactor_planner.speed", currentSpeed)));
            displayedSpeed = currentSpeed;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + 90;
        int y = topPos + 165;
        var part = menu.selectedPart();
        graphics.renderItem(part, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.renderTooltip(font, part, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 38 && mouseY < topPos + 47) {
            Component total = Component.translatable(menu.steam()
                ? "gui.ic2.reactor_planner.total_steam" : "gui.ic2.reactor_planner.total_energy",
                menu.totalProduction());
            int average = menu.averageHeat();
            var lines = new ArrayList<Component>(3);
            lines.add(total);
            if (average >= 0) {
                lines.add(Component.translatable("gui.ic2.reactor_planner.average_heat", average));
            }
            if (menu.peakHeat() >= 0) {
                lines.add(Component.translatable("gui.ic2.reactor_planner.peak_heat", menu.peakHeat()));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable(menu.steam()
            ? "gui.ic2.steam_reactor_status" : "gui.ic2.reactor_status", menu.output(), menu.heat(), menu.maxHeat()),
            8, 38, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.reactor_planner.cycles", menu.cycles()),
            8, 47, 0x404040, false);
        Component initialHeat = Component.translatable("gui.ic2.reactor_planner.starting_heat", menu.startingHeat());
        graphics.drawString(font, initialHeat, 168 - font.width(initialHeat), 47, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, slot.isActive() ? 0xFF8B8B8B : 0xFFAAAAAA);
        }
    }
}
