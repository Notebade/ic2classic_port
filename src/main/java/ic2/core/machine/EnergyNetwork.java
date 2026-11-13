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
    private static final int MILLIEU_PER_EU = 1000;

    private EnergyNetwork() {}

    static int send(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, int sourceStorageOutput) {
        var pending = new PriorityQueue<>(Comparator.comparingInt(Route::lossMilliEu).thenComparingLong(Route::order));
        var visited = new HashSet<BlockPos>();
        long order = 0;
        pending.add(new Route(source, 0, order++, null));
        int remaining = available;

        while (!pending.isEmpty()) {
            Route route = pending.remove();
            if (!visited.add(route.position())) {
                continue;
            }
            int loss = route.lossMilliEu() / MILLIEU_PER_EU;
            for (Direction direction : Direction.values()) {
                if (remaining <= loss) {
                    break;
                }
                if (route.position().equals(source) && !emitsTo.test(direction)) {
                    continue;
                }
                BlockPos next = route.position().relative(direction);
                if (!level.getChunkSource().hasChunk(
                    SectionPos.blockToSectionCoord(next.getX()), SectionPos.blockToSectionCoord(next.getZ()))) {
                    continue;
                }
                if (!next.equals(source) && level.getBlockEntity(next) instanceof EnergySink machine
                    && machine.canReceiveEnergy(direction.getOpposite(), sourceStorageOutput, available)) {
                    int accepted = machine.receiveEnergy(remaining - loss);
                    if (accepted > 0) {
                        remaining -= accepted + loss;
                        recordTransfer(level, route.meters(), accepted, loss);
                    }
                }
                var state = level.getBlockState(next);
                if (state.getBlock() instanceof EnergyCableBlock cable && cable.canCarry(state, available)
                    && !visited.contains(next)) {
                    int nextLoss = route.lossMilliEu() + cable.lossMilliEu();
                    if (nextLoss / MILLIEU_PER_EU < remaining) {
                        MeterVisit meters = cable instanceof EnergyMeterCableBlock meter
                            ? new MeterVisit(meter, next, nextLoss / MILLIEU_PER_EU, route.meters()) : route.meters();
                        pending.add(new Route(next, nextLoss, order++, meters));
                    }
                }
            }
        }
        return available - remaining;
    }

    private static void recordTransfer(Level level, MeterVisit meters, int accepted, int routeLoss) {
        for (MeterVisit visit = meters; visit != null; visit = visit.previous()) {
            // A meter reports energy leaving its cable, including any loss farther along the route.
            visit.meter().recordTransfer(level, visit.position(), accepted + routeLoss - visit.loss());
        }
    }

    private record Route(BlockPos position, int lossMilliEu, long order, MeterVisit meters) {}

    private record MeterVisit(EnergyMeterCableBlock meter, BlockPos position, int loss, MeterVisit previous) {}
}
