package ic2.core.machine;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

final class CopperNetwork {
    private CopperNetwork() {}

    static int send(Level level, BlockPos source, int available, Predicate<Direction> emitsTo, boolean canChargeStorage) {
        var pending = new ArrayDeque<Route>();
        var visited = new HashSet<BlockPos>();
        pending.add(new Route(source, 0));
        visited.add(source);
        int remaining = available;

        while (!pending.isEmpty()) {
            Route route = pending.removeFirst();
            // Classic floors the accumulated 0.3 EU loss per uninsulated copper cable.
            int loss = route.cables() * 3 / 10;
            for (Direction direction : Direction.values()) {
                if (remaining <= loss) {
                    break;
                }
                if (route.cables() == 0 && !emitsTo.test(direction)) {
                    continue;
                }
                BlockPos next = route.position().relative(direction);
                if (!level.getChunkSource().hasChunk(
                    SectionPos.blockToSectionCoord(next.getX()), SectionPos.blockToSectionCoord(next.getZ()))) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof MachineEntity machine
                    && machine.canReceiveEnergy(direction.getOpposite(), canChargeStorage)) {
                    remaining -= machine.receiveEnergy(remaining - loss) + loss;
                }
                int nextCableCount = route.cables() + 1;
                if (nextCableCount * 3 / 10 < remaining && level.getBlockState(next).is(Machines.COPPER_CABLE.get())
                    && visited.add(next)) {
                    pending.addLast(new Route(next, nextCableCount));
                }
            }
        }
        return available - remaining;
    }

    private record Route(BlockPos position, int cables) {}
}
