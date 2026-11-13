package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

final class PumpTank extends FluidTank {
    static final int BUCKET = 1000;

    private final Runnable changed;

    PumpTank(Runnable changed) {
        super(4 * BUCKET);
        this.changed = changed;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    protected void onContentsChanged() {
        changed.run();
    }

    boolean collect(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) {
            return false;
        }
        var bucket = new FluidStack(fluid.getType(), BUCKET);
        if (super.fill(bucket, FluidAction.SIMULATE) != BUCKET || pickup.pickupBlock(null, level, pos, state).isEmpty()) {
            return false;
        }
        super.fill(bucket, FluidAction.EXECUTE);
        return true;
    }

    boolean acceptCollected(FluidStack fluid, boolean simulate) {
        if (fluid.isEmpty() || super.fill(fluid, FluidAction.SIMULATE) != fluid.getAmount()) return false;
        if (!simulate) super.fill(fluid, FluidAction.EXECUTE);
        return true;
    }
}
