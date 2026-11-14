package ic2.core.machine;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

final class ReactorPlannerScreen extends AbstractContainerScreen<ReactorPlannerMenu> {
    private Button run;
    private Button mode;
    private Button limit;
    private Button speed;
    private int displayedSpeed = -1;
    private int displayedLimit = -1;
    private ReactorPlanCode.Format exportFormat = ReactorPlanCode.Format.V2;

    ReactorPlannerScreen(ReactorPlannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 282;
        imageHeight = 278;
        inventoryLabelX = 60;
        inventoryLabelY = 185;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("F"), b -> send(14))
            .bounds(leftPos + 221, topPos + 6, 14, 13).tooltip(Tooltip.create(label("full_run"))).build());
        addRenderableWidget(Button.builder(Component.literal("↔"), b -> exchangeCode())
            .bounds(leftPos + 242, topPos + 6, 17, 13)
            .tooltip(Tooltip.create(label("code_help"))).build());
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
        addRenderableWidget(CycleButton.<ReactorPlanCode.Format>builder(format -> Component.literal(format.name()))
            .withValues(ReactorPlanCode.Format.values()).withInitialValue(exportFormat).displayOnlyValue()
            .withTooltip(format -> Tooltip.create(label("code_format")))
            .create(leftPos + 178, topPos + 165, 50, 13, label("code_format"), (button, format) -> exportFormat = format));
        displayedLimit = -1;
        displayedSpeed = -1;
        updateTooltips();
    }

    private Button button(int command, int x, int width, String key) {
        Component title = key.length() == 1 ? Component.literal(key) : label(key);
        return addRenderableWidget(Button.builder(title, b -> send(command))
            .bounds(leftPos + x, topPos + 145, width, 16).build());
    }

    private void send(int command) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, command);
    }

    private void exchangeCode() {
        boolean importing = Screen.hasShiftDown();
        String code = importing ? minecraft.keyboardHandler.getClipboard().strip() : "";
        if (importing && (code.isEmpty() || code.length() > ReactorPlanCode.MAX_LENGTH)) {
            minecraft.player.displayClientMessage(label("code_invalid"), true);
            return;
        }
        PacketDistributor.sendToServer(new ReactorPlanPayload(menu.containerId, exportFormat.ordinal(), code));
    }

    static void copyCode(String code) {
        var client = Minecraft.getInstance();
        client.keyboardHandler.setClipboard(code);
        client.player.displayClientMessage(label("code_copied"), true);
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
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 104 && mouseY >= topPos + 29 && mouseY < topPos + 139) {
            Component total = Component.translatable(menu.steam()
                ? "gui.ic2.reactor_planner.total_steam" : "gui.ic2.reactor_planner.total_energy",
                menu.totalProduction());
            int average = menu.averageHeat();
            var lines = new ArrayList<Component>(6);
            lines.add(total);
            lines.add(Component.translatable("gui.ic2.reactor_planner.fuel_runtime", menu.fuelTicks(), menu.remainingFuelTicks()));
            if (menu.efficiencyPermille() >= 0) lines.add(Component.translatable("gui.ic2.reactor_planner.efficiency", java.math.BigDecimal.valueOf(menu.efficiencyPermille(), 3).toPlainString()));
            if (menu.fullRunState() > 0) lines.add(label("full_run_state_" + menu.fullRunState()));
            if (average >= 0) {
                lines.add(Component.translatable("gui.ic2.reactor_planner.average_heat", average));
            }
            if (menu.peakHeat() >= 0) {
                lines.add(Component.translatable("gui.ic2.reactor_planner.peak_heat", menu.peakHeat()));
            }
            lines.add(Component.translatable(menu.steam() ? "gui.ic2.reactor_planner.forecast_steam"
                : "gui.ic2.reactor_planner.forecast_energy", menu.forecastOutput()));
            lines.add(Component.translatable("gui.ic2.reactor_planner.forecast_heat", menu.forecastHeatChange()));
            lines.add(Component.translatable("gui.ic2.reactor_planner.forecast_blast",
                java.math.BigDecimal.valueOf(menu.forecastBlast(), 2).toPlainString()));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int y = metric(graphics, Component.translatable(menu.steam()
            ? "gui.ic2.steam_reactor_status" : "gui.ic2.reactor_status", menu.output(), menu.heat(), menu.maxHeat()), 35);
        y = metric(graphics, Component.translatable("gui.ic2.reactor_planner.cycles", menu.cycles()), y);
        metric(graphics, Component.translatable("gui.ic2.reactor_planner.starting_heat", menu.startingHeat()), y);
    }

    private int metric(GuiGraphics graphics, Component text, int y) {
        for (var line : font.split(text, 90)) {
            if (y + font.lineHeight > 119) break;
            graphics.drawString(font, line, 11, y, RemasteredGui.TEXT, false);
            y += font.lineHeight;
        }
        return y + 5;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 29, leftPos + 105, topPos + 139, 0xff252d37);
        graphics.fill(leftPos + 109, topPos + 29, leftPos + 276, topPos + 144, 0xffc5c1b2);
        RemasteredGui.bar(graphics, leftPos + 12, topPos + 128, 88, 4,
            menu.heat(), menu.maxHeat(), RemasteredGui.ORANGE);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (slot.isActive()) RemasteredGui.slot(graphics, x, y);
            else graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff606571);
        }
    }
}
