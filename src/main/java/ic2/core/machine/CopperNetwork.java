package ic2.core.machine;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

final class CopperNetwork {
    private CopperNetwork() {}

    static int send(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, MachineKind sourceKind) {
        var pending = new PriorityQueue<>(Comparator.comparingInt(Route::lossTenths).thenComparingLong(Route::order));
        var visited = new HashSet<BlockPos>();
        long order = 0;
        pending.add(new Route(source, 0, order++));
        int remaining = available;

        while (!pending.isEmpty()) {
            Route route = pending.remove();
            if (!visited.add(route.position())) {
                continue;
            }
            int loss = route.lossTenths() / 10;
            for (Direction direction : Direction.values()) {
                if (remaining <= loss) {
                    break;
                }
                if (route.lossTenths() == 0 && !emitsTo.test(direction)) {
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
                if (state.getBlock() instanceof CopperCableBlock && !visited.contains(next)) {
                    int nextLoss = route.lossTenths() + (state.is(Machines.INSULATED_COPPER_CABLE.get()) ? 2 : 3);
                    if (nextLoss / 10 < remaining) {
                        pending.add(new Route(next, nextLoss, order++));
                    }
                }
            }
        }
        return available - remaining;
    }

    private record Route(BlockPos position, int lossTenths, long order) {}
}
