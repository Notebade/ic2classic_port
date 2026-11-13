package ic2.core.machine;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

interface ChemicalEngine {
    record Tick(int energy, boolean active) {}
    Tick tick(ServerLevel level, BlockPos pos, int energy, int clock, ItemStackHandler inventory);
    boolean accepts(int slot, ItemStack stack);
    IItemHandler automation(Direction side);
    IFluidHandler fluids();
    int metric(int index);
    void telemetry(Map<String, Long> values);
    CompoundTag save(HolderLookup.Provider lookup);
    void load(CompoundTag tag, HolderLookup.Provider lookup);
    default boolean button(int button) { return false; }
}
