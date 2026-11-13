package ic2.core.machine;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

final class ElectricalOverload {
    static void destroy(BlockEntity input) {
        BlockEntity owner = input instanceof PressurePartEntity part ? part.controller()
            : input instanceof ColossalPartEntity part ? part.controller() : input;
        if (owner == null || owner.isRemoved() || !(owner.getLevel() instanceof ServerLevel level)
            || !level.hasChunkAt(owner.getBlockPos()) || level.getBlockEntity(owner.getBlockPos()) != owner) return;
        var origin = owner.getBlockPos(); var positions = new ArrayList<BlockPos>(); positions.add(origin);
        for (var cursor : BlockPos.betweenClosed(origin.offset(-3, -3, -3), origin.offset(3, 3, 3))) {
            if (cursor.equals(origin) || !level.hasChunkAt(cursor)) continue;
            var entity = level.getBlockEntity(cursor);
            boolean linked = entity instanceof PressurePartEntity pressure && pressure.controller() == owner
                || entity instanceof ColossalPartEntity colossal && colossal.controller() == owner
                || entity instanceof PowerPartEntity power && power.controller() == owner;
            if (!linked && owner instanceof ic2.api.energy.EnergyPortOwner)
                linked = ic2.api.energy.EuNetwork.portOwner(level, cursor).filter(origin::equals).isPresent();
            if (linked) positions.add(cursor.immutable());
        }
        for (var pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        for (var pos : positions) ReactorExplosion.detonateElectrical(level, pos);
    }
}
