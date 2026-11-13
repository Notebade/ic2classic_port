package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;

/** Shared client rendering primitives; no inventory or machine behavior lives here. */
final class RemasteredGui {
    static final int TEXT = 0xffe5e8ec;
    static final int MUTED = 0xffadb7c4;
    static final int ORANGE = 0xffffa438;
    static final int GREEN = 0xff77d447;

    private RemasteredGui() {}

    static void background(GuiGraphics graphics, int x, int y) {
        frame(graphics, x, y, AlloyGuiLayout.WIDTH, AlloyGuiLayout.HEIGHT);
        graphics.fill(x + 6, y + 5, x + 250, y + 21, 0xffb2bac3);
        graphics.fill(x + 8, y + 21, x + 248, y + 22, ORANGE);
        recess(graphics, x + 8, y + 48, 240, 96);
        recess(graphics, x + 46, y + 160, 164, 76);
    }

    private static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xff10151b);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff737e8b);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xffa3adb8);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xff48515d);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xff343d48);
        for (int dy = 6; dy < height - 5; dy += 4)
            graphics.fill(x + 4, y + dy, x + width - 4, y + dy + 1, 0xff39434f);
        bolt(graphics, x + 3, y + 3);
        bolt(graphics, x + width - 7, y + 3);
        bolt(graphics, x + 3, y + height - 7);
        bolt(graphics, x + width - 7, y + height - 7);
    }

    private static void recess(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xff7b8794);
        graphics.fill(x, y, x + width, y + height, 0xff111820);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff252d37);
    }

    private static void bolt(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 4, y + 4, 0xff171e27);
        graphics.fill(x, y, x + 3, y + 3, 0xffaeb9c5);
        graphics.fill(x + 1, y + 1, x + 3, y + 2, 0xff414c59);
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
