package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class SolarArmorChecks {
    static void run(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
        var saved = slots.stream().map(slot -> player.getItemBySlot(slot).copy()).toList();
        long daytime = level.getDayTime(); float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1); var position = player.position();
        var pos = new BlockPos(540, 100, 540);
        try {
            for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
            player.setPos(540.5, 100, 540.5); level.getChunkAt(pos);
            var pack = stack("batpack"); var power = (ElectricItem) pack.getItem(); player.setItemSlot(EquipmentSlot.CHEST, pack);
            for (String name : java.util.List.of("solar_helmet", "advanced_solar_helmet")) {
                int production = name.startsWith("advanced") ? 5 : 1;
                var helmet = stack(name);
                require(helmet.getItem() instanceof ArmorItem && ((ArmorItem) helmet.getItem()).getDefense() == 1, "source solar helmet has one armor point");
                player.setItemSlot(EquipmentSlot.HEAD, helmet);
                power.setCharge(pack, 0); level.setDayTime(6000); level.setRainLevel(0); level.setThunderLevel(0); level.updateSkyBrightness();
                tick(player); require(power.charge(pack) == production, "solar helmet produces original daylight EU");
                level.setBlock(pos.above(2), Blocks.STONE.defaultBlockState(), 2 | 16);
                settleLighting(level, pos);
                tick(player); require(power.charge(pack) == production, "roof blocks solar charging");
                level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 2 | 16); settleLighting(level, pos); level.setDayTime(18000); level.updateSkyBrightness();
                tick(player); require(power.charge(pack) == production, "night blocks solar charging");
                level.setDayTime(6000); level.setRainLevel(1); level.updateSkyBrightness();
                tick(player);
                require(power.charge(pack) == (level.getBiome(pos).value().hasPrecipitation() ? production : 2 * production), "source precipitation-aware weather check");
                level.setRainLevel(0); level.updateSkyBrightness(); power.setCharge(pack, power.capacity());
                tick(player); require(power.charge(pack) == power.capacity(), "solar generation never overcharges armor");
            }
        } finally {
            level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 2 | 16);
            level.setDayTime(daytime); level.setRainLevel(rain); level.setThunderLevel(thunder); level.updateSkyBrightness(); player.setPos(position);
            for (int i = 0; i < slots.size(); i++) player.setItemSlot(slots.get(i), saved.get(i));
        }
    }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static void settleLighting(ServerLevel level, BlockPos pos) {
        var engine = level.getChunkSource().getLightEngine();
        var completed = engine.lightChunk(level.getChunkAt(pos), false).orTimeout(10, java.util.concurrent.TimeUnit.SECONDS);
        level.getServer().managedBlock(() -> { engine.tryScheduleUpdate(); return completed.isDone(); });
        completed.join();
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source solar equipment exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
