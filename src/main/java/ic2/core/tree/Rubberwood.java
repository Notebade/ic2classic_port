package ic2.core.tree;

import ic2.core.IC2;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Rubberwood {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<TrunkPlacerType<?>> TRUNKS =
        DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, IC2.MOD_ID);
    private static final ResourceKey<ConfiguredFeature<?, ?>> TREE =
        ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "rubber_tree"));
    private static final TreeGrower GROWER = new TreeGrower("ic2:rubber_tree", Optional.empty(), Optional.of(TREE), Optional.empty());

    static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<RubberTrunkPlacer>> TRUNK =
        TRUNKS.register("rubber_trunk", () -> new TrunkPlacerType<>(RubberTrunkPlacer.CODEC));
    static final DeferredBlock<RubberLogBlock> LOG =
        BLOCKS.registerBlock("rubber_wood", RubberLogBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(1.0F, 5.0F).randomTicks());
    static final DeferredBlock<LeavesBlock> LEAVES =
        BLOCKS.registerBlock("rubber_leaves", LeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES));
    static final DeferredBlock<Block> PLANKS = BLOCKS.registerSimpleBlock("rubberwood_planks",
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_YELLOW));
    static final DeferredBlock<SaplingBlock> SAPLING =
        BLOCKS.registerBlock("rubber_sapling", properties -> new SaplingBlock(GROWER, properties),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));

    private Rubberwood() {}

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        TRUNKS.register(bus);
        var log = items.registerSimpleBlockItem(LOG);
        var leaves = items.registerSimpleBlockItem(LEAVES);
        var planks = items.registerSimpleBlockItem(PLANKS);
        var sapling = items.registerSimpleBlockItem(SAPLING);
        var treeTap = items.registerItem("tree_tap", TreeTapItem::new, new Item.Properties().durability(16));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
                event.accept(planks.get());
            }
            if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
                event.accept(log.get());
                event.accept(leaves.get());
                event.accept(sapling.get());
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(treeTap.get());
            }
        });
    }
}
