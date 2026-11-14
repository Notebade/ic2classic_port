package ic2.core.machine;

import net.minecraft.client.gui.GuiGraphics;

/** Shared client rendering primitives; no inventory or machine behavior lives here. */
public final class RemasteredGui {
    public static final int TEXT = 0xffe5e8ec;
    public static final int MUTED = 0xffadb7c4;
    public static final int ORANGE = 0xffffa438;
    public static final int GREEN = 0xff77d447;

    private RemasteredGui() {}

    /** Pixel lightning silhouette with a dark outline, independent of font glyphs. */
    public static void lightning(GuiGraphics graphics, int x, int y) {
        int[][] rows = {{5,8}, {4,7}, {4,7}, {3,6}, {2,8}, {1,7}, {4,6}, {4,5}, {3,4}, {3,4}, {2,3}, {2,2}};
        for (int row = 0; row < rows.length; row++)
            graphics.fill(x + rows[row][0] - 1, y + row - 1, x + rows[row][1] + 2, y + row + 2, 0xff20242c);
        for (int row = 0; row < rows.length; row++) {
            graphics.fill(x + rows[row][0], y + row, x + rows[row][1] + 1, y + row + 1, 0xffffc42c);
            graphics.fill(x + rows[row][0], y + row, x + rows[row][0] + 1, y + row + 1, 0xffffee83);
        }
    }

    public static void background(GuiGraphics graphics, int x, int y) {
        frame(graphics, x, y, AlloyGuiLayout.WIDTH, AlloyGuiLayout.HEIGHT);
        graphics.fill(x + 6, y + 5, x + 250, y + 27, 0xffc4c8d0);
        graphics.fill(x + 8, y + 28, x + 248, y + 29, 0xff697380);
        recess(graphics, x + 8, y + 31, 240, 113);
        recess(graphics, x + 46, y + 160, 164, 76);
    }

    public static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xff10151b);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff737e8b);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xffa3adb8);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xff747c88);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xffa6acb7);
        for (int dy = 6; dy < height - 5; dy += 4)
            graphics.fill(x + 4, y + dy, x + width - 4, y + dy + 1, 0xffaeb4be);
        bolt(graphics, x + 3, y + 3);
        bolt(graphics, x + width - 7, y + 3);
        bolt(graphics, x + 3, y + height - 7);
        bolt(graphics, x + width - 7, y + height - 7);
    }

    public static void container(GuiGraphics graphics, int x, int y, int width, int height,
            java.util.List<net.minecraft.world.inventory.Slot> slots, int inventoryY) {
        frame(graphics, x, y, width, height);
        recess(graphics, x + 6, y + 17, width - 12, Math.max(1, inventoryY - 21));
        graphics.fill(x + 7, y + 17, x + width - 7, y + 19, ORANGE);
        for (var slot : slots) if (slot.isActive()) slot(graphics, x + slot.x, y + slot.y);
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

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 18, y + 18, 0xff080b0e);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff68727d);
        graphics.fill(x, y, x + 16, y + 16, 0xff858c96);
        graphics.fill(x, y, x + 16, y + 1, 0xff171d24);
    }

    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, int value, int max, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xff10151b);
        graphics.fill(x, y, x + width, y + height, 0xff303945);
        int filled = (int) (width * Math.clamp((double) value / Math.max(1, max), 0, 1));
        graphics.fill(x, y, x + filled, y + height, color);
    }

    public static void verticalBar(GuiGraphics graphics, int x, int y, int width, int height, int value, int max) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xff10151b);
        graphics.fill(x, y, x + width, y + height, 0xff303945);
        int filled = (int) (height * Math.clamp((double) value / Math.max(1, max), 0, 1));
        graphics.fill(x, y + height - filled, x + width, y + height, GREEN);
        for (int dy = 4; dy < height; dy += 5) graphics.fill(x, y + dy, x + width, y + dy + 1, 0xff37552c);
    }
}
