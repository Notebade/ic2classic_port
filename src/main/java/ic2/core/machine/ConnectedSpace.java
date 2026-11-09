package ic2.core.machine;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class ConnectedSpace {
    private ConnectedSpace() {}

    static int count(Level level, BlockPos origin, BoundingBox bounds, int limit, Predicate<BlockPos> passable) {
        var pending = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        pending.add(origin);
        visited.add(origin);
        int count = 0;
        while (!pending.isEmpty()) {
            BlockPos current = pending.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (bounds.isInside(next) && visited.add(next) && level.isLoaded(next) && passable.test(next)) {
                    if (++count == limit) {
                        return count;
                    }
                    pending.add(next);
                }
            }
        }
        return count;
    }
}
