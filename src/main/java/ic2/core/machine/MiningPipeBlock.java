package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shaft crafting component. Like the original, it cannot be placed by hand. */
public final class MiningPipeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    private final boolean slim;
    public MiningPipeBlock(Properties properties) { this(properties, true); }
    public MiningPipeBlock(Properties properties, boolean slim) { super(properties); this.slim = slim; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return slim ? SHAPE : net.minecraft.world.phys.shapes.Shapes.block();
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return false; }
}
