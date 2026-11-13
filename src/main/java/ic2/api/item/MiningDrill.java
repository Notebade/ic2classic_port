package ic2.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Installable world-mining tool; implementations own their charge/durability accounting. */
public interface MiningDrill {
    boolean canUse(ItemStack stack);
    boolean canMine(ItemStack stack, BlockState state, LevelReader level, BlockPos pos);
    void consumeUse(ItemStack stack);
    default int progressPerTick(ItemStack stack, BlockState state) { return 1; }
    default int machineEuPerTick(ItemStack stack) { return 1; }
}
