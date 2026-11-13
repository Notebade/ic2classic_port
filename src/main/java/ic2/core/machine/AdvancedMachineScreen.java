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
        event.register(AdvancedMachines.RARE_EARTH_MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.COMPACTING_MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.SLOW_MENU.get(), AdvancedMachineScreen::new);
        event.register(AdvancedMachines.PRESSURE_MENU.get(), AdvancedMachineScreen::new);
    }

    @Override protected void init() {
        super.init();
        splitButton = addRenderableWidget(Button.builder(splitName(), button ->
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
            .bounds(leftPos + 64, topPos + 56, 104, 18).build());
        splitButton.visible = menu.variant.layout.supportsSplit() || menu.variant == AdvancedMachines.Variant.SLOW;
        if (menu.variant == AdvancedMachines.Variant.SLOW) {
            splitButton.setWidth(64);
            splitButton.setX(leftPos + 104);
        }
    }

    private Component splitName() {
        if (menu.variant == AdvancedMachines.Variant.SLOW) return Component.translatable("gui.ic2.grinder_mode", menu.grinderMode());
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
        int progressX = menu.variant == AdvancedMachines.Variant.SLOW ? 83 : 67;
        graphics.fill(leftPos + progressX, topPos + 35, leftPos + progressX + 24, topPos + 49, 0xff555555);
        graphics.fill(leftPos + progressX, topPos + 35, leftPos + progressX + 24 * menu.progress() / 1000, topPos + 49, 0xffffb84d);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        float titleScale = Math.min(1F, (imageWidth - 16F) / Math.max(1, font.width(title)));
        graphics.pose().pushPose();
        graphics.pose().scale(titleScale, titleScale, 1F);
        graphics.drawString(font, title, (int) (8 / titleScale), (int) (6 / titleScale), 0x404040, false);
        graphics.pose().popPose();
        String metric = switch (menu.variant) {
            case ROTARY, CENTRIFUGAL, RARE_EARTH, COMPACTING -> "gui.ic2.advanced_speed";
            case SLOW -> "gui.ic2.grinder_chance";
            case SINGULARITY -> "gui.ic2.advanced_pressure";
            default -> "gui.ic2.induction_heat";
        };
        int value = menu.variant == AdvancedMachines.Variant.SLOW ? RecyclerControl.chancePercent(menu.grinderMode(), 4) : menu.heat() / 100;
        graphics.drawString(font, Component.translatable(metric, value), 88, 19, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.ic2.storage_energy", menu.energy(), menu.variant.capacity()), 8, 73, 0x404040, false);
        if (menu.variant == AdvancedMachines.Variant.RARE_EARTH) {
            graphics.drawString(font, Component.translatable("gui.ic2.rare_material", menu.materialProgress() / 10), 64, 59, 0x404040, false);
        }
    }
}
