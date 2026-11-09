package ic2.core.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

final class SteamTurbine extends FluidTank {
    static final int CAPACITY = 1_000;
    static final int BUFFER = 4_000;
    static final int PACKET = 128;
    private static final int TOP_SPEED = 200;
    private static final int MAX_PRODUCTION = 64;
    private final Runnable changed;
    private int speed;

    SteamTurbine(Runnable changed) {
        super(CAPACITY, SteamFluids::isSteam);
        this.changed = changed;
    }

    int generate(boolean bufferFull) {
        int previous = speed;
        int consumed = bufferFull ? 0 : super.drain(PACKET, FluidAction.EXECUTE).getAmount();
        int comparison = Integer.compare(consumed * TOP_SPEED, PACKET * speed);
        speed = Math.clamp(speed + comparison, 0, TOP_SPEED);
        if (previous != speed) {
            changed.run();
        }
        return speed * MAX_PRODUCTION / TOP_SPEED;
    }

    boolean spinning() {
        return speed > 0;
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        return isFluidValid(stack) ? super.fill(new FluidStack(SteamFluids.steam(), stack.getAmount()), action) : 0;
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    protected void onContentsChanged() {
        changed.run();
    }

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = writeToNBT(registries, new CompoundTag());
        tag.putInt("speed", speed);
        return tag;
    }

    void load(HolderLookup.Provider registries, CompoundTag tag) {
        readFromNBT(registries, tag);
        speed = Math.clamp(tag.getInt("speed"), 0, TOP_SPEED);
    }
}
