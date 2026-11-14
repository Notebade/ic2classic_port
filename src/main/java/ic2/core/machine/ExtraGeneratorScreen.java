package ic2.core.machine;

import ic2.core.IC2;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class ExtraGeneratorScreen extends AbstractContainerScreen<ExtraGeneratorMenu> {
    ExtraGeneratorScreen(ExtraGeneratorMenu menu, Inventory player, Component title) {
        super(menu, player, title);
        imageHeight = 189;
        inventoryLabelY = 96;
    }

    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ExtraGenerators.SOLAR_MENU.get(), ExtraGeneratorScreen::new);
        event.register(ExtraGenerators.SLAG_MENU.get(), ExtraGeneratorScreen::new);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean solar() {
        return menu.type == ExtraGenerators.Type.SOLAR;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + 169, topPos + 35, 0xff252d37);
        RemasteredGui.lightning(graphics, leftPos + 9, topPos + 20);
        RemasteredGui.bar(graphics, leftPos + 23, topPos + 30, 142, 3, menu.energy(),
            solar() ? SolarTurbineCycle.MAX_STORED : SlagGeneratorCycle.CAPACITY, RemasteredGui.GREEN);
        RemasteredGui.bar(graphics, leftPos + 9, topPos + 82, 45, 4,
            solar() ? menu.heat() : menu.fuelPercent(),
            solar() ? SolarTurbineCycle.MAX_HEAT : 100, solar() ? 0xff439fdf : RemasteredGui.ORANGE);
        for (var slot : menu.slots) {
            RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.ic2.extra_energy", menu.energy()),
            23, 20, RemasteredGui.TEXT, false);
        String output = String.format(Locale.ROOT, "%.2f", menu.outputHundredths() / 100.0);
        graphics.drawString(font, Component.translatable("gui.ic2.turbine_output", output), 8, 42, 0x404040, false);
        if (solar()) {
            graphics.drawString(font, Component.translatable("gui.ic2.extra_water", menu.water()), 8, 53, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.induction_heat",
                100 * menu.heat() / SolarTurbineCycle.MAX_HEAT), 8, 71, 0x404040, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.ic2.extra_fuel", menu.fuelPercent()), 8, 71, 0x404040, false);
        }
    }
}
