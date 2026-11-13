package example;

import ic2.api.Ic2Api;
import ic2.api.machine.MachineSnapshot;
import ic2.api.recipe.Ic2Recipes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Compile-checked example; excluded from the IC2 mod JAR. */
@Mod(ExampleAddon.MOD_ID)
public final class ExampleAddon {
    public static final String MOD_ID = "exampleaddon";
    public ExampleAddon(IEventBus modBus) {
        if (Ic2Api.version() != 1) throw new IllegalStateException("This addon needs IC2 API v1");
        modBus.addListener(this::setup);
        if (Boolean.getBoolean("ic2.api.verify")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ApiSmokeChecks::run);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(StructureChunkChecks::tick);
        }
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Ic2Recipes.addCompression(id("deepslate"), Items.COBBLED_DEEPSLATE, 1, Items.DEEPSLATE);
            Ic2Recipes.addExtraction(id("moss_slime"), Items.MOSS_BLOCK, Items.SLIME_BALL, 2);
        });
    }
    public static Optional<MachineSnapshot> readMachine(ServerLevel level, BlockPos pos) {
        return Ic2Api.machineInfo(level, pos);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
