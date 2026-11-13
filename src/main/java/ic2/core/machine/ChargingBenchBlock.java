package ic2.core.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

final class ChargingBenchBlock extends ChargingBlock {
    static final IntegerProperty CHARGE_STATE = IntegerProperty.create("charge_state", 0, 3);
    ChargingBenchBlock(Properties properties, ChargingDevices.Kind kind) { super(properties, kind); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(CHARGE_STATE);
    }
}
