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
    static final net.neoforged.neoforge.registries.DeferredItem<TestHookUpgrade> HOOK_TEST_UPGRADE = ITEMS.registerItem("hook_test_upgrade", TestHookUpgrade::new);
    static final net.neoforged.neoforge.registries.DeferredItem<TestMiningTool> MINING_TEST_TOOL = ITEMS.registerItem("mining_test_tool", p -> new TestMiningTool(p.stacksTo(1)));
    static final net.neoforged.neoforge.registries.DeferredItem<TestScanner> SCANNER_TEST_TOOL = ITEMS.registerItem("scanner_test_tool", p -> new TestScanner(p.stacksTo(1)));
    static final net.neoforged.neoforge.registries.DeferredItem<TestEuItem> EU_TEST_ITEM = ITEMS.registerItem("eu_test_item", p -> new TestEuItem(p.stacksTo(1)));
    public ExampleAddon(IEventBus modBus) {
        if (Ic2Api.version() != 1) throw new IllegalStateException("This addon needs IC2 API v1");
        ITEMS.register(modBus);
        AddonEnergyFixtures.register(modBus);
        if (System.getProperty("ic2.chunkloader.phase") != null)
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ChunkloaderRestartChecks::run);
        modBus.addListener(this::setup);
        if (Boolean.getBoolean("ic2.api.verify")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ApiSmokeChecks::run);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(StructureChunkChecks::tick);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CableHazardChecks::tick);
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

    static final class TestEuItem extends net.minecraft.world.item.Item implements ic2.api.item.ElectricItem {
        TestEuItem(Properties properties) { super(properties); }
        @Override public int capacity() { return 4000; }
        @Override public int transferRate() { return 100; }
        @Override public int minimumPacket() { return 32; }
        @Override public int charge(net.minecraft.world.item.ItemStack stack) { return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("eu"); }
        @Override public void setCharge(net.minecraft.world.item.ItemStack stack, int charge) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("eu", Math.clamp(charge, 0, capacity())));
        }
    }

    static final class TestMiningTool extends net.minecraft.world.item.Item implements ic2.api.item.MiningDrill {
        TestMiningTool(Properties properties) { super(properties); }
        @Override public boolean canUse(net.minecraft.world.item.ItemStack stack) { return true; }
        @Override public boolean canMine(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) { return state.getDestroySpeed(level, pos) >= 0; }
        @Override public int progressPerTick(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state) { return 200; }
        @Override public int machineEuPerTick(net.minecraft.world.item.ItemStack stack) { return 7; }
        @Override public void consumeUse(net.minecraft.world.item.ItemStack stack) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("uses", tag.getInt("uses") + 1));
        }
    }
    static final class TestScanner extends net.minecraft.world.item.Item implements ic2.api.item.WorldScanner {
        TestScanner(Properties properties) { super(properties); }
        @Override public int scanRadius(net.minecraft.world.item.ItemStack stack, boolean consume) {
            if (consume) net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("scans", tag.getInt("scans") + 1));
            return 2;
        }
        @Override public boolean isValuable(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) { return state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES); }
    }

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
    static final class TestHookUpgrade extends net.minecraft.world.item.Item implements ic2.api.machine.MachineUpgrade {
        TestHookUpgrade(Properties properties) { super(properties); }
        @Override public Category category(net.minecraft.world.item.ItemStack stack) { return Category.CUSTOM; }
        @Override public boolean tickUpgrade(net.minecraft.world.item.ItemStack stack, ic2.api.machine.UpgradeContext context) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("upgrade_ticks", tag.getInt("upgrade_ticks") + 1));
            return true;
        }
        @Override public void recipeFinished(net.minecraft.world.item.ItemStack stack, ic2.api.machine.UpgradeContext context,
                net.minecraft.world.item.ItemStack result, float experience) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putInt("upgrade_recipes", tag.getInt("upgrade_recipes") + 1);
                tag.putString("last_result", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
            });
        }
    }
}
