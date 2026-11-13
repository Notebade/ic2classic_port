package ic2.core.machine;

import ic2.api.energy.EnergyPortOwner;
import ic2.api.energy.EuNetwork;
import ic2.api.machine.MachineSnapshot;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

final class ElectricFisherEntity extends BlockEntity implements EnergyPortOwner, EnergySink, MachineSnapshot.Provider {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("ic2", "electric_fisher"));
    private int energy, progress, length;
    private boolean formed, registered;
    ElectricFisherEntity(BlockPos pos, BlockState state) { super(ElectricFishing.ENTITY.get(), pos, state); }

    private boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this; }
    private IItemHandler output() {
        if (!level.hasChunkAt(worldPosition.above())) return null;
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        return handler != null && handler.getSlots() > 20 ? handler : null;
    }
    private boolean waterlogged(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
            || state.getFluidState().isSource() && state.getFluidState().getType() == Fluids.WATER;
    }
    private int structureDepth() {
        if (!live()) return 0;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int radius = Math.max(Math.abs(x), Math.abs(z));
            if (radius == 0) continue;
            var pos = worldPosition.offset(x, 0, z);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(radius == 1 ? BlockTags.PLANKS : BlockTags.LOGS)) return 0;
        }
        int depth = 0;
        for (int offset = 1; offset <= 10; offset++) {
            var pos = worldPosition.below(offset);
            if (!level.hasChunkAt(pos)) return 0;
            var state = level.getBlockState(pos);
            if (!state.is(Blocks.CHAIN) || !waterlogged(state)) break;
            depth++;
        }
        if (!FisherRules.validDepth(depth)) return 0;
        for (int y = 1; y <= depth; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            var pos = worldPosition.offset(x, -y, z);
            if (!level.hasChunkAt(pos) || !waterlogged(level.getBlockState(pos))) return 0;
        }
        return output() == null ? 0 : depth;
    }
    private boolean registerPorts() {
        var ports = new ArrayList<BlockPos>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (x != 0 || z != 0) ports.add(worldPosition.offset(x, 0, z));
        return EuNetwork.registerPorts((ServerLevel) level, worldPosition, ports);
    }
    void releasePorts() {
        if (registered && level instanceof ServerLevel server) EuNetwork.unregisterPorts(server, worldPosition);
        registered = false; formed = false;
    }
    void tick() {
        int depth = structureDepth();
        if (depth == 0) { releasePorts(); setWorking(false); return; }
        formed = true;
        if (length != depth) { length = depth; setChanged(); }
        if (!registered) {
            registered = registerPorts();
            if (!registered) { formed = false; setWorking(false); return; }
        }
        int previous = progress;
        boolean working = energy >= 150;
        progress = FisherRules.nextProgress(progress, energy);
        if (working) energy -= 150;
        if (progress >= 300) { progress = 0; catchFish(); }
        if (working || progress != previous) setChanged();
        setWorking(working);
    }
    private void setWorking(boolean working) {
        if (getBlockState().getValue(BlockStateProperties.LIT) != working)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, working), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void catchFish() {
        var output = output();
        if (output == null) return;
        var server = (ServerLevel) level;
        var rod = new ItemStack(Items.FISHING_ROD);
        rod.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LUCK_OF_THE_SEA), 3);
        var parameters = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, worldPosition.below().getCenter())
            .withParameter(LootContextParams.TOOL, rod).withLuck(FisherRules.luck(length)).create(LootContextParamSets.FISHING);
        var table = server.getServer().reloadableRegistries().getLootTable(LOOT);
        for (int roll = 0; roll < 32; roll++) for (var stack : table.getRandomItems(parameters, level.random))
            ItemHandlerHelper.insertItemStacked(output, stack, false);
    }
    @Override public boolean ownsEnergyPort(BlockPos pos) {
        return formed && live() && pos.getY() == worldPosition.getY() && !pos.equals(worldPosition)
            && Math.abs(pos.getX() - worldPosition.getX()) <= 2 && Math.abs(pos.getZ() - worldPosition.getZ()) <= 2 && structureDepth() == length;
    }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) {
        return live() && formed && structureDepth() == length && packet > 0 && packet <= 512 && energy < 5000;
    }
    @Override public int receiveEnergy(int offered) {
        if (!live() || !formed || structureDepth() != length) return 0;
        int accepted = Math.clamp(offered, 0, Math.min(512, 5000 - energy)); energy += accepted; if (accepted > 0) setChanged(); return accepted;
    }
    @Override public MachineSnapshot machineInfo() {
        boolean valid = formed && structureDepth() == length;
        return MachineTelemetry.snapshot(this, energy, 5000, MachineSnapshot.EnergyUnit.EU,
            Map.of("formed", valid ? 1L : 0L, "chain_length", (long) length, "fishing_luck", (long) FisherRules.luck(length),
                "progress_units", (long) progress, "progress_target", 300L, "loot_rolls_per_cycle", 32L, "input_packet_eu", 512L, "cost_eu", 150L));
    }
    @Override public void setRemoved() {
        if (level != null && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this) releasePorts();
        super.setRemoved();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putInt("progress", progress); tag.putInt("length", length);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); releasePorts(); energy = Math.clamp(tag.getInt("energy"), 0, 5000);
        progress = Math.clamp(tag.getInt("progress"), 0, 299); length = Math.clamp(tag.getInt("length"), 0, 10);
    }
}
