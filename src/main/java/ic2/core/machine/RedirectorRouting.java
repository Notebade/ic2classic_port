package ic2.core.machine;

import java.util.ArrayDeque;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Packet queues break feedback paths; a slave never accepts new packets while emitting. */
final class RedirectorRouting {
    private final ChargingEntity host;
    final int[] weights = new int[6];
    final ArrayDeque<Integer> pending = new ArrayDeque<>();
    private boolean emitting;
    RedirectorRouting(ChargingEntity host) { this.host = host; }
    private ChargingEntity slave(Direction side) {
        var level = host.getLevel(); var pos = host.getBlockPos().relative(side);
        if (side == host.front() || level == null || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof ChargingEntity device && device.kind == ChargingDevices.Kind.SLAVE
            && device.front() == side.getOpposite() ? device : null;
    }
    int presentMask() { int mask = 0; for (Direction side : Direction.values()) if (slave(side) != null) mask |= 1 << side.ordinal(); return mask; }
    void initialize() {
        if (Arrays.stream(weights).sum() != 0) return;
        for (Direction side : Direction.values()) if (slave(side) != null) { weights[side.ordinal()] = 100; host.setChanged(); break; }
    }
    private boolean canQueue() { return !emitting && host.energy < 32 && pending.size() < 64; }
    boolean ready() {
        initialize();
        for (Direction side : Direction.values()) { ChargingEntity device = slave(side); if (device != null && weights[side.ordinal()] > 0 && device.routing.canQueue()) return true; }
        return false;
    }
    int receive(int offered) {
        if (!ready()) return 0;
        int total = host.energy + offered;
        if (total < 6) { host.energy = total; host.setChanged(); return offered; }
        int budget = Math.min(32768, total); boolean[] ready = new boolean[6]; ChargingEntity[] outputs = new ChargingEntity[6];
        for (Direction side : Direction.values()) {
            int i = side.ordinal(); outputs[i] = slave(side); ready[i] = outputs[i] != null && outputs[i].routing.canQueue();
        }
        int[] shares = ChargingDistribution.split(budget, weights, ready);
        for (int i = 0; i < 6; i++) if (shares[i] > 0) { outputs[i].routing.pending.add(shares[i]); outputs[i].setChanged(); }
        host.energy = total - budget; host.setChanged(); return offered;
    }
    boolean tick() {
        int sent = 0; emitting = true;
        try {
            int old = EnergyNetwork.send(host.getLevel(), host.getBlockPos(), Math.min(32, host.energy), side -> side != host.front(), 0);
            host.energy -= old; sent += old;
            while (!pending.isEmpty()) {
                int packet = pending.removeFirst();
                int used = EnergyNetwork.send(host.getLevel(), host.getBlockPos(), packet, side -> side != host.front(), 0);
                host.energy += packet - used; sent += used; host.setChanged();
            }
        } finally { emitting = false; }
        return sent > 0;
    }
    void button(int id) {
        if (id == 12) { auto(); return; }
        if (id < 0 || id >= 12) return;
        int side = id / 2;
        if (slave(Direction.values()[side]) == null) return;
        int other = -1;
        if (id % 2 == 1) {
            for (int i = 0; i < 6; i++) if (i != side && weights[i] > 0 && (other < 0 || weights[i] > weights[other])) other = i;
            if (other >= 0) { int n = Math.min(5, weights[other]); weights[other] -= n; weights[side] += n; }
        } else {
            for (Direction direction : Direction.values()) if (direction.ordinal() != side && slave(direction) != null) { other = direction.ordinal(); break; }
            if (other >= 0) { int n = Math.min(5, weights[side]); weights[side] -= n; weights[other] += n; }
        }
        host.setChanged();
    }
    private void auto() {
        int mask = presentMask(), count = Integer.bitCount(mask); Arrays.fill(weights, 0);
        int remainder = 100;
        for (int side = 0; side < 6; side++) if ((mask & 1 << side) != 0) { int n = remainder / count--; weights[side] = n; remainder -= n; }
        host.setChanged();
    }
    void save(CompoundTag tag) { tag.putIntArray("shares", weights); tag.putIntArray("packets", pending.stream().mapToInt(Integer::intValue).toArray()); }
    void load(CompoundTag tag) {
        Arrays.fill(weights, 0); int[] saved = tag.getIntArray("shares");
        if (saved.length == 6 && Arrays.stream(saved).allMatch(n -> n >= 0 && n <= 100) && Arrays.stream(saved).sum() == 100) System.arraycopy(saved, 0, weights, 0, 6);
        pending.clear();
        if (host.kind == ChargingDevices.Kind.MASTER) { host.energy = Math.min(5, host.energy); return; }
        for (int packet : tag.getIntArray("packets")) {
            if (pending.size() == 64) break;
            int accepted = Mth.clamp(packet, 0, Math.min(32768, host.kind.capacity - host.stored()));
            if (accepted > 0) pending.add(accepted);
        }
    }
}
