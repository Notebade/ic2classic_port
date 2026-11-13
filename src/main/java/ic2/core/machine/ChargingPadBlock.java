package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class ChargingPadBlock extends ChargingBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    ChargingPadBlock(Properties properties, ChargingDevices.Kind kind) { super(properties, kind); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.LIT); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide && entity instanceof LivingEntity && level.getBlockEntity(pos) instanceof ChargingEntity pad) pad.activatePad();
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity && level.getBlockEntity(pos) instanceof ChargingEntity pad) pad.activatePad();
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        float maxImpact = kind == ChargingDevices.Kind.PAD_LV ? 3.5F : kind == ChargingDevices.Kind.PAD_MV ? 2.5F : 1.5F;
        if (!level.isClientSide && entity instanceof LivingEntity && distance > maxImpact) {
            int tier = kind == ChargingDevices.Kind.PAD_LV ? 1 : kind == ChargingDevices.Kind.PAD_MV ? 2 : kind == ChargingDevices.Kind.PAD_HV ? 3 : 4;
            entity.hurt(ElectricHazards.source(level), 3 * tier);
        }
    }
}
