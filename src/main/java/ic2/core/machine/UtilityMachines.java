package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class UtilityMachines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, IC2.MOD_ID);
    static final Supplier<net.minecraft.sounds.SoundEvent> MAGNETIZER_SOUND = SOUNDS.register("magnetizer_operating", () ->
        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "magnetizer_operating")));
    static final DeferredBlock<UtilityMachineBlock> MAGNETIZER = BLOCKS.registerBlock("magnetizer", p -> new UtilityMachineBlock(p, false), PowerMachines.properties());
    static final DeferredBlock<UtilityMachineBlock> TESLA = BLOCKS.registerBlock("tesla_coil", p -> new UtilityMachineBlock(p, true), PowerMachines.properties());
    static final Supplier<BlockEntityType<UtilityMachineEntity>> ENTITY = ENTITIES.register("utility_machine", () ->
        BlockEntityType.Builder.of(UtilityMachineEntity::new, MAGNETIZER.get(), TESLA.get()).build(null));

    private UtilityMachines() {}

    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        items.registerSimpleBlockItem(MAGNETIZER);
        items.registerSimpleBlockItem(TESLA);
    }
}
