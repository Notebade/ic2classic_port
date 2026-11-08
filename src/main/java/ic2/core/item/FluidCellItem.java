package ic2.core.item;

import ic2.core.IC2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public final class FluidCellItem extends Item {
    private final Fluid fluid;

    public FluidCellItem(Properties properties, Fluid fluid) {
        super(properties);
        this.fluid = fluid;
    }

    public static ItemStack filledStack(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return new ItemStack(IC2.CELL_WATER.get());
        }
        if (fluid == Fluids.LAVA) {
            return new ItemStack(IC2.CELL_LAVA.get());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (fluid != Fluids.EMPTY) {
            return InteractionResultHolder.pass(held);
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(held);
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), held)) {
            return InteractionResultHolder.fail(held);
        }
        BlockState state = level.getBlockState(pos);
        FluidState source = state.getFluidState();
        ItemStack filled = filledStack(source.getType());
        if (!source.isSource() || filled.isEmpty() || !(state.getBlock() instanceof BucketPickup pickup)) {
            return InteractionResultHolder.pass(held);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(held);
        }
        if (pickup.pickupBlock(player, level, pos, state).isEmpty()) {
            return InteractionResultHolder.pass(held);
        }
        pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1, 1));
        return InteractionResultHolder.consume(ItemUtils.createFilledResult(held, player, filled));
    }

    public static final class Handler extends FluidBucketWrapper {
        public Handler(ItemStack stack) {
            super(stack);
        }

        @Override
        public FluidStack getFluid() {
            Fluid fluid = ((FluidCellItem) container.getItem()).fluid;
            return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        }

        @Override
        public boolean canFillFluidType(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER || stack.getFluid() == Fluids.LAVA;
        }

        @Override
        protected void setFluid(FluidStack stack) {
            container = stack.isEmpty() ? new ItemStack(IC2.CELL_EMPTY.get()) : filledStack(stack.getFluid());
        }
    }
}
