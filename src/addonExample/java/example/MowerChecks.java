package example;

import ic2.api.Ic2Api;
import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class MowerChecks {
    static void run(ServerLevel level) {
        var mower = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:mowing_tool")).orElseThrow(() -> new AssertionError("source mowing tool exists")));
        var power = (ElectricItem) mower.getItem();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(slot -> player.getInventory().getItem(slot).copy()).toList();
        var position = player.position(); float pitch = player.getXRot(), yaw = player.getYRot();
        long time = level.getGameTime(); var data = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        var center = new BlockPos(510, 100, 510); var protectedPos = center.west();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> protect = event -> { if (event.getPos().equals(protectedPos)) event.setCanceled(true); };
        var overflowDrops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> capture = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                && item.blockPosition().equals(new BlockPos(514, 100, 514)) && item.getItem().is(Blocks.DANDELION.asItem())) overflowDrops.add(item);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protect);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, capture);
        try {
            player.getInventory().clearContent(); player.setItemInHand(InteractionHand.MAIN_HAND, mower);
            player.setPos(510.5, 102, 510.5); player.setXRot(90);
            data.setGameTime(0);
            for (var pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
                level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2 | 16);
                level.setBlock(pos, Blocks.DANDELION.defaultBlockState(), 2 | 16);
            }
            level.setBlock(center.east().below(), Blocks.FARMLAND.defaultBlockState(), 2 | 16);
            level.setBlock(center.east(), Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7), 2 | 16);
            mower.use(level, player, InteractionHand.MAIN_HAND);
            require(((ConfigurableTool) mower.getItem()).mode(mower).equals("on"), "right click enables mowing mode");
            mower.getItem().inventoryTick(mower, level, player, player.getInventory().selected, true);
            require(level.getBlockState(center).is(Blocks.DANDELION), "empty mower cannot harvest");
            power.setCharge(mower, 1000);
            mower.getItem().inventoryTick(mower, level, player, player.getInventory().selected, true);
            require(level.isEmptyBlock(center) && level.getBlockState(protectedPos).is(Blocks.DANDELION) && level.getBlockState(center.east()).is(Blocks.WHEAT), "mower clears weeds but preserves protected blocks and source-excluded crops");
            int flowers = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(player.getInventory()::getItem).filter(stack -> stack.is(Blocks.DANDELION.asItem())).mapToInt(ItemStack::getCount).sum();
            require(flowers == 7 && power.charge(mower) == 965, "mower auto-imports seven drops and spends five EU each");
            var overflow = new BlockPos(514, 100, 514);
            level.setBlock(overflow.below(), Blocks.DIRT.defaultBlockState(), 2 | 16); level.setBlock(overflow, Blocks.DANDELION.defaultBlockState(), 2 | 16);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) if (slot != player.getInventory().selected) player.getInventory().setItem(slot, new ItemStack(Blocks.DIRT, 64));
            player.setPos(514.5, 102, 514.5);
            mower.getItem().inventoryTick(mower, level, player, player.getInventory().selected, true);
            // The isolated fixture has hidden entity sections until the first server tick.
            // Observe the native spawn event instead of querying only visible sections.
            int overflowCount = overflowDrops.stream().filter(entity -> !entity.isRemoved()).mapToInt(entity -> entity.getItem().getCount()).sum();
            require(overflowCount == 1 && power.charge(mower) == 960, "full inventory leaves excess mower loot: count=" + overflowCount + ", EU=" + power.charge(mower) + ", state=" + level.getBlockState(overflow));
            var copper = new BlockPos(520, 100, 520);
            level.setBlock(copper, Blocks.EXPOSED_COPPER.defaultBlockState(), 2 | 16); player.setPos(520.5, 102, 520.5);
            mower.getItem().inventoryTick(mower, level, player, player.getInventory().selected, true);
            require(level.getBlockState(copper).is(Blocks.COPPER_BLOCK) && power.charge(mower) == 960, "mower keeps source axe-scrape behavior");
            player.setPos(530.5, 100, 530.5); player.setXRot(0); player.setYRot(0);
            var target = new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level) {
                @Override public boolean isAlwaysTicking() { return true; }
            };
            target.setPos(530.5, 100, 533.5); level.addFreshEntity(target);
            try {
                mower.getItem().inventoryTick(mower, level, player, player.getInventory().selected, true);
                require(target.getHealth() == 19 && power.charge(mower) == 959, "mower entity ray hit uses its advertised one EU");
            } finally { target.discard(); }
            mower.getItem().mineBlock(mower, level, Blocks.STONE.defaultBlockState(), center, player);
            require(power.charge(mower) == 958, "manual nonzero-hardness mining uses one EU without double charging automatic harvests");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protect);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture);
            data.setGameTime(time);
            for (int slot = 0; slot < saved.size(); slot++) player.getInventory().setItem(slot, saved.get(slot));
            player.setPos(position); player.setXRot(pitch); player.setYRot(yaw);
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
