package ic2.core.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

class DirectionalMachineBlock extends MachineBlock {
    DirectionalMachineBlock(Properties properties, MachineKind kind) {
        super(properties, kind);
    }

    @Override
    DirectionProperty facingProperty() {
        return BlockStateProperties.FACING;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return defaultBlockState();
        }
        int pitch = Math.round(player.getXRot());
        if (pitch >= 65) {
            return defaultBlockState().setValue(facingProperty(), Direction.UP);
        }
        if (pitch <= -65) {
            return defaultBlockState().setValue(facingProperty(), Direction.DOWN);
        }
        return super.getStateForPlacement(context);
    }
}
