package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class ComparingCableScreen extends AbstractContainerScreen<ComparingCableMenu> {
    private static final Component MINIMUM = Component.translatable("gui.ic2.comparing_cable.minimum");
    private static final Component MAXIMUM = Component.translatable("gui.ic2.comparing_cable.maximum");
    private EditBox minimum;
    private EditBox maximum;
    private Button apply;
    private int syncedMinimum;
    private int syncedMaximum;

    ComparingCableScreen(ComparingCableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 108;
    }

    @Override
    protected void init() {
        super.init();
        minimum = addThresholdBox(leftPos + 100, topPos + 24, MINIMUM);
        maximum = addThresholdBox(leftPos + 100, topPos + 48, MAXIMUM);
        apply = addRenderableWidget(Button.builder(Component.translatable("gui.ic2.comparing_cable.apply"), button -> {
            int buttonId = ComparingCableMenu.thresholdButton(read(minimum), read(maximum));
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            }
        }).bounds(leftPos + 40, topPos + 77, 96, 20).build());
        minimum.setResponder(value -> updateApply());
        maximum.setResponder(value -> updateApply());
        syncedMinimum = -1;
        syncedMaximum = -1;
        syncThresholds();
        updateApply();
    }

    private EditBox addThresholdBox(int x, int y, Component label) {
        EditBox box = new EditBox(font, x, y, 64, 20, label);
        box.setMaxLength(4);
        box.setFilter(value -> value.chars().allMatch(character -> character >= '0' && character <= '9'));
        return addRenderableWidget(box);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncThresholds();
    }

    private void syncThresholds() {
        if (menu.minimum() != syncedMinimum || menu.maximum() != syncedMaximum) {
            syncedMinimum = menu.minimum();
            syncedMaximum = menu.maximum();
            minimum.setValue(Integer.toString(syncedMinimum));
            maximum.setValue(Integer.toString(syncedMaximum));
        }
    }

    private void updateApply() {
        apply.active = ComparingCableEntity.validThresholds(read(minimum), read(maximum));
    }

    private static int read(EditBox box) {
        String value = box.getValue();
        return value.isEmpty() ? -1 : Integer.parseInt(value);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 20, leftPos + 94, topPos + 72, 0xff252d37);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0x404040, false);
        graphics.drawString(font, MINIMUM, 11, 30, RemasteredGui.TEXT, false);
        graphics.drawString(font, MAXIMUM, 11, 54, RemasteredGui.TEXT, false);
    }
}
