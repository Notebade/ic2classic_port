package ic2.api.energy;

import net.minecraft.core.Direction;

/** Implement on a block entity to receive EU from the IC2 cable network. */
public interface EnergySink {
    /** Pure acceptance check. side is the receiver face; packet includes source energy before cable loss.
     * sourceStorageOutput is zero for generators/transformers, or the source storage's nominal output. */
    boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet);
    /** Execute receipt, returning accepted EU in [0, available]. Called on the server thread. */
    int receiveEnergy(int available);
}
