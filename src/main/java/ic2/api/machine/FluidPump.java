package ic2.api.machine;

import net.neoforged.neoforge.fluids.FluidStack;

/** Server-side handoff from world miners to a prepared pump; does not remove the world source. */
public interface FluidPump {
    /** Accept the entire amount only. Simulation never changes progress or the tank. */
    boolean pumpFluid(FluidStack fluid, boolean simulate);
}
