package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

abstract class EnergyMeterCableBlock extends EnergyCableBlock {
    static final int SAMPLE_INTERVAL = 20;

    EnergyMeterCableBlock(Properties properties) {
        super(properties, 500, 2048, 4);
    }

    abstract void recordTransfer(Level level, BlockPos pos, int energy);

    final void scheduleSample(Level level, BlockPos pos) {
        int delay = SAMPLE_INTERVAL - Math.floorMod(level.getGameTime() + pos.hashCode(), SAMPLE_INTERVAL);
        level.scheduleTick(pos, this, delay);
    }

    @Override
    public final boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    protected final boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected final int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }
}
