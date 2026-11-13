package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import ic2.api.energy.EuNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;

final class PowerTierChecks {
    private static final BlockPos POS = new BlockPos(548, 300, 548);
    static void run(ServerLevel level) {
        level.getChunkAt(POS);
        try {
            for (String tier : java.util.List.of("lv", "mv", "hv")) {
                int packet = tier.equals("lv") ? 32 : tier.equals("mv") ? 128 : 512;
                place(level, POS, tier + "_flux_generator");
                require(EuNetwork.send(level, POS.south(), packet, java.util.Set.of(Direction.NORTH), 0) == packet, tier + " converter accepts source EU tier");
                var entity = level.getBlockEntity(POS); var old = level.getCapability(Capabilities.EnergyStorage.BLOCK, POS, Direction.NORTH);
                require(old != null && old.getEnergyStored() == packet * 4 && old.extractEnergy(packet * 4, true) == packet * 4
                    && Ic2Api.machineInfo(level, POS).orElseThrow().energyStored() == packet * 4, tier + " converter exposes exact FE and pure extraction simulation");
                var tag = entity.saveWithoutMetadata(level.registryAccess());
                level.setBlock(POS, Blocks.AIR.defaultBlockState(), 2 | 16); place(level, POS, tier + "_flux_generator");
                level.getBlockEntity(POS).loadWithComponents(tag, level.registryAccess());
                require(old.extractEnergy(Integer.MAX_VALUE, false) == 0 && !old.canExtract(), "cached FE access must stop after converter removal");
                var fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, POS, Direction.NORTH);
                require(fe.extractEnergy(packet * 4, false) == packet * 4 && fe.getEnergyStored() == 0 && fe.receiveEnergy(100, false) == 0,
                    tier + " converter preserves FE over reload, extracts once and rejects reverse conversion");
                require(level.getCapability(Capabilities.EnergyStorage.BLOCK, POS, Direction.WEST) == null, tier + " FE output is restricted to the front");
                level.setBlock(POS, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            place(level, POS, "transformer_iv"); place(level, POS.east(), "pesu");
            require(EuNetwork.send(level, POS.north(), 32768, java.util.Set.of(Direction.SOUTH), 0) == 32768, "IV transformer accepts its high-side packet");
            ApiSmokeChecks.tick(level, POS, level.getBlockEntity(POS).getType());
            require(energy(level, POS.east()) == 32768 && energy(level, POS) == 0, "IV step-down emits four 8192-EU packets without loss");
            level.setBlock(POS.east(), Blocks.AIR.defaultBlockState(), 2 | 16); place(level, POS.north(), "pesu");
            level.setBlockAndUpdate(POS.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            for (int i = 0; i < 4; i++) require(EuNetwork.send(level, POS.east(), 8192, java.util.Set.of(Direction.WEST), 0) == 8192, "IV step-up accepts a low-side packet");
            ApiSmokeChecks.tick(level, POS, level.getBlockEntity(POS).getType());
            require(energy(level, POS.north()) == 32768 && energy(level, POS) == 0, "redstone IV step-up emits one 32768-EU high-side packet");
            clear(level);
            place(level, POS, "adjustable_transformer"); place(level, POS.east(), "pesu");
            var entity = level.getBlockEntity(POS); var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level); var previous = player.position();
            try {
                player.setPos(POS.getCenter()); var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
                for (int i = 0; i < 5; i++) require(menu.clickMenuButton(player, 1), "real adjustable-transformer packet control");
                require(menu.clickMenuButton(player, 5), "real adjustable-transformer packet-count control");
                ((EnergySink) entity).receiveEnergy(2048);
                var saved = entity.saveWithoutMetadata(level.registryAccess()); level.setBlock(POS, Blocks.AIR.defaultBlockState(), 2 | 16); place(level, POS, "adjustable_transformer");
                entity = level.getBlockEntity(POS); entity.loadWithComponents(saved, level.registryAccess());
                var info = Ic2Api.machineInfo(level, POS).orElseThrow();
                require(info.values().get("packet_eu") == 1024 && info.values().get("packets_per_tick") == 2, "adjustable settings survive a real controller reload");
                ApiSmokeChecks.tick(level, POS, entity.getType());
                require(energy(level, POS.east()) == 2048 && energy(level, POS) == 0, "adjustable transformer emits two configured packets");
            } finally { player.setPos(previous); }
            clear(level);
            for (String cable : java.util.List.of("super_cable", "plasma_cable")) {
                int packet = cable.equals("super_cable") ? 8192 : 32768;
                place(level, POS, cable); place(level, POS.east(), "pesu");
                require(EuNetwork.send(level, POS.west(), packet, java.util.Set.of(Direction.EAST), 0) == packet
                    && energy(level, POS.east()) == packet - (cable.equals("plasma_cable") ? 1 : 0), "real " + cable + " carries its full tier with exact accumulated loss");
                require(!level.getBlockState(POS).isAir(), "rated cable survives its supported packet"); clear(level);
            }
            place(level, POS, "pesu"); ((EnergySink) level.getBlockEntity(POS)).receiveEnergy(1000000000);
            var saved = level.getBlockEntity(POS).saveWithoutMetadata(level.registryAccess()); level.setBlock(POS, Blocks.AIR.defaultBlockState(), 2 | 16); place(level, POS, "pesu");
            level.getBlockEntity(POS).loadWithComponents(saved, level.registryAccess());
            require(energy(level, POS) == 1000000000, "PESU preserves all one billion EU across reload");
        } finally { clear(level); }
    }
    private static void place(ServerLevel level, BlockPos pos, String name) {
        var state = Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
        level.setBlock(pos, state, 2 | 16);
    }
    private static long energy(ServerLevel level, BlockPos pos) { return Ic2Api.machineInfo(level, pos).orElseThrow().energyStored(); }
    private static void clear(ServerLevel level) { for (var pos : java.util.List.of(POS, POS.north(), POS.south(), POS.east(), POS.west())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
