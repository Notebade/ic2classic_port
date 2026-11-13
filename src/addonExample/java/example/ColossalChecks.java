package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

final class ColossalChecks {
    static final BlockPos ORIGIN = new BlockPos(256, 303, 256);
    private ColossalChecks() {}

    static void run(ServerLevel level) {
        String[][] profiles = {
            {"furnace", "minecraft:raw_iron", "minecraft:iron_ingot"},
            {"macerator", "minecraft:iron_ingot", "ic2:dust_iron"},
            {"extractor", "ic2:sticky_resin", "ic2:rubber"},
            {"compressor", "ic2:ingot_advanced_alloy", "ic2:plate_advanced_alloy"},
            {"recycler", "minecraft:cobblestone", "ic2:scrap"}
        };
        try {
            for (var profile : profiles) for (int edge = 2; edge <= 4; edge++) {
                clear(level);
                build(level, edge, profile[0]);
                var entity = level.getBlockEntity(ORIGIN);
                ApiSmokeChecks.tick(level, ORIGIN, entity.getType());
                int lanes = 1 << (edge - 1);
                var snapshot = Ic2Api.machineInfo(level, ORIGIN).orElseThrow();
                require(snapshot.values().get("formed") == 1 && snapshot.values().get("recipe_lanes") == lanes, "source cubic shape unlocks expected lanes: " + profile[0] + "/" + edge);
                var inputs = level.getCapability(Capabilities.ItemHandler.BLOCK, ORIGIN, Direction.UP);
                var outputs = level.getCapability(Capabilities.ItemHandler.BLOCK, ORIGIN, Direction.DOWN);
                var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, ORIGIN, Direction.UP);
                require(inputs != null && inputs.getSlots() == lanes && outputs != null && fluids != null, "formed controller exposes lane and fluid capabilities");
                for (int lane = 0; lane < lanes; lane++) require(inputs.insertItem(lane, stack(profile[1]), false).isEmpty(), "recipe enters lane " + lane);
                for (int packet = 0; packet < 5; packet++) ((EnergySink) entity).receiveEnergy(2048);
                ApiSmokeChecks.tick(level, ORIGIN, entity.getType());
                require(Ic2Api.machineInfo(level, ORIGIN).orElseThrow().energyStored() == 10240, "dry machine waits without consuming EU");
                require(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), FluidAction.SIMULATE) == 1000
                    && fluids.getFluidInTank(0).isEmpty(), "simulated water fill does not mutate tanks");
                fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), FluidAction.EXECUTE);
                int ticks = profile[0].equals("recycler") ? 1 : 6;
                for (int tick = 0; tick < ticks; tick++) ApiSmokeChecks.tick(level, ORIGIN, entity.getType());
                for (int lane = 0; lane < lanes; lane++) {
                    var result = outputs.getStackInSlot(lane);
                    require(result.is(Ic2Api.item(ResourceLocation.parse(profile[2])).orElseThrow()), "parallel recipe output: " + profile[0] + "/" + edge + "/" + lane);
                }
                snapshot = Ic2Api.machineInfo(level, ORIGIN).orElseThrow();
                require(snapshot.energyStored() == 10240 - lanes * 30L * ticks, "all lanes consume source EU cost");
                require(snapshot.values().get("water_mb") == 1000 - lanes * 50L && snapshot.values().get("steam_mb") == lanes * 25L, "each completed operation converts 50 water into 25 steam");
                var partPos = ORIGIN.west(edge - 1).south(edge - 1).below(edge - 1);
                var linked = Ic2Api.machineInfo(level, partPos).orElseThrow();
                require(linked.blockId().equals(snapshot.blockId()) && linked.energyStored() == snapshot.energyStored(), "part API resolves its live controller");
                var cachedItems = level.getCapability(Capabilities.ItemHandler.BLOCK, partPos, Direction.DOWN);
                var cachedFluids = level.getCapability(Capabilities.FluidHandler.BLOCK, partPos, Direction.UP);
                require(cachedItems != null && cachedFluids != null, "linked parts expose guarded capabilities");
                level.setBlockAndUpdate(ORIGIN.below(), Blocks.AIR.defaultBlockState());
                require(cachedItems.getSlots() == 0 && cachedItems.getStackInSlot(0).isEmpty(), "cached part items stop immediately when another part breaks");
                require(cachedFluids.drain(100, FluidAction.EXECUTE).isEmpty() && Ic2Api.machineInfo(level, partPos).isEmpty(), "broken structure has neither fluid nor API access");
            }
            com.mojang.logging.LogUtils.getLogger().info("IC2 colossal production checks passed: five machines, three sizes, all parallel lanes and stale part access");
        } finally { clear(level); }
    }

    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse(id)).orElseThrow()); }
    static void build(ServerLevel level, int edge, String kind) {
        for (int y = 0; y < edge; y++) for (int z = 0; z < edge; z++) for (int x = 0; x < edge; x++) {
            var pos = ORIGIN.west(x).south(z).below(y);
            level.getChunkAt(pos);
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:colossal_base")).orElseThrow().defaultBlockState());
        }
        level.setBlockAndUpdate(ORIGIN, Ic2Api.block(ResourceLocation.parse("ic2:colossal_" + kind)).orElseThrow().defaultBlockState());
    }
    static void clear(ServerLevel level) {
        for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            var pos = ORIGIN.west(x).south(z).below(y);
            if (level.hasChunkAt(pos)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
