package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class StoneMaceratorScreen extends AbstractContainerScreen<StoneMaceratorMenu> {
    private static final ResourceLocation BACKGROUND =
        ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation PROGRESS = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    StoneMaceratorScreen(StoneMaceratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.isLit()) {
            int height = (int) Math.ceil(menu.getLitProgress() * 13);
            graphics.blitSprite(FLAME, 14, 14, 0, 14 - height, leftPos + 56, topPos + 50 - height, 14, height);
        }
        int width = (int) Math.ceil(menu.getBurnProgress() * 24);
        if (width > 0) {
            graphics.blitSprite(PROGRESS, 24, 16, 0, 0, leftPos + 79, topPos + 34, width, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
