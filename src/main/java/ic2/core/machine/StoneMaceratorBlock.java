package ic2.core.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

final class StoneMaceratorBlock extends AbstractFurnaceBlock {
    private static final MapCodec<StoneMaceratorBlock> CODEC = simpleCodec(StoneMaceratorBlock::new);

    StoneMaceratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<StoneMaceratorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneMaceratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createFurnaceTicker(level, type, Machines.STONE_MACERATOR_ENTITY.get());
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof StoneMaceratorBlockEntity macerator) {
            player.openMenu(macerator);
        }
    }
}
