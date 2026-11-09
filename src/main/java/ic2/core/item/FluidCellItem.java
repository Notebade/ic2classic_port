package ic2.core.item;

import ic2.core.IC2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
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
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return fluid == Fluids.WATER;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return fluid == Fluids.WATER ? new ItemStack(IC2.CELL_EMPTY.get()) : ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ClipContext.Fluid clip = fluid == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, clip);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(held);
        }
        BlockPos pos = hit.getBlockPos();
        BlockPos adjacent = pos.relative(hit.getDirection());
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(adjacent, hit.getDirection(), held)) {
            return InteractionResultHolder.fail(held);
        }
        if (fluid != Fluids.EMPTY) {
            return pour(level, player, held, hit, adjacent);
        }
        return collect(level, player, held, pos);
    }

    private InteractionResultHolder<ItemStack> pour(Level level, Player player, ItemStack held, BlockHitResult hit,
            BlockPos adjacent) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        BlockPos target = state.getBlock() instanceof LiquidBlockContainer container
            && container.canPlaceLiquid(player, level, pos, state, fluid) ? pos : adjacent;
        BucketItem bucket = (BucketItem) fluid.getBucket();
        if (!bucket.emptyContents(player, level, target, hit, held)) {
            return InteractionResultHolder.fail(held);
        }
        ItemStack result = player.hasInfiniteMaterials() ? held
            : ItemUtils.createFilledResult(held, player, new ItemStack(IC2.CELL_EMPTY.get()));
        return InteractionResultHolder.sidedSuccess(result, level.isClientSide);
    }

    private static InteractionResultHolder<ItemStack> collect(Level level, Player player, ItemStack held, BlockPos pos) {
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
