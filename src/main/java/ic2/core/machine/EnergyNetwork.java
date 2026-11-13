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
        if (available <= 0 || !level.hasChunkAt(source)) return 0;
        var destinations = destinations(level, source, available, emitsTo, sourceStorageOutput);
        var supplied = new java.util.IdentityHashMap<Destination, Integer>();
        var active = new java.util.ArrayList<>(destinations);
        int remaining = available;
        while (remaining > 0 && !active.isEmpty()) {
            active.removeIf(path -> !current(level, path) || !wants(path, sourceStorageOutput, available));
            if (active.isEmpty()) break;
            double inverseLoss = active.stream().mapToDouble(path -> 1 / path.weight()).sum();
            int budget = remaining;
            var next = new java.util.ArrayList<Destination>();
            for (var path : active) {
                int gross = (int) Math.floor(Math.round(budget / inverseLoss / path.weight() * 100000.0) / 100000.0);
                int providing = Math.min(gross - path.loss(), remaining - path.loss());
                if (providing <= 0) { next.add(path); continue; }
                int accepted = transfer(level, path, providing, available, sourceStorageOutput);
                if (accepted > 0) {
                    remaining -= accepted + path.loss();
                    recordTransfer(level, path.route().meters(), accepted, path.loss());
                    supplied.merge(path, accepted, Integer::sum);
                }
                if (accepted == providing && wants(path, sourceStorageOutput, available)) next.add(path);
            }
            // Source fallback: remove the last/most expensive route when rounding cannot move a packet.
            if (remaining == budget && !next.isEmpty()) next.removeLast();
            active = next;
        }
        for (var path : destinations) {
            var amount = supplied.get(path);
            if (amount != null && path.route().cables() != null) CableHazards.supplied(level, path.route().cables(), amount);
        }
        return available - remaining;
    }

    private static java.util.List<Destination> destinations(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, int sourceStorageOutput) {
        var pending = new PriorityQueue<>(Comparator.comparingInt(Route::lossMilliEu).thenComparingLong(Route::order));
        var visited = new HashSet<BlockPos>();
        var owners = new java.util.IdentityHashMap<Object, Destination>();
        var destinations = new java.util.ArrayList<Destination>();
        long order = 0;
        pending.add(new Route(source, 0, order++, null, null));

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
                if (available <= loss) {
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
                var machine = sink(level, next);
                if (!next.equals(source) && machine != null) {
                    var path = new Destination(next, direction.getOpposite(), machine, route);
                    Object owner = machine instanceof EnergySink nativeSink ? nativeSink.networkOwner() : machine;
                    if (owner != null && wants(path, sourceStorageOutput, available) && owners.putIfAbsent(owner, path) == null) destinations.add(path);
                }
                var state = level.getBlockState(next);
                if (state.getBlock() instanceof EnergyCableBlock cable && cable.canCarry(state, 0)
                    && !visited.contains(next)) {
                    int nextLoss = (int) Math.min(Integer.MAX_VALUE, (long) route.lossMilliEu() + cable.lossMilliEu());
                    if (nextLoss / MILLIEU_PER_EU < available) {
                        MeterVisit meters = cable instanceof EnergyMeterCableBlock meter
                            ? new MeterVisit(meter, next, nextLoss / MILLIEU_PER_EU, route.meters()) : route.meters();
                        pending.add(new Route(next, nextLoss, order++, meters, new CableVisit(cable, next, route.cables())));
                    }
                }
            }
        }
        return destinations;
    }

    private static ic2.api.energy.EnergySink sink(Level level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        return entity instanceof ic2.api.energy.EnergySink sink ? sink : level instanceof net.minecraft.server.level.ServerLevel server
            ? ic2.api.internal.EnergyPortRegistry.resolve(server, pos).orElse(null) : null;
    }
    private static boolean wants(Destination path, int sourceStorageOutput, int available) {
        return path.sink().canReceiveEnergy(path.side(), sourceStorageOutput, path.sink() instanceof ic2.api.energy.OvervoltageHandler ? 1 : available);
    }
    private static boolean current(Level level, Destination path) {
        if (!level.hasChunkAt(path.position()) || sink(level, path.position()) != path.sink()) return false;
        for (var visit = path.route().cables(); visit != null; visit = visit.previous()) {
            if (!level.hasChunkAt(visit.position())) return false;
            var state = level.getBlockState(visit.position());
            if (!state.is(visit.cable()) || !visit.cable().canCarry(state, 0)) return false;
        }
        return true;
    }
    private static int transfer(Level level, Destination path, int providing, int offered, int sourceStorageOutput) {
        if (!current(level, path)) return 0;
        int voltage = path.sink() instanceof ic2.api.energy.OvervoltageHandler ? providing : offered;
        if (path.sink().canReceiveEnergy(path.side(), sourceStorageOutput, voltage))
            return Math.clamp(path.sink().receiveEnergy(providing), 0, providing);
        if (path.sink() instanceof ic2.api.energy.OvervoltageHandler handler && wants(path, sourceStorageOutput, offered)) handler.onOvervoltage(path.side(), voltage);
        return 0;
    }

    private static void recordTransfer(Level level, MeterVisit meters, int accepted, int routeLoss) {
        for (MeterVisit visit = meters; visit != null; visit = visit.previous()) {
            // A meter reports energy leaving its cable, including any loss farther along the route.
            visit.meter().recordTransfer(level, visit.position(), accepted + routeLoss - visit.loss());
        }
    }

    private record Route(BlockPos position, int lossMilliEu, long order, MeterVisit meters, CableVisit cables) {}
    private record Destination(BlockPos position, Direction side, ic2.api.energy.EnergySink sink, Route route) {
        int loss() { return route.lossMilliEu() / MILLIEU_PER_EU; }
        double weight() { return Math.max(0.1, route.lossMilliEu() / (double) MILLIEU_PER_EU); }
    }
    record CableVisit(EnergyCableBlock cable, BlockPos position, CableVisit previous) {}

    private record MeterVisit(EnergyMeterCableBlock meter, BlockPos position, int loss, MeterVisit previous) {}
}
