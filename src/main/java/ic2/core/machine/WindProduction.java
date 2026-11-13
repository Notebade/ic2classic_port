package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class WindProduction {
    static final int OUTPUT_LIMIT = 16;
    static final int UNITS_PER_EU = 100;
    static final int SAMPLE_INTERVAL = 128;
    private static final int FULL_AIR_SPACE = 525;

    private WindProduction() {}

    static int sample(ServerLevel level, BlockPos pos) {
        var bounds = BoundingBox.fromCorners(pos.offset(-4, -2, -4), pos.offset(4, 4, 4));
        int freeAir = ConnectedSpace.count(level, pos, bounds, FULL_AIR_SPACE, level::isEmptyBlock);
        return (int) (OUTPUT_LIMIT * windSpeed(level) * freeAir / FULL_AIR_SPACE);
    }

    static double windSpeed(ServerLevel level) {
        return WindWorldData.speed(level);
    }

}
