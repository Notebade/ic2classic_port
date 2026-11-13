package example;

import ic2.api.Ic2Api;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class MiningToolChecks {
    static void run(ServerLevel level) {
        for (String name : java.util.List.of("drill", "od_scanner", "ov_scanner", "ov_scanner_ranged", "fluid_scanner"))
            require(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).isPresent(), "working source mining dependency exists: " + name);
        var stack = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:drill")).orElseThrow());
        require(!stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "empty drill cannot harvest ore");
        var item = (ic2.core.item.RechargeableItem) stack.getItem();
        item.setCharge(stack, 100);
        require(stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()) && stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 8, "powered source drill mines stone and iron ore");
        require(!stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "basic drill retains source iron tool tier");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        item.mineBlock(stack, level, Blocks.STONE.defaultBlockState(), new net.minecraft.core.BlockPos(0, 100, 0), player);
        require(item.charge(stack) == 50, "actual manual drilling spends source 50 EU");
        for (String name : java.util.List.of("od_scanner", "ov_scanner", "ov_scanner_ranged", "fluid_scanner")) {
            var tool = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).orElseThrow());
            var scanner = (ic2.api.item.WorldScanner) tool.getItem();
            var battery = (ic2.core.item.RechargeableItem) tool.getItem();
            int expectedRadius = switch (name) { case "od_scanner" -> 2; case "ov_scanner" -> 4; case "ov_scanner_ranged" -> 8; default -> 20; };
            int expectedCost = switch (name) { case "od_scanner" -> 50; case "ov_scanner" -> 250; case "ov_scanner_ranged" -> 750; default -> 100; };
            require(scanner.scanRadius(tool, false) == expectedRadius && scanner.scanRadius(tool, true) == 0, name + " exposes its source range but cannot scan without power");
            battery.setCharge(tool, expectedCost);
            require(scanner.scanRadius(tool, true) == expectedRadius && battery.charge(tool) == 0 && scanner.scanRadius(tool, true) == 0, name + " debits exactly one source scan");
            var state = name.equals("fluid_scanner") ? Blocks.WATER.defaultBlockState() : Blocks.IRON_ORE.defaultBlockState();
            require(scanner.isValuable(tool, state, level, net.minecraft.core.BlockPos.ZERO), name + " recognizes the corresponding world resource");
            require(!scanner.isValuable(tool, Blocks.STONE.defaultBlockState(), level, net.minecraft.core.BlockPos.ZERO), name + " ignores plain stone");
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
