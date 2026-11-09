package ic2.core.machine;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

final class WindProduction {
    static final int OUTPUT_LIMIT = 16;
    static final int UNITS_PER_EU = 100;
    static final int SAMPLE_INTERVAL = 128;
    private static final int WIND_PERIOD = 6000;
    private static final int FULL_AIR_SPACE = 525;

    private WindProduction() {}

    static int sample(ServerLevel level, BlockPos pos) {
        return (int) (OUTPUT_LIMIT * windSpeed(level) * freeAir(level, pos) / FULL_AIR_SPACE);
    }

    private static double windSpeed(ServerLevel level) {
        long time = level.getGameTime();
        long period = Math.floorDiv(time, WIND_PERIOD);
        long seed = level.getSeed() ^ level.dimension().location().hashCode();
        double previous = target(seed, period - 1);
        double next = target(seed, period);
        double step = Math.floorMod(time, WIND_PERIOD) / 10.0;
        return Math.abs(previous + Math.copySign(Math.min(step, Math.abs(next - previous)), next - previous));
    }

    private static double target(long seed, long period) {
        return RandomSource.create(seed ^ (period * 0x9E3779B97F4A7C15L)).nextDouble() * 200 - 100;
    }

    private static int freeAir(ServerLevel level, BlockPos origin) {
        var pending = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        pending.add(origin);
        visited.add(origin);
        int free = 0;
        while (!pending.isEmpty() && free < FULL_AIR_SPACE) {
            BlockPos current = pending.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (insideSearchBox(origin, next) && visited.add(next)
                    && level.isLoaded(next) && level.isEmptyBlock(next)) {
                    pending.add(next);
                    free++;
                }
            }
        }
        return Math.min(free, FULL_AIR_SPACE);
    }

    private static boolean insideSearchBox(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= 4 && Math.abs(pos.getZ() - origin.getZ()) <= 4
            && pos.getY() >= origin.getY() - 2 && pos.getY() <= origin.getY() + 4;
    }
}
