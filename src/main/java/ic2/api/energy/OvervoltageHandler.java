package ic2.api.energy;

import net.minecraft.core.Direction;

/** Optional voltage-sensitive sink. Acceptance must be monotone for positive packet sizes. */
public interface OvervoltageHandler extends EnergySink {
    /** Called instead of receipt when one EU is accepted but the actual incoming packet is rejected.
     * For this contract, packet checks use EU after cable loss. Handle your own fuse/damage behavior.
     * No energy is recorded as received by this callback. */
    void onOvervoltage(Direction side, int packet);
}
