package ic2.core.machine;

import ic2.core.Radiation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;

final class ReactorHeatEffects {
    private ReactorHeatEffects() {}

    static void apply(Level level, BlockPos core, int heat, int capacity, float containment) {
        if (heat < 4_000 || GenerationConfig.REACTOR_DAMAGE.get() <= 0) {
            return;
        }
        float ratio = (float) heat / capacity;
        if (ratio >= 0.85F && level.random.nextFloat() < 0.2F * containment) {
            BlockPos target = nearby(level, core);
            if (target != null) melt(level, target);
        }
        if (ratio >= 0.7F) {
            Radiation.exposeNearReactor(level, core, containment);
        }
        if (ratio >= 0.5F && level.random.nextFloat() < containment) {
            BlockPos target = nearby(level, core);
            if (target != null) {
                BlockState state = level.getBlockState(target);
                if (state.getFluidState().is(FluidTags.WATER) && state.getBlock() instanceof BucketPickup pickup) {
                    pickup.pickupBlock(null, level, target, state);
                }
            }
        }
        if (ratio >= 0.4F && level.random.nextFloat() < containment) {
            BlockPos target = nearby(level, core);
            if (target != null && burns(level.getBlockState(target))) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private static void melt(Level level, BlockPos target) {
        var state = level.getBlockState(target);
        if (state.isAir()) {
            level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
            return;
        }
        float hardness = state.getDestroySpeed(level, target);
        if (hardness < 0 || hardness > 10 || level.getBlockEntity(target) != null) return;
        boolean mineral = state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.DIRT)
            || state.is(Blocks.CLAY) || state.getFluidState().is(FluidTags.LAVA);
        level.setBlockAndUpdate(target, mineral ? Blocks.LAVA.defaultBlockState() : Blocks.FIRE.defaultBlockState());
    }

    private static boolean burns(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL);
    }

    private static BlockPos nearby(Level level, BlockPos core) {
        BlockPos target = core.offset(level.random.nextInt(5) - 2, level.random.nextInt(5) - 2, level.random.nextInt(5) - 2);
        return target.equals(core) || !level.isLoaded(target) ? null : target;
    }
}
