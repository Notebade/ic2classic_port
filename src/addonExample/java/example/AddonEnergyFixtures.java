package example;

import ic2.api.energy.EnergyConnectable;
import ic2.api.energy.EnergySink;
import ic2.api.energy.OvervoltageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class AddonEnergyFixtures {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ExampleAddon.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExampleAddon.MOD_ID);
    static final net.neoforged.neoforge.registries.DeferredBlock<TestBlock> LEGACY = BLOCKS.registerBlock("legacy_sink", p -> new TestBlock(p, false));
    static final net.neoforged.neoforge.registries.DeferredBlock<TestBlock> FUSE = BLOCKS.registerBlock("fused_sink", p -> new TestBlock(p, true));
    private static final java.util.function.Supplier<BlockEntityType<TestSink>> LEGACY_ENTITY = ENTITIES.register("legacy_sink", () -> BlockEntityType.Builder.of((pos, state) -> new TestSink(pos, state, false), LEGACY.get()).build(null));
    private static final java.util.function.Supplier<BlockEntityType<TestSink>> FUSE_ENTITY = ENTITIES.register("fused_sink", () -> BlockEntityType.Builder.<TestSink>of(FusedSink::new, FUSE.get()).build(null));
    static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
    static final class TestBlock extends Block implements EntityBlock, EnergyConnectable {
        private final boolean fused;
        TestBlock(Properties properties, boolean fused) { super(properties); this.fused = fused; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return fused ? new FusedSink(pos, state) : new TestSink(pos, state, false); }
    }
    static class TestSink extends BlockEntity implements EnergySink {
        int stored;
        TestSink(BlockPos pos, BlockState state, boolean fused) { super(fused ? FUSE_ENTITY.get() : LEGACY_ENTITY.get(), pos, state); }
        @Override public boolean canReceiveEnergy(Direction side, int source, int packet) { return packet > 0 && packet <= 32 && stored < 1000; }
        @Override public int receiveEnergy(int amount) { int accepted = Math.clamp(amount, 0, 1000 - stored); stored += accepted; return accepted; }
    }
    static final class FusedSink extends TestSink implements OvervoltageHandler {
        int trippedPacket;
        FusedSink(BlockPos pos, BlockState state) { super(pos, state, true); }
        @Override public boolean canReceiveEnergy(Direction side, int source, int packet) { return trippedPacket == 0 && super.canReceiveEnergy(side, source, packet); }
        @Override public void onOvervoltage(Direction side, int packet) { trippedPacket = packet; }
    }
}
