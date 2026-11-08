package ic2.core.machine;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

final class EnergyNetwork {
    private EnergyNetwork() {}

    static int send(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, MachineKind sourceKind) {
        var pending = new PriorityQueue<>(Comparator.comparingInt(Route::lossHundredths).thenComparingLong(Route::order));
        var visited = new HashSet<BlockPos>();
        long order = 0;
        pending.add(new Route(source, 0, order++));
        int remaining = available;

        while (!pending.isEmpty()) {
            Route route = pending.remove();
            if (!visited.add(route.position())) {
                continue;
            }
            int loss = route.lossHundredths() / 100;
            for (Direction direction : Direction.values()) {
                if (remaining <= loss) {
                    break;
                }
                if (route.lossHundredths() == 0 && !emitsTo.test(direction)) {
                    continue;
                }
                BlockPos next = route.position().relative(direction);
                if (!level.getChunkSource().hasChunk(
                    SectionPos.blockToSectionCoord(next.getX()), SectionPos.blockToSectionCoord(next.getZ()))) {
                    continue;
                }
                if (!next.equals(source) && level.getBlockEntity(next) instanceof MachineEntity machine
                    && machine.canReceiveEnergy(direction.getOpposite(), sourceKind, available)) {
                    remaining -= machine.receiveEnergy(remaining - loss) + loss;
                }
                var state = level.getBlockState(next);
                if (state.getBlock() instanceof EnergyCableBlock cable && !visited.contains(next)) {
                    int nextLoss = route.lossHundredths() + cable.lossHundredths();
                    if (nextLoss / 100 < remaining) {
                        pending.add(new Route(next, nextLoss, order++));
                    }
                }
            }
        }
        return available - remaining;
    }

    private record Route(BlockPos position, int lossHundredths, long order) {}
}
