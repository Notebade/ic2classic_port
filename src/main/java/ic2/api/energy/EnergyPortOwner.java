package ic2.api.energy;

import net.minecraft.core.BlockPos;

/** Implement on a multiblock controller to receive EU through registered positions without block entities. */
public interface EnergyPortOwner extends EnergySink {
    /** Must verify a live owned port without loading chunks or changing the structure. */
    boolean ownsEnergyPort(BlockPos position);
}
