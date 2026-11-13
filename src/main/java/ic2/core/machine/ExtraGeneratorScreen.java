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
        imageHeight = 166;
    }

    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ExtraGenerators.SOLAR_MENU.get(), ExtraGeneratorScreen::new);
        event.register(ExtraGenerators.SLAG_MENU.get(), ExtraGeneratorScreen::new);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot == null && mouseX >= leftPos + 8 && mouseX < leftPos + 168
            && mouseY >= topPos + 14 && mouseY < topPos + 74) {
            var info = new java.util.ArrayList<Component>();
            info.add(Component.translatable("gui.ic2.extra_energy", menu.energy()));
            info.add(Component.translatable("gui.ic2.extra_output", String.format(Locale.ROOT, "%.2f", menu.outputHundredths() / 100.0)));
            if (menu.type == ExtraGenerators.Type.SOLAR) {
                info.add(Component.translatable("gui.ic2.induction_heat", menu.heat() / 240));
                info.add(Component.translatable("gui.ic2.extra_water", menu.water()));
            } else info.add(Component.translatable("gui.ic2.extra_fuel", menu.fuelPercent()));
            graphics.renderComponentTooltip(font, info, mouseX, mouseY);
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var layout = GenerationLayout.forId(menu.type == ExtraGenerators.Type.SOLAR ? "solar_turbine" : "slag_generator");
        var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, layout.texture());
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.type == ExtraGenerators.Type.SOLAR) {
            int water = Math.clamp(menu.water() * 41 / 40000, 0, 41);
            graphics.blit(texture, leftPos + 99, topPos + 67 - water, 176, 55 + 41 - water, 12, water);
            graphics.blit(texture, leftPos + 99, topPos + 26, 188, 14, 12, 41);
        } else {
            int fuel = Math.clamp(menu.fuelPercent() * 14 / 100, 0, 14);
            graphics.blit(texture, leftPos + 66, topPos + 50 - fuel, 176, 14 - fuel, 14, fuel);
            int energy = Math.clamp(menu.energy() * 24 / 8000, 0, 24);
            graphics.blit(texture, leftPos + 94, topPos + 35, 176, 14, energy, 17);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 74, 0x404040, false);
    }
}
