package ic2.core.machine;

import ic2.core.IC2;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class ExtraGeneratorScreen extends AbstractContainerScreen<ExtraGeneratorMenu> {
    ExtraGeneratorScreen(ExtraGeneratorMenu menu, Inventory player, Component title) {
        super(menu, player, title);
        imageHeight = 188;
    }

    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ExtraGenerators.SOLAR_MENU.get(), ExtraGeneratorScreen::new);
        event.register(ExtraGenerators.SLAG_MENU.get(), ExtraGeneratorScreen::new);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.extra_energy", menu.energy()), 8, 18, 0x404040, false);
        if (menu.type == ExtraGenerators.Type.SOLAR) {
            graphics.drawString(font, Component.translatable("gui.ic2.induction_heat", menu.heat() / 240), 8, 30, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.extra_water", menu.water()), 8, 42, 0x404040, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.ic2.extra_fuel", menu.fuelPercent()), 8, 30, 0x404040, false);
        }
        graphics.drawString(font, Component.translatable("gui.ic2.extra_output", String.format(Locale.ROOT, "%.2f", menu.outputHundredths() / 100.0)), 8, 54, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.charge"), 12, 65, 0x404040, false);
        graphics.drawString(font, Component.translatable(menu.type == ExtraGenerators.Type.SOLAR ? "gui.ic2.extra_water_slot" : "gui.ic2.extra_fuel_slot"), 64, 65, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.extra_empty_slot"), 125, 65, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 96, 0x404040, false);
    }
}
