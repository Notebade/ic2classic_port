package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class TransformerBlock extends DirectionalMachineBlock {
    static final int LV_HIGH_PACKET = 128;
    static final int MV_HIGH_PACKET = 512;
    static final int HV_HIGH_PACKET = 2048;
    static final int LOW_PACKETS = 4;
    private final int highPacket;

    TransformerBlock(Properties properties, MachineKind kind, int highPacket) {
        super(properties, kind);
        this.highPacket = highPacket;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    int packetSize(boolean high) {
        return high ? highPacket : highPacket / LOW_PACKETS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            player.displayClientMessage(machine.transformerStatus(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
