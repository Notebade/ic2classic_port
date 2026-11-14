package ic2.api.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Rotation hooks for blocks which need custom facing rules. */
public final class PrecisionWrench {
    private static final Map<Block, Rotation> ROTATIONS = new ConcurrentHashMap<>();
    private static final Map<Block, Dismantling> DISMANTLING = new ConcurrentHashMap<>();

    private PrecisionWrench() {}

    public static void registerRotation(Block block, Rotation rotation) {
        if (ROTATIONS.putIfAbsent(block, rotation) != null) {
            throw new IllegalArgumentException("Duplicate precision wrench rotation for " + block);
        }
    }

    public static Rotation rotation(Block block) {
        return ROTATIONS.get(block);
    }

    /** Original block-specific recovery chance and intact drops; failed recovery uses the block loot table. */
    public record Dismantling(double recoveryChance, java.util.function.Function<UseOnContext, java.util.List<net.minecraft.world.item.ItemStack>> intactDrops) {
        public Dismantling {
            if (!Double.isFinite(recoveryChance) || recoveryChance < 0 || recoveryChance > 1) throw new IllegalArgumentException("Recovery chance outside [0,1]");
            java.util.Objects.requireNonNull(intactDrops);
        }
    }
    public static void registerDismantling(Block block, Dismantling dismantling) {
        if (DISMANTLING.putIfAbsent(block, dismantling) != null) throw new IllegalArgumentException("Duplicate wrench dismantling for " + block);
    }
    public static Dismantling dismantling(Block block) { return DISMANTLING.get(block); }

    @FunctionalInterface
    public interface Rotation {
        /** Return the proposed state of the same block. Return the original state to decline. Do not mutate the level. */
        BlockState rotate(BlockState state, UseOnContext context, Direction target);
    }
}
