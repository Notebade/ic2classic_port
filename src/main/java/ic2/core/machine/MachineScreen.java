package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(Machines.STONE_MACERATOR_MENU.get(), StoneMaceratorScreen::new);
        event.register(Machines.MENU.get(), MachineScreen::new);
        event.register(Machines.BATBOX_MENU.get(), MachineScreen::new);
        event.register(Machines.MFE_MENU.get(), MachineScreen::new);
        event.register(Machines.MFSU_MENU.get(), MachineScreen::new);
        event.register(Machines.ALLOY_SMELTER_MENU.get(), MachineScreen::new);
        event.register(Machines.RARE_EARTH_MENU.get(), MachineScreen::new);
        event.register(Machines.ELECTROLYZER_MENU.get(), MachineScreen::new);
        event.register(Machines.FABRICATOR_MENU.get(), MachineScreen::new);
        event.register(Machines.GEOTHERMAL_MENU.get(), MachineScreen::new);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
            graphics.fill(x, y, x + 17, y + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.isStorage()) {
            graphics.drawString(font, Component.translatable("gui.ic2.storage_energy", menu.energy(), menu.storageCapacity()),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.charge"), 43, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.discharge"), 95, 61, 0x404040, false);
            Direction direction = Direction.from3DDataValue(menu.work());
            Component output = Component.translatable("gui.ic2.storage_output",
                Component.translatable("direction.ic2." + direction.getName()));
            graphics.drawString(font, output, imageWidth - 8 - font.width(output), inventoryLabelY, 0x404040, false);
            return;
        }
        if (menu.isGeothermal()) {
            graphics.drawString(font, Component.translatable("gui.ic2.geothermal_status", menu.energy(),
                menu.fluid(), GeothermalGeneratorBlock.TANK_CAPACITY), 8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.input"), 9, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.output"), 69, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.charge"), 120, 61, 0x404040, false);
            return;
        }
        if (menu.isAlloySmelter()) {
            graphics.drawString(font, Component.translatable("gui.ic2.machine_status", menu.energy(), menu.work()),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.input"), 14, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.input"), 70, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.output"), 120, 61, 0x404040, false);
            return;
        }
        if (menu.isFabricator()) {
            graphics.drawString(font, Component.translatable("gui.ic2.storage_energy", menu.energy(), MassFabrication.MATTER_COST),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.amplifier", menu.work()),
                8, 30, 0x404040, false);
        } else {
            Component status;
            if (menu.isRareEarth()) {
                status = Component.translatable("gui.ic2.rare_earth_status", menu.energy(), menu.work(), menu.fluid());
            } else if (menu.isElectrolyzer()) {
                status = Component.translatable("gui.ic2.storage_energy", menu.energy(), MachineKind.ELECTROLYZER.capacity);
            } else if (menu.fluid() < 0) {
                status = Component.translatable("gui.ic2.machine_status", menu.energy(), menu.work());
            } else {
                status = Component.translatable("gui.ic2.pump_status", menu.energy(), menu.work(), menu.fluid());
            }
            graphics.drawString(font, status, 8, 21, 0x404040, false);
        }
        Component input = Component.translatable(menu.isFabricator() ? "gui.ic2.amplifier_slot" : "gui.ic2.input");
        graphics.drawString(font, input, 61 - font.width(input) / 2, 61, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.output"), 102, 61, 0x404040, false);
    }
}
