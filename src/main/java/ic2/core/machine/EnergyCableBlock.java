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

class EnergyCableBlock extends PipeBlock implements net.minecraft.world.level.block.LiquidBlockContainer, net.minecraft.world.level.block.BucketPickup {
    private static final net.minecraft.world.level.block.state.properties.EnumProperty<CableFluid> FLUID = net.minecraft.world.level.block.state.properties.EnumProperty.create("fluid", CableFluid.class);
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
        BlockState state = defaultBlockState().setValue(FLUID, CableFluid.NONE);
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends EnergyCableBlock> codec() {
        return CODEC;
    }

    int lossMilliEu() {
        return lossMilliEu;
    }

    private int maxPacket() {
        return maxPacket;
    }

    boolean canCarry(BlockState state, int packetSize) {
        return packetSize <= maxPacket;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        builder.add(FLUID);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor));
        }
        return state.setValue(FLUID, CableFluid.of(context.getLevel().getFluidState(context.getClickedPos()).getType()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        scheduleFluid(level, pos, state);
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor));
    }

    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) { return state.getValue(FLUID).state(); }
    @Override public boolean canPlaceLiquid(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return state.getValue(FLUID) == CableFluid.NONE && CableFluid.of(fluid) != CableFluid.NONE;
    }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, net.minecraft.world.level.material.FluidState fluid) {
        if (!canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
        if (!level.isClientSide()) {
            var next = state.setValue(FLUID, CableFluid.of(fluid.getType()));
            level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE); scheduleFluid(level, pos, next);
        }
        return true;
    }
    @Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.entity.player.Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        var fluid = state.getValue(FLUID);
        if (fluid == CableFluid.NONE) return net.minecraft.world.item.ItemStack.EMPTY;
        level.setBlock(pos, state.setValue(FLUID, CableFluid.NONE), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return new net.minecraft.world.item.ItemStack(fluid.state().getType().getBucket());
    }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound() { return java.util.Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL); }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(BlockState state) {
        return java.util.Optional.of(state.getValue(FLUID) == CableFluid.LAVA ? net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA : net.minecraft.sounds.SoundEvents.BUCKET_FILL);
    }
    private static void scheduleFluid(LevelAccessor level, BlockPos pos, BlockState state) {
        var fluid = state.getValue(FLUID);
        if (fluid != CableFluid.NONE) level.scheduleTick(pos, fluid.state().getType(), fluid.state().getType().getTickDelay(level));
    }
    private enum CableFluid implements net.minecraft.util.StringRepresentable {
        NONE, WATER, LAVA;
        static CableFluid of(net.minecraft.world.level.material.Fluid fluid) {
            if (fluid == net.minecraft.world.level.material.Fluids.WATER || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER) return WATER;
            return fluid == net.minecraft.world.level.material.Fluids.LAVA || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA ? LAVA : NONE;
        }
        net.minecraft.world.level.material.FluidState state() {
            return this == WATER ? net.minecraft.world.level.material.Fluids.WATER.getSource(false)
                : this == LAVA ? net.minecraft.world.level.material.Fluids.LAVA.getSource(false) : net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    private boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof EnergyCableBlock || state.getBlock() instanceof MachineBlock
            || state.getBlock() instanceof AdvancedMachineBlock || state.getBlock() instanceof PressurePartBlock
            || state.getBlock() instanceof ExtraGeneratorBlock || state.getBlock() instanceof PowerMachineBlock
            || state.getBlock() instanceof ic2.api.energy.EnergyConnectable;
    }
}
