package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.ItemStackHandler;

final class MonitorEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    private int energy;
    private boolean requireRedstone = true;
    List<Component> lines = List.of();
    final ItemStackHandler cards = new ItemStackHandler(8){

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ReactorCardItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    final ContainerData data = new ContainerData(){

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int get(int i) {
            return i == 0 ? energy : requireRedstone ? 1 : 0;
        }

        @Override
        public void set(int i, int v) {
        }
    };

    MonitorEntity(BlockPos p, BlockState s) {
        super(NuclearInfrastructure.MONITOR_ENTITY.get(), p, s);
    }

    void tick() {
        boolean active = energy > 0 && (!requireRedstone || level.hasNeighborSignal(worldPosition));
        if (active) {
            energy--;
            setChanged();
        }
        boolean stateChanged = getBlockState().getValue(BlockStateProperties.LIT) != active;
        if (stateChanged) level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
        if (stateChanged || level.getGameTime() % 5 == 0) {
            var next = new ArrayList<Component>();
            if (active) for (int i = 0; i < 8; i++) if (!cards.getStackInSlot(i).isEmpty()) next.add(ReactorCardItem.linkedStatus(level, cards.getStackInSlot(i)));
            if (!lines.equals(next)) {
                lines = List.copyOf(next);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    void toggleRedstone() {
        requireRedstone = !requireRedstone;
        setChanged();
    }

    @Override
    public boolean canReceiveEnergy(Direction side, int output, int packet) {
        return packet > 0 && packet <= 8192 && energy < 1000;
    }

    @Override
    public int receiveEnergy(int amount) {
        int used = Math.max(0, Math.min(amount, 1000 - energy));
        energy += used;
        if (used > 0) setChanged();
        return used;
    }

    @Override
    public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, 1000, MachineSnapshot.EnergyUnit.EU, Map.of("cards", (long)java.util.stream.IntStream.range(0, 8).filter((i)->!cards.getStackInSlot(i).isEmpty()).count(), "requires_redstone", requireRedstone ? 1L : 0L));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ic2.monitor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MonitorMenu(id, inv, cards, data, ContainerLevelAccess.create(level, worldPosition), this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("energy", energy);
        tag.putBoolean("redstone", requireRedstone);
        tag.put("cards", cards.serializeNBT(lookup));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        energy = Math.clamp(tag.getInt("energy"), 0, 1000);
        requireRedstone = !tag.contains("redstone") || tag.getBoolean("redstone");
        if (tag.contains("cards")) {
            var saved = tag.getCompound("cards").copy();
            saved.putInt("Size", 8);
            cards.deserializeNBT(lookup, saved);
        }
        if (tag.contains("line_count")) {
            var next = new ArrayList<Component>();
            for (int i = 0; i < Math.clamp(tag.getInt("line_count"), 0, 8); i++) {
                var line = Component.Serializer.fromJson(tag.getString("line_" + i), lookup);
                if (line != null) next.add(line);
            }
            lines = List.copyOf(next);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        var tag = saveWithoutMetadata(lookup);
        tag.putInt("line_count", lines.size());
        for (int i = 0; i < lines.size(); i++) tag.putString("line_" + i, Component.Serializer.toJson(lines.get(i), lookup));
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
