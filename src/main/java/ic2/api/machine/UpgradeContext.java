package ic2.api.machine;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** Live, server-thread machine access during an installed upgrade callback. Do not retain this context. */
public record UpgradeContext(ServerLevel level, BlockPos position, IItemHandler inputs, IItemHandler outputs,
                             Optional<IFluidHandler> fluids) {
    public interface ConnectionProvider { java.util.List<BlockPos> upgradeConnectionPositions(); }
    public UpgradeContext {
        Objects.requireNonNull(level, "level"); position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(inputs, "inputs"); Objects.requireNonNull(outputs, "outputs"); Objects.requireNonNull(fluids, "fluids");
    }
    public java.util.List<BlockPos> connectionPositions() {
        return level.hasChunkAt(position) && level.getBlockEntity(position) instanceof ConnectionProvider provider
            ? java.util.List.copyOf(provider.upgradeConnectionPositions()) : java.util.List.of(position);
    }
}
