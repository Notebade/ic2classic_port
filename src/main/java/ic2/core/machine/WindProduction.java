package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class WindProduction {
    static final int OUTPUT_LIMIT = 16;
    static final int UNITS_PER_EU = 100;
    static final int SAMPLE_INTERVAL = 128;
    private static final int WIND_PERIOD = 6000;
    private static final int FULL_AIR_SPACE = 525;

    private WindProduction() {}

    static int sample(ServerLevel level, BlockPos pos) {
        var bounds = BoundingBox.fromCorners(pos.offset(-4, -2, -4), pos.offset(4, 4, 4));
        int freeAir = ConnectedSpace.count(level, pos, bounds, FULL_AIR_SPACE, level::isEmptyBlock);
        return (int) (OUTPUT_LIMIT * windSpeed(level) * freeAir / FULL_AIR_SPACE);
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

}
