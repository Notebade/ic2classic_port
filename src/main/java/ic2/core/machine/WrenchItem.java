package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class WrenchItem extends Item {
    WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachineBlock machine) || player == null || !player.mayBuild()
            || !level.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }

        var facing = machine.facingProperty();
        Direction target = selectedFace(context, player.isShiftKeyDown());
        boolean hasFacing = !(machine instanceof SolarPanelBlock);
        if (hasFacing && !facing.getPossibleValues().contains(target)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            boolean rotate = hasFacing && state.getValue(facing) != target;
            boolean changed = rotate
                ? server.setBlockAndUpdate(pos, state.setValue(facing, target))
                : dismantle(server, pos, state, context);
            if (!changed) {
                return InteractionResult.PASS;
            }
            stack.hurtAndBreak(rotate ? 1 : 10, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean dismantle(ServerLevel level, BlockPos pos, BlockState state, UseOnContext context) {
        var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), context.getPlayer(), context.getItemInHand());
        if (!level.removeBlock(pos, false)) {
            return false;
        }
        drops.forEach(drop -> Block.popResource(level, pos, drop));
        return true;
    }

    private static Direction selectedFace(UseOnContext context, boolean opposite) {
        Direction face = context.getClickedFace();
        Vec3 hit = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()));
        return switch (face.getAxis()) {
            case X -> faceOrEdge(face, opposite, hit.y, Direction.UP, hit.z, Direction.SOUTH);
            case Y -> faceOrEdge(face, opposite, hit.x, Direction.EAST, hit.z, Direction.SOUTH);
            case Z -> faceOrEdge(face, opposite, hit.y, Direction.UP, hit.x, Direction.EAST);
        };
    }

    private static Direction faceOrEdge(Direction face, boolean opposite, double first, Direction firstDirection,
                                        double second, Direction secondDirection) {
        double firstDistance = Math.abs(first);
        double secondDistance = Math.abs(second);
        if (Math.max(firstDistance, secondDistance) <= 0.3) {
            return opposite ? face.getOpposite() : face;
        }
        if (firstDistance >= secondDistance) {
            return first > 0 ? firstDirection : firstDirection.getOpposite();
        }
        return second > 0 ? secondDirection : secondDirection.getOpposite();
    }
}
