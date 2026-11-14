package ic2.core.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Half;

/** IC2's vanilla wrench handlers, including inverted fronts and stair halves. */
final class VanillaWrenchRotation {
    private VanillaWrenchRotation() {}
    /** Null means unsupported; the same state means a refused or redundant rotation. */
    static BlockState rotate(BlockState state, Direction target) {
        var block = state.getBlock();
        if (block instanceof StairBlock) return target.getAxis().isVertical()
            ? state.setValue(StairBlock.HALF, target == Direction.UP ? Half.TOP : Half.BOTTOM)
            : state.setValue(StairBlock.FACING, target.getOpposite());
        if (block instanceof GlazedTerracottaBlock || block instanceof RepeaterBlock || block instanceof ComparatorBlock)
            return target.getAxis().isHorizontal() ? state.setValue(HorizontalDirectionalBlock.FACING, target.getOpposite()) : state;
        if (block instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) return state;
        if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AbstractFurnaceBlock)
            return target.getAxis().isHorizontal() ? state.setValue(HorizontalDirectionalBlock.FACING, target) : state;
        if (block instanceof PistonBaseBlock) return state.getValue(PistonBaseBlock.EXTENDED) ? state : state.setValue(PistonBaseBlock.FACING, target);
        if (block instanceof ObserverBlock || block instanceof DispenserBlock) return state.setValue(DirectionalBlock.FACING, target);
        if (block instanceof HopperBlock) return target == Direction.UP ? state : state.setValue(HopperBlock.FACING, target);
        if (block instanceof RotatedPillarBlock && state.is(net.minecraft.tags.BlockTags.LOGS)
                && block.builtInRegistryHolder().key().location().getNamespace().equals("minecraft"))
            return state.setValue(RotatedPillarBlock.AXIS, target.getAxis());
        return null;
    }
}
