package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class PoweredLampEntity extends BlockEntity implements EnergySink, MachineSnapshot.Provider {
    private int energy;
    private int brightness;

    PoweredLampEntity(BlockPos p, BlockState s) {
        super(NuclearInfrastructure.LAMP_ENTITY.get(), p, s);
        brightness = ((PoweredLampBlock)s.getBlock()).adjustable ? 5 : 15;
    }

    void tick() {
        if (level.getGameTime() % 10 != 0) return;
        boolean active = energy > 0;
        if (active) {
            energy = Math.max(0, energy - (getBlockState().getValue(BlockStateProperties.LIT) ? 1 : 2));
            setChanged();
        }
        updateLight(active);
    }

    private void updateLight(boolean active) {
        int light = active ? brightness : 0;
        if (getBlockState().getValue(PoweredLampBlock.LIGHT) != light || getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(PoweredLampBlock.LIGHT, light).setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
    }

    void adjust(int delta) {
        brightness = Math.clamp(brightness + delta, 0, 15);
        setChanged();
        updateLight(energy > 0);
    }

    @Override
    public boolean canReceiveEnergy(Direction side, int source, int packet) {
        return packet > 0 && packet <= 2048 && energy < 50 && side == getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING).getOpposite();
    }

    @Override
    public int receiveEnergy(int offered) {
        int used = Math.max(0, Math.min(offered, 50 - energy));
        energy += used;
        if (used > 0) setChanged();
        return used;
    }

    @Override
    public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, 50, MachineSnapshot.EnergyUnit.EU, Map.of("brightness", (long)brightness));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("energy", energy);
        tag.putInt("brightness", brightness);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        energy = Math.clamp(tag.getInt("energy"), 0, 50);
        brightness = Math.clamp(tag.getInt("brightness"), 0, 15);
    }
}
