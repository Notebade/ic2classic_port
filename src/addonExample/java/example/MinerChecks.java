package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class MinerChecks {
    static void run(ServerLevel level) {
        var pos = new BlockPos(480, 300, 480);
        level.getChunkAt(pos); level.getChunkAt(pos.west());
        try {
            var block = Ic2Api.block(ResourceLocation.parse("ic2:miner"));
            require(block.isPresent(), "working miner is present in the original ranged-pump crafting chain");
            level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.IRON_ORE.defaultBlockState());
            level.setBlockAndUpdate(pos.below(2), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var entity = level.getBlockEntity(pos);
            var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            var drill = stack("drill"); ((ic2.core.item.RechargeableItem) drill.getItem()).setCharge(drill, 10000);
            menu.slots.get(3).safeInsert(drill); menu.slots.get(2).safeInsert(stack("miningpipe_slim"));
            ((EnergySink) entity).receiveEnergy(1000);
            for (int tick = 0; tick < 100; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos); entity.loadWithComponents(saved, level.registryAccess());
            menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            for (int tick = 0; tick < 101; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos.west());
            require(chest.getItem(0).is(Items.RAW_IRON) && chest.getItem(0).getCount() == 1, "miner outputs exactly one raw iron after a mid-cycle reload");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 790, "source miner spends 200 progress EU plus 10 for its pipe");
            require(level.getBlockState(pos.below()).getBlock() instanceof ic2.core.machine.MiningPipeBlock && menu.slots.get(2).getItem().isEmpty(), "miner installs one real pipe and consumes one ingredient");
            var installed = menu.slots.get(3).getItem();
            require(((ic2.core.item.RechargeableItem) installed.getItem()).charge(installed) == 9950, "miner separately spends 50 EU from the drill");
            menu.slots.get(3).setByPlayer(ItemStack.EMPTY);
            for (int tick = 0; tick < 5; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(level.getBlockState(pos.below()).isAir() && level.getBlockState(pos.below(2)).is(Blocks.BEDROCK), "removing the drill retracts its pipe without touching bedrock");
            require(java.util.stream.IntStream.range(0, chest.getContainerSize()).mapToObj(chest::getItem).anyMatch(item -> item.is(stack("miningpipe_slim").getItem())), "retraction returns the pipe into the adjacent chest");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos); menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.east().below(), Blocks.IRON_ORE.defaultBlockState());
            menu.slots.get(3).safeInsert(new ItemStack(ExampleAddon.MINING_TEST_TOOL.get()));
            menu.slots.get(1).safeInsert(new ItemStack(ExampleAddon.SCANNER_TEST_TOOL.get()));
            menu.slots.get(2).safeInsert(stack("miningpipe_slim").copyWithCount(2));
            ((EnergySink) entity).receiveEnergy(1000);
            for (int tick = 0; tick < 4; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(level.getBlockState(pos.east().below()).isAir(), "real addon drill and scanner discover and mine a lateral ore");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 976, "addon work rate and machine EU are respected: 7+10 plus 7");
            var used = menu.slots.get(3).getItem().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            require(used.getInt("uses") == 2, "addon drill receives exactly two completed-use callbacks");
            level.setBlockAndUpdate(pos.east().below(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos); menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            menu.slots.get(3).safeInsert(new ItemStack(ExampleAddon.MINING_TEST_TOOL.get())); menu.slots.get(2).safeInsert(stack("miningpipe_slim"));
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState()); ((EnergySink) entity).receiveEnergy(1000);
            java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> protection = event -> { if (event.getPos().equals(pos.below())) event.setCanceled(true); };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protection);
            try {
                for (int tick = 0; tick < 20; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
                require(level.getBlockState(pos.below()).is(Blocks.STONE) && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 1000, "cancelled protection event prevents world damage and machine EU consumption");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protection); }
            for (int tick = 0; tick < 2; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(level.getBlockState(pos.below()).getBlock() instanceof ic2.core.machine.MiningPipeBlock, "miner resumes when the protection veto is removed");
            for (String pumpName : java.util.List.of("pump", "overclocked_pump")) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
                entity = level.getBlockEntity(pos); menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
                menu.slots.get(3).safeInsert(new ItemStack(ExampleAddon.MINING_TEST_TOOL.get())); menu.slots.get(2).safeInsert(stack("miningpipe_slim"));
                level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState()); ((EnergySink) entity).receiveEnergy(1000);
                level.setBlockAndUpdate(pos.east(), Ic2Api.block(ResourceLocation.parse("ic2:" + pumpName)).orElseThrow().defaultBlockState());
                var pump = level.getBlockEntity(pos.east()); boolean basic = pumpName.equals("pump");
                for (int packet = 0; packet < (basic ? 4 : 2); packet++)
                    require(ic2.api.energy.EuNetwork.send(level, pos.east(2), basic ? 32 : 100, java.util.Set.of(net.minecraft.core.Direction.WEST), 0) == (basic ? 32 : 100), "pump receives its valid source voltage");
                for (int tick = 0; tick < (basic ? 120 : 25); tick++) ApiSmokeChecks.tick(level, pos.east(), pump.getType());
                for (int tick = 0; tick < 2; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
                var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos.east(), net.minecraft.core.Direction.UP);
                require(tank.getFluidInTank(0).getAmount() == 1000 && level.getBlockState(pos.below()).getBlock() instanceof ic2.core.machine.MiningPipeBlock, "miner hands one world source to prepared " + pumpName + " before extending its pipe");
                level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            }
        } finally { for (var at : java.util.List.of(pos, pos.below(), pos.below(2), pos.west(), pos.east(), pos.east().below())) level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState()); }
    }
    private static ItemStack stack(String name) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
