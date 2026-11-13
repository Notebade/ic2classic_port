package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import ic2.api.energy.EuNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;

final class FisherChecks {
    private static final BlockPos POS = new BlockPos(320, 80, 320);
    private FisherChecks() {}

    static void run(ServerLevel level) {
        var port = POS.east(2);
        try {
            build(level, 5);
            level.setBlockAndUpdate(POS.above(), Blocks.HOPPER.defaultBlockState());
            var entity = level.getBlockEntity(POS);
            ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "formed") == 0, "source structure rejects containers with at most twenty slots");
            level.setBlockAndUpdate(POS.above(), Blocks.CHEST.defaultBlockState());
            ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "formed") == 1 && value(level, "chain_length") == 5 && value(level, "fishing_luck") == 1, "source timber rings, water prism, chest and chain form the fisher");
            require(EuNetwork.portOwner(level, port).orElseThrow().equals(POS), "plain wood ring registers an owned EU port");
            require(Ic2Api.machineInfo(level, port).orElseThrow().blockId().equals(ResourceLocation.parse("ic2:electric_fisher")), "plain-block API resolves the live fisher");
            var distant = new BlockPos(30000000, 80, 30000000);
            require(!level.hasChunkAt(distant) && !EuNetwork.registerPorts(level, POS, List.of(port, distant))
                && !level.hasChunkAt(distant) && EuNetwork.portOwner(level, port).orElseThrow().equals(POS), "invalid replacement is atomic and never loads distant chunks");
            require(send(level, 149) == 149, "EU routes through a vanilla wood block");
            ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "progress_units") == 0 && Ic2Api.machineInfo(level, POS).orElseThrow().energyStored() == 149, "fewer than 150 EU cannot advance fishing");
            send(level, 1); ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "progress_units") == 1, "150 EU advances one tick");
            ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "progress_units") == 0, "starvation rolls progress back by one");
            for (int tick = 0; tick < 150; tick++) { send(level, 150); ApiSmokeChecks.tick(level, POS, entity.getType()); }
            send(level, 150);
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            var stale = (EnergySink) entity;
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            require(EuNetwork.portOwner(level, port).isEmpty() && stale.receiveEnergy(150) == 0, "removal revokes plain ports and stale sink references");
            place(level); entity = level.getBlockEntity(POS); entity.loadWithComponents(saved, level.registryAccess());
            ApiSmokeChecks.tick(level, POS, entity.getType());
            require(value(level, "progress_units") == 151 && EuNetwork.portOwner(level, port).isPresent(), "reload retains progress and restores owned ports");
            long seed = 0x511F15L;
            var expected = expectedLoot(level, 5, seed);
            require(expected.size() >= 32, "source-weighted loot table resolves all vanilla sub-tables");
            for (int tick = 0; tick < 149; tick++) {
                send(level, 150);
                if (tick == 148) level.random.setSeed(seed);
                ApiSmokeChecks.tick(level, POS, entity.getType());
            }
            require(value(level, "progress_units") == 0 && Ic2Api.machineInfo(level, POS).orElseThrow().energyStored() == 0, "one full cycle spends exactly 45000 EU");
            var chest = level.getCapability(Capabilities.ItemHandler.BLOCK, POS.above(), Direction.DOWN);
            for (int slot = 0; slot < chest.getSlots(); slot++) {
                var actual = chest.getStackInSlot(slot);
                int remaining = actual.getCount();
                for (var stack : expected) if (ItemStack.isSameItemSameComponents(stack, actual)) {
                    int consumed = Math.min(remaining, stack.getCount()); remaining -= consumed; stack.shrink(consumed);
                }
                require(remaining == 0, "actual catch contains only results from the expected thirty-two rolls");
            }
            require(expected.stream().allMatch(ItemStack::isEmpty), "actual catch includes every expected item and enchantment from all thirty-two rolls");
            level.setBlockAndUpdate(POS.west(2).north(2), Blocks.AIR.defaultBlockState());
            require(EuNetwork.portOwner(level, port).isEmpty() && Ic2Api.machineInfo(level, port).isEmpty() && send(level, 150) == 0,
                "breaking a remote timber part immediately revokes EU and telemetry access");
            com.mojang.logging.LogUtils.getLogger().info("IC2 electric fisher production checks passed: original structure, plain-block EU ports, exact 32-roll loot, progress reload and broken-link revocation");
        } finally { clear(level); }
    }
    private static int send(ServerLevel level, int packet) { return EuNetwork.send(level, POS.east(3), packet, Set.of(Direction.WEST), 0); }
    private static long value(ServerLevel level, String key) { return Ic2Api.machineInfo(level, POS).orElseThrow().values().get(key); }
    private static void place(ServerLevel level) { level.setBlockAndUpdate(POS, Ic2Api.block(ResourceLocation.parse("ic2:electric_fisher")).orElseThrow().defaultBlockState()); }
    private static void build(ServerLevel level, int depth) {
        for (int y = -depth; y <= 1; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            var pos = POS.offset(x, y, z); level.getChunkAt(pos);
            if (y < 0) level.setBlockAndUpdate(pos, x == 0 && z == 0 ? Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true) : Blocks.WATER.defaultBlockState());
            else if (y == 0 && (x != 0 || z != 0)) level.setBlockAndUpdate(pos, Math.max(Math.abs(x), Math.abs(z)) == 1 ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState());
        }
        place(level);
    }
    private static List<ItemStack> expectedLoot(ServerLevel level, int depth, long seed) {
        var rod = new ItemStack(Items.FISHING_ROD);
        rod.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LUCK_OF_THE_SEA), 3);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, POS.below().getCenter())
            .withParameter(LootContextParams.TOOL, rod).withLuck(depth - 4).create(LootContextParamSets.FISHING);
        var loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("ic2:electric_fisher")));
        var random = RandomSource.create(seed);
        var result = new ArrayList<ItemStack>();
        for (int roll = 0; roll < 32; roll++) for (var stack : loot.getRandomItems(params, random)) result.add(stack.copy());
        return result;
    }
    private static void clear(ServerLevel level) {
        for (int y = -10; y <= 1; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            var pos = POS.offset(x, y, z); if (level.hasChunkAt(pos)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
