package ic2.api.machine;

import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Immutable, point-in-time telemetry; quantities use the units documented in API.md. */
public record MachineSnapshot(ResourceLocation blockId, boolean active, long energyStored,
                              long energyCapacity, EnergyUnit energyUnit, Map<String, Long> values) {
    public enum EnergyUnit { EU, FE, NONE }
    public MachineSnapshot {
        Objects.requireNonNull(blockId, "blockId"); Objects.requireNonNull(energyUnit, "energyUnit");
        if (energyStored < 0 || energyCapacity < 0) throw new IllegalArgumentException("Negative energy");
        values = Map.copyOf(values);
    }
    /** Implement on an addon block entity to participate in Ic2Api.machineInfo. */
    public interface Provider {
        MachineSnapshot machineInfo();
    }
    /** Optional telemetry for a linked part whose loaded controller may be unavailable. */
    public interface Access {
        java.util.Optional<MachineSnapshot> resolveMachineInfo();
    }
}
