package ic2.core.machine;

interface EnergySink extends ic2.api.energy.OvervoltageHandler {
    @Override default void onOvervoltage(net.minecraft.core.Direction side, int packet) {
        ElectricalOverload.destroy((net.minecraft.world.level.block.entity.BlockEntity) this);
    }
}
