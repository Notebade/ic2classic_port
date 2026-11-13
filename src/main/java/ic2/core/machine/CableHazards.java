package ic2.core.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Source thresholds operate on energy actually delivered along a path, never on an idle offer. */
final class CableHazards {
    private static final Map<ServerLevel, Map<LivingEntity, Long>> SHOCKS = new WeakHashMap<>();
    private CableHazards() {}

    static void supplied(Level world, EnergyNetwork.CableVisit path, int energy) {
        if (!(world instanceof ServerLevel level)) return;
        var shock = new HashMap<LivingEntity, Integer>();
        for (var visit = path; visit != null; visit = visit.previous()) {
            var pos = visit.position();
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(visit.cable())) continue;
            var rating = rating(visit.cable());
            if (energy > rating.absorption()) {
                var state = level.getBlockState(pos);
                boolean water = state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
                var contact = state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? new AABB(pos) : new AABB(pos).inflate(1);
                var wet = new AABB(pos).inflate(3);
                for (var entity : level.getEntitiesOfClass(LivingEntity.class, water ? wet : contact, ElectricHazards::shockable))
                    if (entity.getBoundingBox().intersects(contact) || water && entity.isInWaterOrRain() && entity.getBoundingBox().intersects(wet))
                        shock.merge(entity, energy - rating.absorption(), Math::max);
            }
            if (energy > rating.maximum()) {
                level.setBlock(pos, level.getBlockState(pos).getFluidState().createLegacyBlock(), 2 | 16);
            } else if (energy >= 9001 && rating.bare() != null) {
                level.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ic2", rating.bare())).defaultBlockState(), 2 | 16);
            }
        }
        if (!shock.isEmpty()) {
            var pending = SHOCKS.computeIfAbsent(level, ignored -> new HashMap<>());
            shock.forEach((entity, amount) -> pending.merge(entity, (long) amount, Long::sum));
        }
    }

    static void tick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pending = SHOCKS.remove(level);
        if (pending == null) return;
        pending.forEach((entity, energy) -> {
            if (entity.level() == level && ElectricHazards.shockable(entity))
                entity.hurt(ElectricHazards.source(level), (float) ((energy + 63) / 64));
        });
    }

    private static Rating rating(EnergyCableBlock cable) {
        String name = BuiltInRegistries.BLOCK.getKey(cable).getPath();
        return switch (name) {
            case "tin_cable" -> new Rating(5, 3, null);
            case "copper_cable" -> new Rating(32, 8, null);
            case "copper_insulated_cable" -> new Rating(32, 32, "copper_cable");
            case "gold_cable", "bronze_cable" -> new Rating(128, 8, null);
            case "gold_insulated_cable" -> new Rating(128, 32, "gold_cable");
            case "bronze_insulated_cable" -> new Rating(128, 32, "bronze_cable");
            case "gold_double_insulated_cable" -> new Rating(128, 128, "gold_cable");
            case "bronze_double_insulated_cable" -> new Rating(128, 128, "bronze_cable");
            case "iron_cable" -> new Rating(2048, 0, null);
            case "iron_insulated_cable" -> new Rating(2048, 128, "iron_cable");
            case "iron_double_insulated_cable" -> new Rating(2048, 512, "iron_cable");
            case "iron_triple_insulated_cable" -> new Rating(2048, 2048, "iron_cable");
            case "aluminium_cable" -> new Rating(8192, 8, null);
            case "aluminium_insulated_cable" -> new Rating(8192, 128, "aluminium_cable");
            case "aluminium_double_insulated_cable" -> new Rating(8192, 512, "aluminium_cable");
            case "aluminium_triple_insulated_cable" -> new Rating(8192, 2048, "aluminium_cable");
            case "aluminium_quadruple_insulated_cable" -> new Rating(8192, 8192, "aluminium_cable");
            case "glassfiber_cable" -> new Rating(512, 9001, null);
            case "super_cable" -> new Rating(8192, 9001, null);
            case "plasma_cable" -> new Rating(32768, 32769, null);
            default -> new Rating(2048, 9001, null);
        };
    }
    private record Rating(int maximum, int absorption, String bare) {}
}
