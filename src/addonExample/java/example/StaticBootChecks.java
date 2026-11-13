package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import ic2.api.item.GeneratedArmor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class StaticBootChecks {
    static void run(ServerLevel level) {
        var boots = stack("static_boots"); var generator = (GeneratedArmor) boots.getItem();
        require(!ElectricItem.supports(boots, Integer.MAX_VALUE, true), "static boots have generation-only storage");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
        var saved = slots.stream().map(slot -> player.getItemBySlot(slot).copy()).toList();
        var position = player.position(); boolean grounded = player.onGround(); long time = level.getGameTime();
        long day = level.getDayTime(); float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        var data = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        try {
            for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
            var pack = stack("batpack"); var power = (ElectricItem) pack.getItem(); player.setItemSlot(EquipmentSlot.CHEST, pack); player.setItemSlot(EquipmentSlot.FEET, boots);
            data.setGameTime(1); player.setOnGround(true); player.setPos(600.5, 100, 600.5); tick(player);
            require(generator.generatedEnergy(boots) == 0, "standing initializes anchor without generating EU");
            player.setPos(605.5, 100, 600.5); tick(player);
            require(generator.generatedEnergy(boots) == 1, "five walked blocks generate one EU");
            player.setPos(620.5, 100, 600.5); tick(player);
            require(generator.generatedEnergy(boots) == 4, "source distance increment is capped at three EU");
            var carpet = new BlockPos(625, 100, 600); level.setBlock(carpet.below(), Blocks.DIRT.defaultBlockState(), 2 | 16); level.setBlock(carpet, Blocks.WHITE_CARPET.defaultBlockState(), 2 | 16);
            player.setPos(625.5, 100.0625, 600.5); tick(player);
            require(generator.generatedEnergy(boots) == 7, "walking on wool carpet uses the source static bonus");
            player.setOnGround(false); player.setPos(630.5, 100, 600.5); tick(player);
            player.setOnGround(true); tick(player);
            require(generator.generatedEnergy(boots) == 7, "airborne movement resets anchor without generating EU");
            data.setGameTime(10); player.setPos(635.5, 100, 600.5); tick(player);
            require(power.charge(pack) == 7 && generator.generatedEnergy(boots) == 1, "same-tick transfer plus walking conserves old energy without duplicating it");
            data.setGameTime(1); generator.setGeneratedEnergy(boots, 19); player.setPos(650.5, 100, 600.5); tick(player);
            require(generator.generatedEnergy(boots) == 22 && generator.generationCapacity() == 22, "source threshold twenty permits a final increment up to twenty-two");
            var copy = boots.copy(); generator.setGeneratedEnergy(copy, 0);
            require(generator.generatedEnergy(boots) == 22 && generator.generatedEnergy(copy) == 0, "generation state survives copies without shared mutation");
            player.setOnGround(false); player.setPos(660.5, 100, 600.5); tick(player);
            generator.setGeneratedEnergy(boots, 0); player.setOnGround(true); tick(player);
            require(generator.generatedEnergy(boots) == 0, "airborne movement also resets a full generator anchor");
            generator.setGeneratedEnergy(boots, 7); power.setCharge(pack, power.capacity() - 2);
            player.setItemSlot(EquipmentSlot.HEAD, stack("solar_helmet"));
            level.setDayTime(6000); level.setRainLevel(0); level.setThunderLevel(0); level.updateSkyBrightness();
            SolarArmorChecks.settleLighting(level, player.blockPosition());
            require(level.isDay() && level.canSeeSkyFromBelowWater(player.blockPosition()), "combined-generation fixture is sunlit");
            data.setGameTime(10); tick(player);
            require(power.charge(pack) == power.capacity() && generator.generatedEnergy(boots) == 5, "source armor tick order is feet before head when storage is nearly full");
        } finally {
            data.setGameTime(time); player.setPos(position); player.setOnGround(grounded);
            level.setDayTime(day); level.setRainLevel(rain); level.setThunderLevel(thunder); level.updateSkyBrightness();
            for (int i = 0; i < slots.size(); i++) player.setItemSlot(slots.get(i), saved.get(i));
        }
    }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source static equipment exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
