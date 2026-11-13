package ic2.core.machine;

import net.minecraft.core.Direction;

interface EnergySink {
    boolean canReceiveEnergy(Direction side, MachineKind source, int packet);

    int receiveEnergy(int available);
}
