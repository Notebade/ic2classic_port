package ic2.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Source-compatible scanner installed in miners/ranged pumps. Methods must not load chunks. */
public interface WorldScanner {
    /** A free query when useEnergy=false; otherwise debit one scan or return zero. */
    int scanRadius(ItemStack stack, boolean useEnergy);
    boolean isValuable(ItemStack stack, BlockState state, LevelReader level, BlockPos pos);
    default boolean scansFluids() { return false; }
}
