package ic2.core;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

final class Materials {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);

    private Materials() {}

    static void register(IEventBus bus, DeferredRegister.Items items) {
        var tinOre = items.registerSimpleBlockItem(BLOCKS.registerBlock("tin_ore", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)));
        var deepslateTinOre = items.registerSimpleBlockItem(BLOCKS.registerBlock("deepslate_tin_ore", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_COPPER_ORE)));
        var machineBlock = items.registerSimpleBlockItem(BLOCKS.registerBlock("machine_block", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
        var advancedMachineBlock = items.registerSimpleBlockItem(BLOCKS.registerBlock("advanced_machine_block", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
        var aluminiumOre = items.registerSimpleBlockItem(BLOCKS.registerBlock("aluminium_ore_nether", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_QUARTZ_ORE).strength(4.0F, 5.0F).requiresCorrectToolForDrops()));
        var rawTin = items.registerSimpleItem("raw_tin");
        var rawAluminium = items.registerSimpleItem("raw_aluminium");
        var carbonFiber = items.registerSimpleItem("carbon_fiber");
        var ironDust = items.registerSimpleItem("dust_iron");
        var goldDust = items.registerSimpleItem("dust_gold");
        var coalDust = items.registerSimpleItem("dust_coal");
        var charcoalDust = items.registerSimpleItem("dust_charcoal");
        var refinedIron = items.registerSimpleItem("ingot_refined_iron");
        var circuit = items.registerSimpleItem("circuit");
        var advancedCircuit = items.registerSimpleItem("advanced_circuit");
        BLOCKS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
                event.accept(tinOre.get());
                event.accept(deepslateTinOre.get());
                event.accept(aluminiumOre.get());
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                event.accept(rawTin.get());
                event.accept(IC2.TIN_INGOT.get());
                event.accept(IC2.TIN_DUST.get());
                event.accept(rawAluminium.get());
                event.accept(IC2.ALUMINIUM_DUST.get());
                event.accept(IC2.ALUMINIUM_INGOT.get());
                event.accept(IC2.BRONZE_INGOT.get());
                event.accept(IC2.ADVANCED_ALLOY.get());
                event.accept(IC2.ADVANCED_ALLOY_PLATE.get());
                event.accept(carbonFiber.get());
                event.accept(IC2.CARBON_MESH.get());
                event.accept(IC2.CARBON_PLATE.get());
                event.accept(IC2.RARE_EARTH_DUST.get());
                event.accept(IC2.RARE_EARTH_CHUNK.get());
                event.accept(IC2.DEAD_MAGNET.get());
                event.accept(ironDust.get());
                event.accept(goldDust.get());
                event.accept(coalDust.get());
                event.accept(charcoalDust.get());
                event.accept(refinedIron.get());
                event.accept(circuit.get());
                event.accept(advancedCircuit.get());
                event.accept(machineBlock.get());
                event.accept(advancedMachineBlock.get());
            }
        });
    }
}
