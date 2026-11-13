package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ElectricFishing {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    static final DeferredBlock<ElectricFisherBlock> BLOCK = BLOCKS.registerBlock("electric_fisher", ElectricFisherBlock::new, PowerMachines.properties());
    static final Supplier<BlockEntityType<ElectricFisherEntity>> ENTITY = ENTITIES.register("electric_fisher", () -> BlockEntityType.Builder.of(ElectricFisherEntity::new, BLOCK.get()).build(null));
    private ElectricFishing() {}
    static void register(IEventBus bus, DeferredRegister.Items items) { BLOCKS.register(bus); ENTITIES.register(bus); items.registerSimpleBlockItem(BLOCK); }
}
