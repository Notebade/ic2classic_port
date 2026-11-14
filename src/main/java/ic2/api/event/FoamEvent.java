package ic2.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Source-compatible selection and application hooks for foam targets supplied by native blocks or addons. */
public abstract class FoamEvent extends LevelEvent {
    private final BlockPos pos;
    protected FoamEvent(LevelAccessor level, BlockPos pos) { super(level); this.pos = pos.immutable(); }
    public BlockPos getPos() { return pos; }
    public BlockState getState() { return getLevel().getBlockState(pos); }
    public BlockEntity getBlockEntity() { return getLevel().getBlockEntity(pos); }
    public enum TargetType { ANY, SCAFFOLD, CABLE, TUBE, PIPE, CUSTOM }
    public static final class Check extends FoamEvent implements net.neoforged.bus.api.ICancellableEvent {
        private TargetType type = TargetType.ANY;
        private boolean custom;
        public Check(LevelAccessor level, BlockPos pos) { super(level, pos); }
        public void setCustomTarget(TargetType type) { this.type = java.util.Objects.requireNonNull(type); custom = true; }
        public boolean isCustomPlacement() { return custom; }
        public TargetType getType() { return type; }
    }
    public static final class Place extends FoamEvent implements net.neoforged.bus.api.ICancellableEvent {
        private boolean placeFoam;
        public Place(LevelAccessor level, BlockPos pos) { super(level, pos); }
        public void requestFoamPlacement() { placeFoam = true; }
        public boolean shouldPlaceFoam() { return placeFoam; }
    }
}
