package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared client rendering primitives; no inventory or machine behavior lives here. */
final class RemasteredGui {
    static final int TEXT = 0xffe5e8ec;
    static final int MUTED = 0xffadb7c4;
    static final int ORANGE = 0xffffa438;
    static final int GREEN = 0xff77d447;
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
        IC2.MOD_ID, "textures/gui/remastered/machine_panel.png");

    private RemasteredGui() {}

    static void background(GuiGraphics graphics, int x, int y) {
        // Map the generated art's three bands to the exact Minecraft layout.
        graphics.blit(PANEL, x, y, 256, 48, 0, 0, 1254, 280, 1254, 1254);
        graphics.blit(PANEL, x, y + 48, 256, 100, 0, 280, 1254, 570, 1254, 1254);
        graphics.blit(PANEL, x, y + 148, 256, 92, 0, 850, 1254, 404, 1254, 1254);
    }

    static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 18, y + 18, 0xff080b0e);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff68727d);
        graphics.fill(x, y, x + 16, y + 16, 0xff303741);
        graphics.fill(x, y, x + 16, y + 1, 0xff171d24);
    }

    static void bar(GuiGraphics graphics, int x, int y, int width, int height, int value, int max, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xff10151b);
        graphics.fill(x, y, x + width, y + height, 0xff303945);
        int filled = (int) (width * Math.clamp((double) value / Math.max(1, max), 0, 1));
        graphics.fill(x, y, x + filled, y + height, color);
    }
}
