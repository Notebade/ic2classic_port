package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

final class MiningWorld {
    static ItemStack pipe() { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ic2:miningpipe_slim"))); }
    static BlockPos tip(Level level, BlockPos controller) {
        var pos = controller.below();
        while (pos.getY() >= level.getMinBuildHeight() && level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof MiningPipeBlock) pos = pos.below();
        return pos.above();
    }
    static void install(Level level, BlockPos pos) {
        if (level.getBlockState(pos.above()).getBlock() instanceof MiningPipeBlock)
            level.setBlock(pos.above(), Block.byItem(pipe().getItem()).defaultBlockState(), 2 | 16);
        level.setBlock(pos, ExtractionMachines.PIPE_TIP.get().defaultBlockState(), 2 | 16);
    }
    static void retract(Level level, BlockPos controller, ItemStackHandler inventory, int slot) {
        var pos = tip(level, controller);
        send(level, controller, pipe());
        var fill = inventory.getStackInSlot(slot);
        var state = !fill.is(pipe().getItem()) && fill.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, state, 2 | 16);
        if (!state.isAir()) inventory.extractItem(slot, 1, false);
        if (level.getBlockState(pos.above()).getBlock() instanceof MiningPipeBlock) level.setBlock(pos.above(), ExtractionMachines.PIPE_TIP.get().defaultBlockState(), 2 | 16);
    }
    static void send(Level level, BlockPos controller, ItemStack stack) {
        for (var side : Direction.Plane.HORIZONTAL) {
            var pos = controller.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side.getOpposite());
            if (handler != null) stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            if (stack.isEmpty()) return;
        }
        Containers.dropItemStack(level, controller.getX(), controller.getY(), controller.getZ(), stack);
    }
}
