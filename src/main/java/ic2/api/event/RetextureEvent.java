package ic2.api.event;

import ic2.api.item.SurfaceTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Fired before an obscurator changes a surface; addon handlers mark successful applications. */
public final class RetextureEvent extends LevelEvent implements net.neoforged.bus.api.ICancellableEvent {
    private final BlockPos pos;
    private final Direction side;
    private final Player player;
    private final SurfaceTexture texture;
    private boolean applied;
    public RetextureEvent(LevelAccessor level, BlockPos pos, Direction side, Player player, SurfaceTexture texture) {
        super(level); this.pos = pos.immutable(); this.side = side; this.player = player; this.texture = texture;
    }
    public BlockPos getPos() { return pos; }
    public Direction getSide() { return side; }
    public Player getPlayer() { return player; }
    public SurfaceTexture getTexture() { return texture; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
}
