package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

final class UtilityMachineEntity extends BlockEntity implements EnergySink, MachineSnapshot.Provider {
    private int energy, delay, clock;
    boolean upgraded;
    private final boolean tesla = ((UtilityMachineBlock) getBlockState().getBlock()).tesla;

    UtilityMachineEntity(BlockPos pos, BlockState state) { super(UtilityMachines.ENTITY.get(), pos, state); }

    private int capacity() { return tesla ? 50000 : upgraded ? 1000 : 100; }
    private int packet() { return tesla ? 128 : 32; }

    void tick() {
        if (tesla) {
            if (++clock >= 32) { clock = 0; if (!level.hasNeighborSignal(worldPosition)) shock(); }
        } else if (energy > 0 && delay-- <= 0) {
            if (delay < 0) delay++;
            if (level.hasNeighborSignal(worldPosition)) { delay = 5; return; }
            boolean changed = chargeColumn(Direction.UP) | chargeColumn(Direction.DOWN);
            if (!changed) delay = 5;
            else if (delay == 0) {
                level.playSound(null, worldPosition, UtilityMachines.MAGNETIZER_SOUND.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
                delay = -5;
            }
        }
    }

    private void shock() {
        int damage = UtilityCycles.teslaDamage(energy);
        if (damage == 0) return;
        energy--;
        var victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(4), ElectricHazards::shockable);
        for (var victim : victims) {
            victim.hurt(ElectricHazards.source(level, net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft((ServerLevel) level)), damage);
            ((ServerLevel) level).sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0, 0, 1), 1),
                victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), damage, .3, .5, .3, .05);
        }
        if (!victims.isEmpty()) energy = Math.max(0, energy - damage * 400);
        setChanged();
    }

    private boolean chargeColumn(Direction direction) {
        boolean changed = false;
        for (int distance = 1; upgraded || distance <= 20; distance++) {
            if (energy < (upgraded ? 10 : 1)) break;
            var pos = worldPosition.relative(direction, distance);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) break;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof MagneticFenceBlock)) break;
            int charge = state.getValue(MagneticFenceBlock.CHARGE);
            int added = UtilityCycles.fenceCharge(charge, energy, upgraded);
            if (added == 0) continue;
            energy -= added * (upgraded ? 10 : 1);
            level.setBlock(pos, state.setValue(MagneticFenceBlock.CHARGE, charge + added), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            changed = true;
        }
        if (changed) setChanged();
        return changed;
    }

    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) { return packet > 0 && packet <= packet() && energy < capacity(); }
    @Override public int receiveEnergy(int offered) {
        int accepted = Math.clamp(offered, 0, Math.min(packet(), capacity() - energy));
        energy += accepted;
        if (accepted > 0) setChanged();
        return accepted;
    }
    @Override public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, capacity(), MachineSnapshot.EnergyUnit.EU,
            Map.of("input_packet_eu", (long) packet(), "upgraded", upgraded ? 1L : 0L, "redstone_disabled", level.hasNeighborSignal(worldPosition) ? 1L : 0L));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putBoolean("upgraded", upgraded);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); upgraded = tag.getBoolean("upgraded"); energy = Math.clamp(tag.getInt("energy"), 0, capacity());
    }
}
