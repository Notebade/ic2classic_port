package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import ic2.api.machine.MachineSnapshot;
import ic2.api.recipe.Ic2Recipes;
import ic2.api.recipe.ProcessingRecipe.Process;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in runtime checks for an isolated dev-server world, never included in IC2 itself. */
public final class ApiSmokeChecks {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private ApiSmokeChecks() {}
    public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var compression = Ic2Recipes.find(Process.COMPRESSION, new ItemStack(Items.COBBLED_DEEPSLATE)).orElseThrow();
            require(compression.id().equals(ResourceLocation.parse("exampleaddon:deepslate")) && compression.output() == Items.DEEPSLATE, "addon compression is used by the machine recipe lookup");
            var extraction = Ic2Recipes.find(Process.EXTRACTION, new ItemStack(Items.MOSS_BLOCK)).orElseThrow();
            require(extraction.output() == Items.SLIME_BALL && extraction.outputCount() == 2, "addon extraction keeps output count");
            require(Ic2Recipes.find(Process.COMPRESSION, new ItemStack(Items.COPPER_INGOT)).orElseThrow().inputCount() == 8, "builtin recipe survives addon registration");
            require(Ic2Api.item(ResourceLocation.parse("ic2:missing_api_test_item")).isEmpty(), "unknown registry ID does not resolve to air");
            try {
                Ic2Recipes.addExtraction(ResourceLocation.parse("exampleaddon:late"), Items.NETHERRACK, Items.SLIME_BALL, 1);
                throw new AssertionError("late registration was accepted");
            } catch (IllegalStateException expected) { /* registry is correctly frozen */ }
            var level = server.overworld();
            var distant = new BlockPos(10_000_000, 300, 10_000_000);
            require(!level.hasChunkAt(distant), "fixture distant chunk must be unloaded");
            require(Ic2Api.machineInfo(level, distant).isEmpty() && !level.hasChunkAt(distant), "query never loads a chunk");
            var pos = new BlockPos(0, 300, 0);
            level.getChunkAt(pos); // Explicit test-fixture loading, outside the API query.
            require(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()), "fixture positions must be empty");
            try {
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:plasmafier")).orElseThrow().defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Ic2Api.block(ResourceLocation.parse("ic2:generator")).orElseThrow().defaultBlockState());
                var before = Ic2Api.machineInfo(level, pos).orElseThrow();
                require(before.energyStored() == 0 && before.energyCapacity() == 5120000 && before.energyUnit() == MachineSnapshot.EnergyUnit.EU, "snapshot reports actual machine capacity and unit");
                int spent = EuNetwork.send(level, pos.above(), 128, Set.of(Direction.DOWN), 0);
                require(spent == 128 && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 128, "public EU API transfers energy into a real IC2 machine");
                require(before.energyStored() == 0, "old snapshot stays unchanged");
                try { before.values().put("mutation", 1L); throw new AssertionError("mutable snapshot"); }
                catch (UnsupportedOperationException expected) { /* immutable snapshot */ }
            } finally {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            }
            LOGGER.info("IC2 API runtime smoke checks passed");
        } catch (Throwable error) {
            LOGGER.error("IC2 API runtime smoke checks FAILED", error);
        } finally {
            server.halt(false);
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
