package ic2.core.machine;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public final class EnergyNetwork {
    private static final int MILLIEU_PER_EU = 1000;

    private EnergyNetwork() {}

    public static int send(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, int sourceStorageOutput) {
        var pending = new PriorityQueue<>(Comparator.comparingInt(Route::lossMilliEu).thenComparingLong(Route::order));
        var visited = new HashSet<BlockPos>();
        var supplied = new java.util.HashMap<CableVisit, Integer>();
        long order = 0;
        pending.add(new Route(source, 0, order++, null, null));
        int remaining = available;

        while (!pending.isEmpty()) {
            Route route = pending.remove();
            if (!level.hasChunkAt(route.position())) continue;
            if (!route.position().equals(source) && (!(level.getBlockState(route.position()).getBlock() instanceof EnergyCableBlock cable)
                || !cable.canCarry(level.getBlockState(route.position()), 0))) continue;
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
                var entity = level.getBlockEntity(next);
                var machine = entity instanceof ic2.api.energy.EnergySink sink ? sink : level instanceof net.minecraft.server.level.ServerLevel server
                    ? ic2.api.internal.EnergyPortRegistry.resolve(server, next).orElse(null) : null;
                int incoming = machine instanceof ic2.api.energy.OvervoltageHandler ? remaining - loss : available;
                if (!next.equals(source) && machine != null
                    && machine.canReceiveEnergy(direction.getOpposite(), sourceStorageOutput, incoming)) {
                    int accepted = Math.clamp(machine.receiveEnergy(remaining - loss), 0, remaining - loss);
                    if (accepted > 0) {
                        remaining -= accepted + loss;
                        recordTransfer(level, route.meters(), accepted, loss);
                        if (route.cables() != null) supplied.merge(route.cables(), accepted, Integer::sum);
                    }
                } else if (!next.equals(source) && machine instanceof ic2.api.energy.OvervoltageHandler handler
                    && machine.canReceiveEnergy(direction.getOpposite(), sourceStorageOutput, 1)) {
                    handler.onOvervoltage(direction.getOpposite(), incoming);
                }
                var state = level.getBlockState(next);
                if (state.getBlock() instanceof EnergyCableBlock cable && cable.canCarry(state, 0)
                    && !visited.contains(next)) {
                    int nextLoss = route.lossMilliEu() + cable.lossMilliEu();
                    if (nextLoss / MILLIEU_PER_EU < remaining) {
                        MeterVisit meters = cable instanceof EnergyMeterCableBlock meter
                            ? new MeterVisit(meter, next, nextLoss / MILLIEU_PER_EU, route.meters()) : route.meters();
                        pending.add(new Route(next, nextLoss, order++, meters, new CableVisit(cable, next, route.cables())));
                    }
                }
            }
        }
        supplied.forEach((path, energy) -> CableHazards.supplied(level, path, energy));
        return available - remaining;
    }

    private static void recordTransfer(Level level, MeterVisit meters, int accepted, int routeLoss) {
        for (MeterVisit visit = meters; visit != null; visit = visit.previous()) {
            // A meter reports energy leaving its cable, including any loss farther along the route.
            visit.meter().recordTransfer(level, visit.position(), accepted + routeLoss - visit.loss());
        }
    }

    private record Route(BlockPos position, int lossMilliEu, long order, MeterVisit meters, CableVisit cables) {}
    record CableVisit(EnergyCableBlock cable, BlockPos position, CableVisit previous) {}

    private record MeterVisit(EnergyMeterCableBlock meter, BlockPos position, int loss, MeterVisit previous) {}
}
