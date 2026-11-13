package ic2.api.internal;

import ic2.api.energy.EnergyPortOwner;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class EnergyPortRegistry {
    private static final Map<ServerLevel, Map<BlockPos, BlockPos>> LEVELS = new WeakHashMap<>();
    private EnergyPortRegistry() {}
    private static void check(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Access energy ports on the server thread");
    }
    public static Optional<EnergyPortOwner> resolve(ServerLevel level, BlockPos port) {
        check(level); Objects.requireNonNull(port, "port");
        var entries = LEVELS.get(level);
        if (entries == null || !level.hasChunkAt(port)) return Optional.empty();
        var owner = entries.get(port);
        if (owner == null || !level.hasChunkAt(owner)) return Optional.empty();
        var entity = level.getBlockEntity(owner);
        return entity instanceof EnergyPortOwner sink && !entity.isRemoved() && sink.ownsEnergyPort(port) ? Optional.of(sink) : Optional.empty();
    }
    public static Optional<BlockPos> owner(ServerLevel level, BlockPos port) {
        return resolve(level, port).isPresent() ? Optional.of(LEVELS.get(level).get(port)) : Optional.empty();
    }
    public static boolean register(ServerLevel level, BlockPos owner, Collection<BlockPos> ports) {
        check(level); Objects.requireNonNull(owner, "owner");
        var requested = ports.stream().map(position -> Objects.requireNonNull(position, "port").immutable()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!level.hasChunkAt(owner)) return false;
        var controller = level.getBlockEntity(owner);
        if (!(controller instanceof EnergyPortOwner sink) || controller.isRemoved()) return false;
        var entries = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        for (var port : requested) {
            if (level.isOutsideBuildHeight(port) || !level.hasChunkAt(port) || !sink.ownsEnergyPort(port)) return false;
            var previous = entries.get(port);
            if (previous != null && !previous.equals(owner) && (!level.hasChunkAt(previous) || resolve(level, port).isPresent())) return false;
        }
        entries.entrySet().removeIf(entry -> entry.getValue().equals(owner));
        for (var port : requested) entries.put(port, owner.immutable());
        return true;
    }
    public static void unregister(ServerLevel level, BlockPos owner) {
        check(level); Objects.requireNonNull(owner, "owner");
        var entries = LEVELS.get(level);
        if (entries != null) entries.entrySet().removeIf(entry -> entry.getValue().equals(owner));
    }
}
