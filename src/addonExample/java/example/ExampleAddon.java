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
    private static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS = net.neoforged.neoforge.registries.DeferredRegister.createItems(MOD_ID);
    static final net.neoforged.neoforge.registries.DeferredItem<TestElectricItem> ELECTRIC_TEST_ITEM = ITEMS.registerItem("electric_test_item", p -> new TestElectricItem(p.stacksTo(1)));
    static final net.neoforged.neoforge.registries.DeferredItem<TestPassiveUpgrade> PASSIVE_TEST_UPGRADE = ITEMS.registerItem("passive_test_upgrade", TestPassiveUpgrade::new);
    public ExampleAddon(IEventBus modBus) {
        if (Ic2Api.version() != 1) throw new IllegalStateException("This addon needs IC2 API v1");
        ITEMS.register(modBus);
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

    static final class TestElectricItem extends net.minecraft.world.item.Item implements ic2.api.item.ElectricEnchantable {
        TestElectricItem(Properties properties) { super(properties); }
        @Override public boolean acceptsElectricEnchantment(net.minecraft.world.item.ItemStack stack,
                net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
            return enchantment.is(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY);
        }
    }
    static final class TestPassiveUpgrade extends net.minecraft.world.item.Item implements ic2.api.machine.MachineUpgrade {
        TestPassiveUpgrade(Properties properties) { super(properties); }
        @Override public int extraEnergyStorage(net.minecraft.world.item.ItemStack stack) { return 2500; }
    }
}
