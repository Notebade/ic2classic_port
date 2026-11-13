package ic2.core.machine;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

final class StorageEntity extends BlockEntity implements MenuProvider, EnergySink, ic2.api.machine.MachineSnapshot.Provider {
    private UUID owner;
    private int energy;
    final ItemStackHandler inventory = new ItemStackHandler(personal() ? 54 : 9){

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    StorageEntity(BlockPos pos, BlockState state) {
        super(StorageInfrastructure.ENTITY.get(), pos, state);
    }

    boolean personal() {
        return ((StorageBlock)getBlockState().getBlock()).personal;
    }

    void owner(Player player) {
        if (personal()) {
            owner = player.getUUID();
            setChanged();
        }
    }

    boolean allowed(Player player) {
        return !personal() || owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    @Override
    public ic2.api.machine.MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, personal() ? 0 : 500, personal() ? ic2.api.machine.MachineSnapshot.EnergyUnit.NONE : ic2.api.machine.MachineSnapshot.EnergyUnit.EU, java.util.Map.of("storage_slots", (long)inventory.getSlots(), "locked", owner == null ? 0L : 1L));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory player, Player ownerPlayer) {
        if (!allowed(ownerPlayer)) return null;
        var container = new net.minecraft.world.Container(){

            @Override
            public int getContainerSize() {
                return inventory.getSlots();
            }

            @Override
            public boolean isEmpty() {
                for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
                return true;
            }

            @Override
            public ItemStack getItem(int i) {
                return inventory.getStackInSlot(i);
            }

            @Override
            public ItemStack removeItem(int i, int count) {
                return inventory.extractItem(i, count, false);
            }

            @Override
            public ItemStack removeItemNoUpdate(int i) {
                return inventory.extractItem(i, Integer.MAX_VALUE, false);
            }

            @Override
            public void setItem(int i, ItemStack stack) {
                inventory.setStackInSlot(i, stack);
            }

            @Override
            public void setChanged() {
                StorageEntity.this.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return allowed(player) && level.getBlockEntity(worldPosition) == StorageEntity.this && player.distanceToSqr(worldPosition.getCenter()) <= 64;
            }

            @Override
            public void clearContent() {
                for (int i = 0; i < getContainerSize(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        };
        return new ChestMenu(personal() ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x1, id, player, container, personal() ? 6 : 1);
    }

    @Override
    public boolean canReceiveEnergy(Direction side, int source, int packet) {
        return !personal() && packet > 0 && packet <= 32 && energy < 500;
    }

    @Override
    public int receiveEnergy(int amount) {
        int used = Math.clamp(amount, 0, 500 - energy);
        energy += used;
        if (used > 0) setChanged();
        return used;
    }

    void drop() {
        for (int i = 0; i < inventory.getSlots(); i++) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.put("items", inventory.serializeNBT(lookup));
        tag.putInt("energy", energy);
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        var saved = tag.getCompound("items").copy();
        saved.putInt("Size", inventory.getSlots());
        inventory.deserializeNBT(lookup, saved);
        energy = Math.clamp(tag.getInt("energy"), 0, 500);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
