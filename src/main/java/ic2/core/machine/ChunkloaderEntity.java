package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class ChunkloaderEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    private int energy, radius = 1, mode = 1;
    private boolean processing, active, issued;
    UUID owner;
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 6; }
        @Override public int get(int index) { return switch (index) { case 0 -> energy; case 1 -> radius; case 2 -> processing ? 1 : 0; case 3 -> mode; case 4 -> active ? 1 : 0; default -> ChunkloaderRules.cost(radius, processing); }; }
        @Override public void set(int index, int value) {}
    };

    ChunkloaderEntity(BlockPos pos, BlockState state) { super(Chunkloading.ENTITY.get(), pos, state); }

    boolean savedActive() { return active && energy >= ChunkloaderRules.cost(radius, processing); }

    void tick() {
        int cost = ChunkloaderRules.cost(radius, processing);
        if (active && (energy < cost || level.hasNeighborSignal(worldPosition))) release();
        if (!active && energy >= ChunkloaderRules.startup(radius, processing) && !level.hasNeighborSignal(worldPosition)) active = true;
        if (!active) return;
        if (!issued) { tickets(true); issued = true; }
        energy -= cost;
        if (level.getGameTime() % 4 == 0) ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
            worldPosition.getX() + .5, worldPosition.getY() + 1.5, worldPosition.getZ() + .5, 1, 0, .2, 0, .2);
        setChanged();
    }

    private void tickets(boolean add) {
        if (!(level instanceof ServerLevel server)) return;
        int x = worldPosition.getX() >> 4, z = worldPosition.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
            Chunkloading.TICKETS.forceChunk(server, worldPosition, x + dx, z + dz, add, processing);
    }

    void release() {
        if (active || issued) tickets(false);
        active = false; issued = false; setChanged();
    }

    boolean allowed(Player player, boolean edit) {
        if (owner == null) { owner = player.getUUID(); setChanged(); }
        if (owner.equals(player.getUUID()) || player.hasPermissions(2)) return true;
        if (edit) return false;
        if (mode == 0) return true;
        var owningPlayer = level.getPlayerByUUID(owner);
        return mode == 1 && owningPlayer != null && player.isAlliedTo(owningPlayer);
    }

    void button(Player player, int button) {
        if (!allowed(player, true) || button < 0 || button > 3 && (button < 16 || button > 23)) return;
        release();
        if (button >= 16) {
            radius = (button - 16) & 3; processing = (button - 16 & 4) != 0; setChanged(); return;
        }
        switch (button) {
            case 0 -> radius = Math.max(0, radius - 1);
            case 1 -> radius = Math.min(3, radius + 1);
            case 2 -> processing = !processing;
            case 3 -> mode = (mode + 1) % 3;
        }
        setChanged();
    }

    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) { return packet > 0 && packet <= 128 && energy < 60000; }
    @Override public int receiveEnergy(int offered) { int accepted = Math.clamp(offered, 0, Math.min(128, 60000 - energy)); energy += accepted; if (accepted > 0) setChanged(); return accepted; }
    @Override public Component getDisplayName() { return Component.translatable("block.ic2.chunkloader"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return allowed(player, false) ? new ChunkloaderMenu(id, inventory, this, data) : null; }
    @Override public MachineSnapshot machineInfo() {
        return new MachineSnapshot(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()), active,
            energy, 60000, MachineSnapshot.EnergyUnit.EU, Map.of("radius", (long) radius, "chunk_processing", processing ? 1L : 0L,
            "cost_eu", (long) ChunkloaderRules.cost(radius, processing), "access_mode", (long) mode, "forced_chunks", active ? (long) (2 * radius + 1) * (2 * radius + 1) : 0L));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putInt("radius", radius); tag.putInt("mode", mode);
        tag.putBoolean("processing", processing); tag.putBoolean("active", active); if (owner != null) tag.putUUID("owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); energy = Math.clamp(tag.getInt("energy"), 0, 60000); radius = tag.contains("radius") ? Math.clamp(tag.getInt("radius"), 0, 3) : 1;
        mode = tag.contains("mode") ? Math.clamp(tag.getInt("mode"), 0, 2) : 1; processing = tag.getBoolean("processing"); active = tag.getBoolean("active");
        issued = false; owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
