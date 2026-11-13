package ic2.core.machine;

import ic2.api.machine.DirectionalMachineUpgrade;
import ic2.api.machine.UpgradeContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

final class TransportUpgradeItem extends Item implements DirectionalMachineUpgrade {
    enum Kind { ITEM_IMPORT, ITEM_EXPORT, FLUID_IMPORT, FLUID_EXPORT }
    private final Kind kind;
    TransportUpgradeItem(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    @Override public Category category(ItemStack stack) { return Category.TRANSPORT; }
    @Override public Optional<Direction> facing(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int side = tag.contains("upgrade_facing") ? tag.getInt("upgrade_facing") : -1;
        return side >= 0 && side < 6 ? Optional.of(Direction.from3DDataValue(side)) : Optional.empty();
    }
    @Override public void setFacing(ItemStack stack, Optional<Direction> direction) {
        if (stack.getItem() != this) throw new IllegalArgumentException("Configure the upgrade's own ItemStack");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (direction.isPresent()) tag.putInt("upgrade_facing", direction.get().ordinal()); else tag.remove("upgrade_facing");
        });
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        var direction = context.isSecondaryUseActive() ? context.getClickedFace().getOpposite() : context.getClickedFace();
        setFacing(stack, Optional.of(direction));
        if (!context.getLevel().isClientSide) context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("message.ic2.upgrade_direction", "Direction: %s", direction.getName()), false);
        return InteractionResult.SUCCESS;
    }
    private List<BlockPos> neighbors(UpgradeContext context, Direction direction) {
        var ports = context.connectionPositions();
        var owned = java.util.Set.copyOf(ports);
        return ports.stream().map(position -> position.relative(direction)).filter(position -> !owned.contains(position) && context.level().hasChunkAt(position)).distinct().toList();
    }
    @Override public boolean tickUpgrade(ItemStack stack, UpgradeContext context) {
        if (kind == Kind.FLUID_IMPORT || kind == Kind.FLUID_EXPORT) transferFluids(stack, context);
        else {
            boolean working = ic2.api.Ic2Api.machineInfo(context.level(), context.position()).map(snapshot -> snapshot.values().getOrDefault("processing", snapshot.active() ? 1L : 0L) != 0).orElse(false);
            if (!working && context.level().getGameTime() % 20 == 0) transferItems(stack, context);
        }
        return false;
    }
    @Override public void recipeFinished(ItemStack stack, UpgradeContext context, ItemStack result, float experience) {
        if (kind == Kind.ITEM_IMPORT || kind == Kind.ITEM_EXPORT) transferItems(stack, context);
    }
    private void transferItems(ItemStack stack, UpgradeContext context) {
        var direction = facing(stack).orElse(null);
        if (direction == null) return;
        int remaining = Math.min(9, stack.getCount());
        for (var neighbor : neighbors(context, direction)) {
            var external = context.level().getCapability(Capabilities.ItemHandler.BLOCK, neighbor, direction.getOpposite());
            if (external == null) continue;
            var source = kind == Kind.ITEM_IMPORT ? external : context.outputs();
            var target = kind == Kind.ITEM_IMPORT ? context.inputs() : external;
            remaining -= moveItems(source, target, remaining, context);
            if (remaining == 0) return;
        }
    }
    private static int moveItems(IItemHandler source, IItemHandler target, int limit, UpgradeContext context) {
        int moved = 0;
        for (int slot = 0; slot < source.getSlots() && moved < limit; slot++) {
            var offered = source.extractItem(slot, limit - moved, true);
            if (offered.isEmpty()) continue;
            var rejected = ItemHandlerHelper.insertItemStacked(target, offered.copy(), true);
            int accepted = offered.getCount() - rejected.getCount();
            if (accepted <= 0) continue;
            var extracted = source.extractItem(slot, accepted, false);
            int extractedCount = extracted.getCount();
            var remainder = ItemHandlerHelper.insertItemStacked(target, extracted, false);
            moved += extractedCount - remainder.getCount();
            if (!remainder.isEmpty()) {
                var returned = source.insertItem(slot, remainder, false);
                if (!returned.isEmpty()) net.minecraft.world.level.block.Block.popResource(context.level(), context.position(), returned);
            }
        }
        return moved;
    }
    private void transferFluids(ItemStack stack, UpgradeContext context) {
        var direction = facing(stack).orElse(null);
        if (direction == null || context.fluids().isEmpty()) return;
        int remaining = Math.min(90, stack.getCount() * 10);
        for (var neighbor : neighbors(context, direction)) {
            var external = context.level().getCapability(Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite());
            if (external == null) continue;
            var source = kind == Kind.FLUID_IMPORT ? external : context.fluids().get();
            var target = kind == Kind.FLUID_IMPORT ? context.fluids().get() : external;
            remaining -= FluidUtil.tryFluidTransfer(target, source, remaining, true).getAmount();
            if (remaining == 0) return;
        }
    }
}
