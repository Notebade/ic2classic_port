package ic2.core.machine;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class RemasteredIconButton extends RemasteredButton {
    private final ItemStack icon;
    private final Component label;
    RemasteredIconButton(int x, int y, Component label, ItemStack icon, OnPress action, BooleanSupplier selected) {
        super(x, y, 22, 22, Component.empty(), action, selected);
        this.icon = icon;
        this.label = label;
        setTooltip(Tooltip.create(label));
    }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() { return label.copy(); }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.renderWidget(graphics, mouseX, mouseY, tick);
        graphics.renderItem(icon, getX() + 3, getY() + 3);
    }
}
