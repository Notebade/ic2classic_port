package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;

final class ComparingCableEntity extends ic2.core.equipment.FoamTextureEntity implements MenuProvider {
    static final int MAX_THRESHOLD = 8192;
    static final int MINIMUM = 0;
    static final int MAXIMUM = 1;
    private static final int MAX_SAMPLE = MAX_THRESHOLD * EnergyMeterCableBlock.SAMPLE_INTERVAL;

    private final ContainerData thresholds = new SimpleContainerData(2);
    private int transferred;

    ComparingCableEntity(BlockPos pos, BlockState state) {
        super(Machines.COMPARING_CABLE_ENTITY.get(), pos, state);
        thresholds.set(MAXIMUM, 1);
    }

    boolean recordTransfer(int energy) {
        boolean first = transferred == 0;
        transferred = Math.min(transferred + energy, MAX_SAMPLE);
        setChanged();
        return first;
    }

    int sample() {
        int average = transferred / EnergyMeterCableBlock.SAMPLE_INTERVAL;
        if (transferred > 0) {
            transferred = 0;
            setChanged();
        }
        int minimum = thresholds.get(MINIMUM);
        int range = thresholds.get(MAXIMUM) - minimum;
        return 15 * Mth.clamp(average - minimum, 0, range) / range;
    }

    static boolean validThresholds(int minimum, int maximum) {
        return minimum >= 0 && minimum < maximum && maximum <= MAX_THRESHOLD;
    }

    boolean setThresholds(int minimum, int maximum) {
        if (!validThresholds(minimum, maximum)) {
            return false;
        }
        thresholds.set(MINIMUM, minimum);
        thresholds.set(MAXIMUM, maximum);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("min_value", thresholds.get(MINIMUM));
        tag.putInt("max_value", thresholds.get(MAXIMUM));
        tag.putInt("transferred", transferred);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int minimum = Mth.clamp(tag.getInt("min_value"), 0, MAX_THRESHOLD - 1);
        thresholds.set(MINIMUM, minimum);
        thresholds.set(MAXIMUM, Mth.clamp(tag.getInt("max_value"), minimum + 1, MAX_THRESHOLD));
        transferred = Mth.clamp(tag.getInt("transferred"), 0, MAX_SAMPLE);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public ComparingCableMenu createMenu(int id, Inventory inventory, Player player) {
        return new ComparingCableMenu(id, this, thresholds);
    }
}
