package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class DynamicEnergyChecks {
    static void run(ServerLevel level) {
        var item = ExampleAddon.DYNAMIC_EU_ITEM.get(); var small = new ItemStack(item); var big = new ItemStack(item);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, big, tag -> tag.putBoolean("big", true));
        require(ElectricItem.supports(small, 32, true), "dynamic EU item uses per-stack capacity rather than a zero baseline");
        require(!ElectricItem.supports(big, 32, true) && ElectricItem.supports(big, 512, true), "dynamic charging packet changes per stack");
        require(!ElectricItem.supports(small, 32, false), "dynamic consumer cannot provide energy until enabled");
        var pos = new BlockPos(680, 100, 680);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        try {
            level.setBlock(pos, Ic2Api.block(ResourceLocation.parse("ic2:charging_bench_lv")).orElseThrow().defaultBlockState(), 2 | 16);
            var entity = level.getBlockEntity(pos);
            var menu = ((net.minecraft.world.MenuProvider) entity).createMenu(80, player.getInventory(), player);
            require(menu.getSlot(0).mayPlace(small), "native bench admits dynamic storage"); menu.getSlot(0).setByPlayer(small);
            require(((ic2.api.energy.EnergySink) entity).receiveEnergy(32) == 32, "fixture supplies one legal LV packet");
            ApiSmokeChecks.tick(level, pos, entity.getType());
            require(item.charge(small) == 7 && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 25, "native bench honors the dynamic seven EU rate");
            item.setCharge(small, 200);
            var automation = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            require(!automation.extractItem(0, 1, true).isEmpty(), "native completion extraction uses dynamic capacity");
            var source = new ItemStack(item); net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, source, tag -> tag.putBoolean("provider", true)); item.setCharge(source, 200);
            require(menu.getSlot(16).mayPlace(source), "native source slot uses per-stack provider flag"); menu.getSlot(16).setByPlayer(source);
            ApiSmokeChecks.tick(level, pos, entity.getType());
            require(item.charge(source) == 193 && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 32, "dynamic provider discharge conserves energy at its own rate");
        } finally { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); }
        var saved = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(slot -> player.getInventory().getItem(slot).copy()).toList(); int selected = player.getInventory().selected;
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            var battery = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:re_battery")).orElseThrow()); var power = (ElectricItem) battery.getItem(); power.setCharge(battery, 1000);
            var target = new ItemStack(item); player.getInventory().setItem(0, battery); player.getInventory().setItem(1, target);
            battery.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND); battery.getItem().onUseTick(level, player, battery, 19999);
            require(item.charge(target) == 200 && power.charge(battery) == 800, "portable charging honors dynamic capacity and source rate");
            player.stopUsingItem();
            var pack = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:batpack")).orElseThrow()); var wornPower = (ElectricItem) pack.getItem(); wornPower.setCharge(pack, 1000); player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, pack);
            target = new ItemStack(item); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, target);
            require(ic2.api.item.ElectricEquipment.use(target, 50, player) && item.charge(target) == 200 && wornPower.charge(pack) == 750, "armor refill uses dynamic target capacity and preserves EU");
            require(!ic2.api.item.ElectricEquipment.use(target, 201, player) && item.charge(target) == 200 && wornPower.charge(pack) == 750, "oversized dynamic use cannot destroy a partial charge");
            var disposable = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:su_battery")).orElseThrow());
            item.setCharge(target, 0); player.getInventory().setItem(1, target); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, disposable);
            disposable.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            require(disposable.isEmpty() && item.charge(target) == 200, "disposable battery respects dynamic target capacity");
        } finally {
            player.stopUsingItem(); for (int slot = 0; slot < saved.size(); slot++) player.getInventory().setItem(slot, saved.get(slot)); player.getInventory().selected = selected;
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
