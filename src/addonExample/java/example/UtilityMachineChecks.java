package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;

final class UtilityMachineChecks {
    private UtilityMachineChecks() {}

    static void run(ServerLevel level) {
        var pos = new BlockPos(208, 300, 208);
        level.getChunkAt(pos);
        Zombie victim = null;
        try {
            place(level, pos, "magnetizer");
            place(level, pos.above(), "iron_fence");
            var machine = level.getBlockEntity(pos);
            var sink = (EnergySink) machine;
            for (int tick = 0; tick < 4; tick++) sink.receiveEnergy(32);
            ApiSmokeChecks.tick(level, pos, machine.getType());
            var charge = level.getBlockState(pos.above()).getValues().entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals("charge")).findFirst().orElseThrow().getValue();
            require(charge.equals(6), "magnetizer charges a real adjacent iron fence using original formula");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 94, "charging spends actual EU");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            place(level, pos, "tesla_coil");
            machine = level.getBlockEntity(pos);
            sink = (EnergySink) machine;
            for (int tick = 0; tick < 40; tick++) sink.receiveEnergy(100);
            victim = new Zombie(EntityType.ZOMBIE, level);
            victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
            victim.setPos(pos.getX() + 2, pos.getY(), pos.getZ());
            level.addFreshEntity(victim);
            for (int tick = 0; tick < 32; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(victim.getHealth() == 10, "Tesla shocks targets within four blocks at 400 EU per damage");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 0, "Tesla spends its pulse and scan cost");
        } finally {
            if (victim != null) victim.discard();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        }
    }

    private static void place(ServerLevel level, BlockPos pos, String path) {
        level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.fromNamespaceAndPath("ic2", path)).orElseThrow().defaultBlockState());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
