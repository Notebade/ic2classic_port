package ic2.api.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Copied block face, its baked-layer tints and quarter-turn UV rotations. */
public record SurfaceTexture(BlockState state, Direction side, List<Integer> colors, List<Integer> rotations) {
    private record Wire(BlockState state, Direction side, List<Integer> colors, List<Integer> rotations) {}
    public static final Codec<SurfaceTexture> CODEC = RecordCodecBuilder.<Wire>create(instance -> instance.group(
        BlockState.CODEC.fieldOf("state").forGetter(Wire::state),
        Direction.CODEC.fieldOf("side").forGetter(Wire::side),
        Codec.INT.listOf(1, 10).fieldOf("colors").forGetter(Wire::colors),
        Codec.intRange(0, 3).listOf(1, 10).fieldOf("rotations").forGetter(Wire::rotations)
    ).apply(instance, Wire::new)).comapFlatMap(wire -> wire.colors.size() == wire.rotations.size()
        ? com.mojang.serialization.DataResult.success(new SurfaceTexture(wire.state, wire.side, wire.colors, wire.rotations))
        : com.mojang.serialization.DataResult.error(() -> "Surface tint and rotation counts differ"),
        value -> new Wire(value.state, value.side, value.colors, value.rotations));
    public SurfaceTexture {
        colors = List.copyOf(colors); rotations = List.copyOf(rotations);
        if (colors.isEmpty() || colors.size() > 10 || colors.size() != rotations.size()
                || rotations.stream().anyMatch(rotation -> rotation < 0 || rotation > 3)) throw new IllegalArgumentException("Invalid surface texture layers");
    }
    public SurfaceTexture rotate(int layer) {
        var next = new java.util.ArrayList<>(rotations); next.set(layer, (next.get(layer) + 1) % 4);
        return new SurfaceTexture(state, side, colors, next);
    }
    public boolean sameSource(SurfaceTexture other) { return state.equals(other.state) && side == other.side && colors.equals(other.colors); }
}
