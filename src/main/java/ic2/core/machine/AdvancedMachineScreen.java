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
final class AdvancedMachineScreen extends AbstractContainerScreen<AdvancedMachineMenu> {
    private Button splitButton;

    AdvancedMachineScreen(AdvancedMachineMenu menu, Inventory player, Component title) {
        super(menu, player, title);
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(AdvancedMachines.MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.ROTARY_MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.CENTRIFUGAL_MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.SINGULARITY_MENU.get(), AdvancedMachineScreen::new);
    }

    @Override protected void init() {
        super.init();
        splitButton = addRenderableWidget(Button.builder(splitName(), button ->
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
            .bounds(leftPos + 64, topPos + 56, 104, 18).build());
        splitButton.visible = menu.variant.layout.supportsSplit();
    }

    private Component splitName() {
        return Component.translatable(menu.split() ? "gui.ic2.induction_split" : "gui.ic2.induction_shared");
    }

    @Override protected void containerTick() {
        super.containerTick();
        splitButton.setMessage(splitName());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 2, 0xffffffff);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
        graphics.fill(leftPos + 67, topPos + 35, leftPos + 91, topPos + 49, 0xff555555);
        graphics.fill(leftPos + 67, topPos + 35, leftPos + 67 + 24 * menu.progress() / 1000, topPos + 49, 0xffffb84d);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        String metric = switch (menu.variant) {
            case ROTARY, CENTRIFUGAL -> "gui.ic2.advanced_speed";
            case SINGULARITY -> "gui.ic2.advanced_pressure";
            default -> "gui.ic2.induction_heat";
        };
        graphics.drawString(font, Component.translatable(metric, menu.heat() / 100), 88, 19, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.induction_energy", menu.energy()), 8, 73, 0x404040, false);
    }
}
