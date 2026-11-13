package example;

import ic2.api.Ic2Api;
import ic2.api.machine.DirectionalMachineUpgrade;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

final class TransportUpgradeChecks {
    private TransportUpgradeChecks() {}
    static void run(ServerLevel level) {
        long previousTime = level.getGameTime();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previousPosition = player.position();
        try {
            level.getServer().getWorldData().overworldData().setGameTime((previousTime + 19) / 20 * 20);
            items(level, player);
            fluids(level, player);
            com.mojang.logging.LogUtils.getLogger().info("IC2 transport upgrade checks passed: configured directions, item conservation, blocked outputs and remote multiblock fluid ports");
        } finally { level.getServer().getWorldData().overworldData().setGameTime(previousTime); player.setPos(previousPosition); }
    }
    private static void items(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        var pos = new BlockPos(448, 303, 448);
        level.getChunkAt(pos.west()); level.getChunkAt(pos.east()); player.setPos(pos.getCenter());
        try {
            place(level, pos, "induction_furnace");
            level.setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
            var source = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.west(), Direction.EAST);
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.east(), Direction.WEST);
            source.insertItem(0, new ItemStack(Items.RAW_IRON, 16), false);
            var machine = level.getBlockEntity(pos);
            var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            menu.slots.get(5).safeInsert(stack("upgrade_simple_import", 3));
            menu.slots.get(6).safeInsert(directed("upgrade_simple_export", 3, Direction.EAST));
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(menu.slots.get(1).getItem().isEmpty() && source.getStackInSlot(0).getCount() == 16, "unconfigured upgrade does not pick an arbitrary side");
            var importer = menu.slots.get(5).getItem().copy();
            ((DirectionalMachineUpgrade) importer.getItem()).setFacing(importer, Optional.of(Direction.WEST));
            menu.slots.get(5).setByPlayer(importer);
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(menu.slots.get(1).getItem().getCount() == 3 && source.getStackInSlot(0).getCount() == 13, "three simple import upgrades move three matching items");
            for (int slot = 0; slot < target.getSlots(); slot++) target.insertItem(slot, new ItemStack(Items.COBBLESTONE, 64), false);
            var hot = machine.saveWithoutMetadata(level.registryAccess()); hot.putInt("heat", 10000); hot.putInt("energy", 10000);
            machine.loadWithComponents(hot, level.registryAccess());
            for (int tick = 0; tick < 12; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            var inputs = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            var outputs = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
            require(count(outputs, Items.IRON_INGOT) == 1 && count(target, Items.IRON_INGOT) == 0, "blocked export preserves the produced item");
            require(materials(source) + materials(inputs) + materials(outputs) + materials(target) == 16, "import/crafting/export conserve input equivalents with a full target");
            target.extractItem(0, 64, false);
            for (int tick = 0; tick < 12; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(count(target, Items.IRON_INGOT) == 2 && count(outputs, Items.IRON_INGOT) == 0, "export sends buffered and newly completed outputs after space is available");
            require(materials(source) + materials(inputs) + materials(outputs) + materials(target) == 16, "successful export neither duplicates nor loses items");
        } finally {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        }
    }
    private static void fluids(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        var pos = ColossalChecks.ORIGIN;
        var sourcePos = pos.west(2).south().below();
        var targetPos = pos.east().south().below();
        try {
            ColossalChecks.build(level, 2, "furnace"); player.setPos(pos.getCenter());
            var machine = level.getBlockEntity(pos); ApiSmokeChecks.tick(level, pos, machine.getType());
            place(level, sourcePos, "machine_tank"); place(level, targetPos, "machine_tank");
            var source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, Direction.EAST);
            var target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, Direction.WEST);
            source.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), FluidAction.EXECUTE);
            var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            menu.slots.get(5).safeInsert(directed("fluid_import_upgrade_simple", 9, Direction.WEST));
            menu.slots.get(6).safeInsert(directed("fluid_export_upgrade_simple", 9, Direction.EAST));
            for (int tick = 0; tick < 3; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("water_mb") == 270 && source.getFluidInTank(0).getAmount() == 730,
                "stacked fluid importer moves source-capped 90 mB/t through a remote casing neighbor");
            var inputs = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            inputs.insertItem(0, new ItemStack(Items.RAW_IRON), false); inputs.insertItem(1, new ItemStack(Items.RAW_IRON), false);
            var sink = (ic2.api.energy.EnergySink) machine;
            for (int packet = 0; packet < 5; packet++) sink.receiveEnergy(2048);
            for (int tick = 0; tick < 7; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(target.getFluidInTank(0).getAmount() == 50 && Ic2Api.machineInfo(level, pos).orElseThrow().values().get("steam_mb") == 0,
                "fluid exporter moves both operations' steam through a different remote casing neighbor");
            require(source.getFluidInTank(0).getAmount() + Ic2Api.machineInfo(level, pos).orElseThrow().values().get("water_mb") + target.getFluidInTank(0).getAmount() * 2 == 1000,
                "fluid transport preserves water equivalents across source, machine and destination");
        } finally {
            ColossalChecks.clear(level); level.setBlockAndUpdate(sourcePos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
        }
    }
    private static ItemStack directed(String id, int count, Direction direction) {
        var stack = stack(id, count); var upgrade = (DirectionalMachineUpgrade) stack.getItem();
        upgrade.setFacing(stack, Optional.of(direction)); require(upgrade.facing(stack).orElseThrow() == direction, "public direction setter round trip"); return stack;
    }
    private static ItemStack stack(String id, int count) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow(), count); }
    private static void place(ServerLevel level, BlockPos pos, String id) { level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState()); }
    private static int materials(IItemHandler handler) { return count(handler, Items.RAW_IRON) + count(handler, Items.IRON_INGOT); }
    private static int count(IItemHandler handler, net.minecraft.world.item.Item item) { int count = 0; for (int slot = 0; slot < handler.getSlots(); slot++) if (handler.getStackInSlot(slot).is(item)) count += handler.getStackInSlot(slot).getCount(); return count; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
