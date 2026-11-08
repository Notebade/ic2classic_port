package ic2.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

final class EnergyCableBlock extends PipeBlock {
    private static final MapCodec<EnergyCableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        propertiesCodec(),
        Codec.intRange(1, 1200).fieldOf("loss_millieu").forGetter(EnergyCableBlock::lossMilliEu),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_packet", Integer.MAX_VALUE)
            .forGetter(EnergyCableBlock::maxPacket),
        Codec.floatRange(1, 8).optionalFieldOf("radius", 2.0F).forGetter(cable -> cable.radius)
    ).apply(instance, EnergyCableBlock::new));
    private final int lossMilliEu;
    private final int maxPacket;
    private final float radius;

    EnergyCableBlock(Properties properties, int lossMilliEu, int maxPacket, float radius) {
        super(radius / 16.0F, properties);
        this.lossMilliEu = lossMilliEu;
        this.maxPacket = maxPacket;
        this.radius = radius;
        BlockState state = defaultBlockState();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<EnergyCableBlock> codec() {
        return CODEC;
    }

    int lossMilliEu() {
        return lossMilliEu;
    }

    int maxPacket() {
        return maxPacket;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor));
    }

    private boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof EnergyCableBlock || state.getBlock() instanceof MachineBlock;
    }
}
