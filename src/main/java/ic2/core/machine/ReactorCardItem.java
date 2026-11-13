package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ReactorCardItem extends Item {
    public ReactorCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        MachineEntity target = level.getBlockEntity(pos) instanceof MachineEntity machine && machine.reactorPlanner() != null
            ? machine : findReactor(level, pos);
        if (player == null || target == null) {
            return InteractionResult.PASS;
        }
        interact(player, context.getItemInHand(), target);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    void interact(Player player, ItemStack card, MachineEntity machine) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        var planner = machine.reactorPlanner();
        if (planner != null) {
            transferPlan(player, card, planner);
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            tag.putString("reactor_dimension", level.dimension().location().toString());
            tag.putLong("reactor_position", machine.getBlockPos().asLong());
        });
        player.displayClientMessage(machine.reactorStatus(), true);
    }

    private void transferPlan(Player player, ItemStack card, ReactorPlannerState planner) {
        var registries = player.level().registryAccess();
        if (player.isShiftKeyDown()) {
            var tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains("reactor_plan")) {
                planner.loadLayout(registries, tag.getCompound("reactor_plan"));
                player.displayClientMessage(Component.translatable("gui.ic2.reactor_card.loaded"), true);
            } else {
                player.displayClientMessage(Component.translatable("gui.ic2.reactor_card.empty"), true);
            }
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> tag.put("reactor_plan", planner.saveLayout(registries)));
            player.displayClientMessage(Component.translatable("gui.ic2.reactor_card.saved"), true);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            MachineEntity reactor = null;
            if (tag.getString("reactor_dimension").equals(level.dimension().location().toString())) {
                reactor = findReactor(level, BlockPos.of(tag.getLong("reactor_position")));
            }
            player.displayClientMessage(reactor == null ? Component.translatable("gui.ic2.reactor_card.unavailable")
                : reactor.reactorStatus(), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static MachineEntity findReactor(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (level.getBlockState(pos).getBlock() instanceof ReactorChamberBlock) {
            return ReactorChamberBlock.reactor(level, pos);
        }
        return level.getBlockEntity(pos) instanceof MachineEntity machine && machine.isReactor() ? machine : null;
    }
    static Component linkedStatus(Level level, ItemStack card) {
        var tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var reactor = tag.getString("reactor_dimension").equals(level.dimension().location().toString()) && tag.contains("reactor_position")
            ? findReactor(level, BlockPos.of(tag.getLong("reactor_position"))) : null;
        return reactor == null ? Component.translatable("gui.ic2.reactor_card.unavailable") : reactor.reactorStatus();
    }
}
