package ic2.core.machine;

import java.util.Set;

final class MonitorLayout {

    record Rectangle(int x, int y, int width, int height) {}

    static long key(int x, int y) {
        return (long)x << 32 | y & 4294967295L;
    }

    static Rectangle rectangle(Set<Long> cells) {
        if (cells.isEmpty()) return null;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (long cell : cells) {
            int x = (int)(cell >> 32);
            int y = (int)cell;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        long width = (long)maxX - minX + 1;
        long height = (long)maxY - minY + 1;
        if (width * height != cells.size()) return null;
        return new Rectangle(minX, minY, (int)width, (int)height);
    }

    static int textX(int width, int textWidth, int alignment) {
        return Math.max(0, width - textWidth) * Math.clamp(alignment, 0, 2) / 2;
    }
}
