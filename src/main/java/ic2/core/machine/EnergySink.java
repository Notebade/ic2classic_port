package ic2.core.machine;

import net.minecraft.core.Direction;

interface EnergySink {
    boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet);

    int receiveEnergy(int available);
}
