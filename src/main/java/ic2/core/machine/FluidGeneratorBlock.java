package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

final class FluidGeneratorBlock extends ActiveMachineBlock {
    static final int GEOTHERMAL_OUTPUT = 20;
    static final int GEOTHERMAL_TANK = 24_000;
    static final int WATER_TANK = 4_000;
    static final int WATER_PER_TICK = 2;
    private final Fluid fuel;
    private final int tankCapacity;

    FluidGeneratorBlock(Properties properties, MachineKind kind, Fluid fuel, int tankCapacity) {
        super(properties, kind);
        this.fuel = fuel;
        this.tankCapacity = tankCapacity;
    }

    Fluid fuel() {
        return fuel;
    }

    int tankCapacity() {
        return tankCapacity;
    }

    boolean acceptsFuel(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(fluid -> fluid.getFluid() == fuel).isPresent();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && acceptsFuel(stack)
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
