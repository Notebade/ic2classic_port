package ic2.core.machine;

import ic2.api.item.PrecisionWrench;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

final class WrenchItem extends Item {
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, "ic2");
    private static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> SOUND =
        SOUNDS.register("tools.wrench", net.minecraft.sounds.SoundEvent::createVariableRangeEvent);
    static void register(net.neoforged.bus.api.IEventBus bus) { SOUNDS.register(bus); }
    WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnMachine(context, cost -> true, cost ->
            stack.hurtAndBreak(cost, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand())));
    }

    static InteractionResult useOnMachine(UseOnContext context, IntPredicate canPay, IntConsumer pay) {
        return useOnMachine(context, canPay, pay, false);
    }

    static InteractionResult useOnMachine(UseOnContext context, IntPredicate canPay, IntConsumer pay, boolean precision) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        Direction target = selectedFace(context, player.isShiftKeyDown());
        var recovery = PrecisionWrench.dismantling(state.getBlock());
        var hook = precision ? PrecisionWrench.rotation(state.getBlock()) : null;
        if (hook != null) {
            var rotated = hook.rotate(state, context, target);
            if (rotated == null || !rotated.is(state.getBlock())) return InteractionResult.PASS;
            if (rotated != state || recovery == null) return rotateExternal(context, canPay, pay, state, rotated);
        }
        if (recovery != null) return remove(context, canPay, pay);
        var vanilla = VanillaWrenchRotation.rotate(state, target);
        if (vanilla != null) {
            if (vanilla == state && state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock)
                return remove(context, canPay, pay);
            return rotateExternal(context, canPay, pay, state, vanilla);
        }
        if (!(state.getBlock() instanceof MachineBlock || state.getBlock() instanceof AdvancedMachineBlock
                || state.getBlock() instanceof PressurePartBlock || state.getBlock() instanceof ExtraGeneratorBlock
                || state.getBlock() instanceof MonitorBlock || state.getBlock() instanceof FluidPipeBlock || state.getBlock() instanceof PoweredLampBlock
                || state.getBlock() instanceof ChargingBlock || state.getBlock() instanceof PowerMachineBlock || state.getBlock() instanceof PowerPartBlock || state.getBlock() instanceof PowerTankBlock)) {
            return InteractionResult.PASS;
        }

        var facing = state.getBlock() instanceof MachineBlock machine ? machine.facingProperty()
            : state.getBlock() instanceof PowerMachineBlock || state.getBlock() instanceof ChargingBlock || state.getBlock() instanceof PoweredLampBlock ? net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
            : state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                ? net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
                : net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        boolean hasFacing = state.hasProperty(facing);
        if (state.getBlock() instanceof PowerMachineBlock block && (block.kind.multiblock() || block.kind == PowerMachines.Kind.WINDMILL)
                && target.getAxis().isVertical()) return InteractionResult.PASS;
        if (hasFacing && !facing.getPossibleValues().contains(target)) {
            return InteractionResult.PASS;
        }
        boolean rotate = hasFacing && state.getValue(facing) != target;
        if (rotate && state.getBlock() instanceof ChargingBlock block && !block.canFace(level, pos, target)) return InteractionResult.PASS;
        if (!rotate) return remove(context, canPay, pay);
        if (!canPay.test(1)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            boolean changed = server.setBlockAndUpdate(pos, state.setValue(facing, target));
            if (!changed) {
                return InteractionResult.PASS;
            }
            pay.accept(1);
            playSound(context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult rotateExternal(UseOnContext context, IntPredicate canPay, IntConsumer pay,
                                                     BlockState state, BlockState rotated) {
        if (rotated == state || !canPay.test(1)) return InteractionResult.PASS;
        var level = context.getLevel();
        if (level instanceof ServerLevel server) {
            if (!server.setBlockAndUpdate(context.getClickedPos(), rotated)) return InteractionResult.PASS;
            pay.accept(1);
            playSound(context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult remove(UseOnContext context, IntPredicate canPay, IntConsumer pay) {
        var stack = context.getItemInHand();
        boolean protectedLoss = stack.getItem() instanceof ElectricWrenchItem wrench && wrench.canOverrideLoss(stack);
        if (!canPay.test(protectedLoss ? 100 : 10)) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel server
                && !dismantle(server, context.getClickedPos(), server.getBlockState(context.getClickedPos()), context, pay)) return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static boolean dismantle(ServerLevel level, BlockPos pos, BlockState state, UseOnContext context, IntConsumer pay) {
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, context.getPlayer())).isCanceled()) {
            return false;
        }
        var stack = context.getItemInHand();
        var policy = PrecisionWrench.dismantling(state.getBlock());
        var wrench = stack.getItem() instanceof ElectricWrenchItem item ? item : null;
        double chance = (policy == null ? 1 : policy.recoveryChance()) * (wrench == null ? 1 : wrench.recoveryMultiplier(stack));
        boolean recovered = level.random.nextDouble() <= chance;
        boolean preventedLoss = !recovered && wrench != null && wrench.canOverrideLoss(stack);
        var drops = policy != null && (recovered || preventedLoss) ? policy.intactDrops().apply(context)
            : Block.getDrops(state, level, pos, level.getBlockEntity(pos), context.getPlayer(), stack);
        if (!level.removeBlock(pos, false)) {
            return false;
        }
        pay.accept(10);
        if (preventedLoss) { wrench.preventedLoss(stack, context.getPlayer()); pay.accept(200); }
        playSound(context);
        drops.forEach(drop -> Block.popResource(level, pos, drop));
        return true;
    }

    private static void playSound(UseOnContext context) {
        context.getLevel().playSound(null, context.getPlayer().blockPosition(), SOUND.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
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
