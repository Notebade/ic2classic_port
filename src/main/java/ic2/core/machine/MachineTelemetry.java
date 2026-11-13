package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class MachineTelemetry {
    private MachineTelemetry() {}
    static MachineSnapshot snapshot(BlockEntity entity, long energy, long capacity, MachineSnapshot.EnergyUnit unit, Map<String, Long> values) {
        var state = entity.getBlockState();
        return new MachineSnapshot(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
            state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT), energy, capacity, unit, values);
    }
}
