package ic2.api.energy;

import ic2.core.machine.EnergyNetwork;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class EuNetwork {
    private EuNetwork() {}
    /** Executes one packet. Return value includes cable loss; subtract it from your source buffer.
     * No simulation, chunk loading, or per-tick scheduling is performed. */
    public static int send(ServerLevel level, BlockPos source, int packet, Set<Direction> outputs, int sourceStorageOutput) {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(source, "source");
        var faces = Set.copyOf(outputs);
        if (packet < 0 || packet > 32768 || sourceStorageOutput < 0 || sourceStorageOutput > 32768)
            throw new IllegalArgumentException("EU packet/storage output must be in [0, 32768]");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Send EU on the server thread");
        return packet == 0 || faces.isEmpty() || !level.hasChunkAt(source) ? 0
            : EnergyNetwork.send(level, source, packet, faces::contains, sourceStorageOutput);
    }
}
