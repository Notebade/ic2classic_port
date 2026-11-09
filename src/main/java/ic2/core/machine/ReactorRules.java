package ic2.core.machine;

import ic2.core.item.UraniumType;
import java.util.Arrays;

final class ReactorRules {
    static final int ROWS = 6;
    static final int COLUMNS = 9;
    static final int BASE_COLUMNS = 3;
    static final int SLOT_COUNT = ROWS * COLUMNS;
    static final int MAX_HEAT = 10_000;
    static final int CYCLE_TICKS = 20;
    static final int EU_PER_PULSE = 20;

    private ReactorRules() {}

    static int[] pulseTargets(int slot, int width, UraniumType type) {
        if (type != UraniumType.ENDER_PEARL) {
            return neighbors(slot, width);
        }
        int row = slot / COLUMNS;
        int column = slot % COLUMNS;
        int[] targets = new int[width + ROWS - 2];
        int index = 0;
        for (int distance = 1; distance < width; distance++) {
            if (column + distance < width) {
                targets[index++] = slot + distance;
            }
            if (column >= distance) {
                targets[index++] = slot - distance;
            }
        }
        for (int distance = 1; distance < ROWS; distance++) {
            if (row + distance < ROWS) {
                targets[index++] = slot + distance * COLUMNS;
            }
            if (row >= distance) {
                targets[index++] = slot - distance * COLUMNS;
            }
        }
        return targets;
    }

    static int[] heatTargets(int slot, int width, UraniumType type) {
        int[] adjacent = neighbors(slot, width);
        if (type != UraniumType.ENDER_PEARL) {
            return adjacent;
        }
        int[] targets = Arrays.copyOf(adjacent, 8);
        targets[4] = adjacent[0] >= 0 && adjacent[2] >= 0 ? slot - COLUMNS - 1 : -1;
        targets[5] = adjacent[1] >= 0 && adjacent[2] >= 0 ? slot - COLUMNS + 1 : -1;
        targets[6] = adjacent[0] >= 0 && adjacent[3] >= 0 ? slot + COLUMNS - 1 : -1;
        targets[7] = adjacent[1] >= 0 && adjacent[3] >= 0 ? slot + COLUMNS + 1 : -1;
        return targets;
    }

    static int[] neighbors(int slot, int width) {
        int column = slot % COLUMNS;
        return new int[] {
            column > 0 ? slot - 1 : -1,
            column + 1 < width ? slot + 1 : -1,
            slot >= COLUMNS ? slot - COLUMNS : -1,
            slot < SLOT_COUNT - COLUMNS ? slot + COLUMNS : -1
        };
    }
}
