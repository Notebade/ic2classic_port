package ic2.api.item;

import ic2.api.event.FoamEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A conductor or transport block that retains its identity under wet and hardened construction foam. */
public interface FoamTarget {
    IntegerProperty FOAMED = IntegerProperty.create("foamed", 0, 2);
    FoamEvent.TargetType foamTargetType();
    default int foamLevel(BlockState state) { return state.getValue(FOAMED); }
    boolean applyFoam(ServerLevel level, BlockPos pos);
}
