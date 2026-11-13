package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

final class MagneticFenceBlock extends FenceBlock {
    static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);

    MagneticFenceBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(CHARGE, 0)); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(CHARGE); }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player) || state.getValue(CHARGE) == 0) return;
        var boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(Items.IRON_BOOTS) && !boots.is(Items.GOLDEN_BOOTS) && !boots.is(Items.CHAINMAIL_BOOTS)
                && !boots.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2", "magnetic_boots")))) return;
        var motion = player.getDeltaMovement();
        double speed = Math.min((motion.y + .075) * (motion.y > 0 ? 1.03 : 1), player.isShiftKeyDown() ? motion.y > 0 ? motion.y - .03 : -.3 : 1.5);
        player.setDeltaMovement(motion.x, speed, motion.z);
        player.hurtMarked = true;
        player.fallDistance = 0;
        level.setBlock(pos, state.setValue(CHARGE, state.getValue(CHARGE) - 1), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
