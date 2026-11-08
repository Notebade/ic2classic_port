package ic2.core.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageAttachment;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

final class RubberTrunkPlacer extends TrunkPlacer {
    static final MapCodec<RubberTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
        trunkPlacerParts(instance).apply(instance, RubberTrunkPlacer::new));

    private RubberTrunkPlacer(int baseHeight, int randomHeightA, int randomHeightB) {
        super(baseHeight, randomHeightA, randomHeightB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return Rubberwood.TRUNK.get();
    }

    @Override
    public List<FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> place,
            RandomSource random, int height, BlockPos pos, TreeConfiguration config) {
        setDirtAt(level, place, random, pos.below(), config);
        for (int y = 0; y < height; y++) {
            BlockPos logPos = pos.above(y);
            if (y == 1) {
                placeLog(level, place, random, logPos, config, state -> state.setValue(RubberLogBlock.RESIN, true)
                    .setValue(RubberLogBlock.COLLECTABLE, true)
                    .setValue(RubberLogBlock.RESIN_FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)));
            } else {
                placeLog(level, place, random, logPos, config);
            }
        }
        return List.of(new FoliageAttachment(pos.above(height), 0, false));
    }
}
