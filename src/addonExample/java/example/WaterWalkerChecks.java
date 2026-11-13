package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

final class WaterWalkerChecks {
    static void run(ServerLevel level) {
        var module = stack("water_walker_module");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level); var saved = player.getItemBySlot(EquipmentSlot.FEET).copy();
        var position = player.position(); var velocity = player.getDeltaMovement(); boolean ground = player.onGround(), shift = player.isShiftKeyDown();
        var pos = new BlockPos(780, 99, 780); var original = level.getBlockState(pos); long time = level.getGameTime();
        try {
            var boots = ((ModularArmor) stack("nano_suit_boots").getItem()).defaultArmor(); var armor = (ModularArmor) boots.getItem();
            require(armor.install(boots, ArmorModule.MOVEMENT, module), "water walking module fits boot movement slot"); armor.setCharge(boots, 1000);
            player.setItemSlot(EquipmentSlot.FEET, boots); player.setPos(780.5, 100.02, 780.5); player.setShiftKeyDown(false);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2 | 16);
            ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(time - time % 10);
            player.setOnGround(false); player.setDeltaMovement(0, -.3, 0); tick(player);
            require(player.onGround() && player.getDeltaMovement().y == 0 && armor.charge(boots) == 900, "source liquid walking supports falling player and spends 100 EU per ten world ticks");
            player.setShiftKeyDown(true); player.setOnGround(false); player.setDeltaMovement(0, -.3, 0); tick(player);
            require(!player.onGround() && player.getDeltaMovement().y == -.3 && armor.charge(boots) == 900, "sneaking disables liquid walking without spending EU");
            player.setShiftKeyDown(false); armor.setCharge(boots, 99); tick(player); require(player.getDeltaMovement().y == -.3 && armor.charge(boots) == 99, "insufficient EU cannot support a player above liquid");
        } finally {
            ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(time); level.setBlock(pos, original, 2 | 16);
            player.setItemSlot(EquipmentSlot.FEET, saved); player.setPos(position); player.setDeltaMovement(velocity); player.setOnGround(ground); player.setShiftKeyDown(shift);
        }
        checkBoat(level);
    }
    private static void checkBoat(ServerLevel level) {
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("ic2:carbon_boat"));
        require(type != null && type.create(level) instanceof net.minecraft.world.entity.vehicle.Boat, "source carbon boat has a native spawnable entity");
        var boat = (net.minecraft.world.entity.vehicle.Boat) type.create(level); var pos = new BlockPos(784, 99, 780); var old = level.getBlockState(pos);
        try {
            require(boat.getDropItem() == stack("carbon_boat").getItem(), "carbon boat returns its own item");
            var savedBoat = new net.minecraft.nbt.CompoundTag(); require(boat.save(savedBoat), "carbon boat serializes with its native entity ID");
            var loaded = net.minecraft.world.entity.EntityType.loadEntityRecursive(savedBoat, level, entity -> entity);
            require(loaded instanceof net.minecraft.world.entity.vehicle.Boat restored && restored.getDropItem() == stack("carbon_boat").getItem(), "carbon boat retains its type and drops after entity reload");
            if (loaded != null) loaded.discard();
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2 | 16); boat.setPos(784.5, 100, 780.5); boat.tick();
            boat.fallDistance = 30; boat.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -.1, 0));
            require(boat.isAlive(), "carbon boat survives a real long-fall ground collision");
            var passenger = net.minecraft.world.entity.EntityType.COW.create(level); passenger.startRiding(boat, true);
            for (int i = 0; i < 65; i++) { boat.onAboveBubbleCol(true); boat.tick(); }
            require(passenger.getVehicle() == boat, "source carbon boat resists downward bubble-column passenger ejection"); passenger.stopRiding();
            var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:carbon_boat")).orElseThrow();
            require(recipe.value().getResultItem(level.registryAccess()).is(stack("carbon_boat").getItem()), "source carbon boat crafting route exists");
        } finally { boat.discard(); level.setBlock(pos, old, 2 | 16); }
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow(() -> new AssertionError("source water walker dependency exists: " + id))); }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
