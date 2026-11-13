package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class AdvancedUpgradeChecks {
    private AdvancedUpgradeChecks() {}
    static void run(ServerLevel level) {
        var pos = new BlockPos(416, 300, 416);
        level.getChunkAt(pos);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        try {
            player.setPos(pos.getCenter());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("induction_furnace")).orElseThrow().defaultBlockState());
            var machine = level.getBlockEntity(pos);
            var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            require(menu.slots.size() == 43, "induction furnace exposes its two source upgrade slots");
            require(menu.slots.get(5).safeInsert(stack("upgrade_energy_storage", 2)).isEmpty(), "storage upgrade installs in the real machine");
            require(menu.slots.get(6).safeInsert(stack("upgrade_transformer", 1)).isEmpty(), "tier upgrade installs in the real machine");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity() == 30000, "upgrades add 20000 EU storage");
            require(((EnergySink) machine).canReceiveEnergy(Direction.UP, 0, 512), "transformer upgrade increases accepted input tier");
            require(!menu.slots.get(5).mayPlace(stack("upgrade_overclocker", 1)), "source advanced machines reject recipe modifiers");
            var saved = machine.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("induction_furnace")).orElseThrow().defaultBlockState());
            machine = level.getBlockEntity(pos); machine.loadWithComponents(saved, level.registryAccess());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity() == 30000, "advanced upgrades survive reload");
            menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            menu.slots.get(5).setByPlayer(ItemStack.EMPTY); menu.slots.get(6).setByPlayer(ItemStack.EMPTY);
            menu.slots.get(5).safeInsert(stack("upgrade_muffler", 2));
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("sound_multiplier_permille") == 640, "mufflers multiply volume per installed item");
            menu.slots.get(6).safeInsert(stack("upgrade_mute", 1));
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("sound_multiplier_permille") == 0, "mute overrides the sound level");
            menu.slots.get(5).setByPlayer(ItemStack.EMPTY); menu.slots.get(6).setByPlayer(ItemStack.EMPTY);
            ((EnergySink) machine).receiveEnergy(1000);
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("heat") == 0, "empty induction furnace waits without a redstone signal");
            menu.slots.get(5).safeInsert(stack("upgrade_redstone_inverter", 1));
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("heat") == 1, "inverter changes actual standby heating");
            menu.slots.get(5).setByPlayer(ItemStack.EMPTY);
            menu.slots.get(5).safeInsert(stack("upgrade_redstone_sensitivity", 1));
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("heat") == 2, "sensitivity toggle enables unconditional standby heat");
            menu.slots.get(6).safeInsert(stack("upgrade_redstone_sensitivity", 1));
            ApiSmokeChecks.tick(level, pos, machine.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("heat") == 0, "two sensitivity stacks cancel as in the source install hooks");

            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("induction_furnace")).orElseThrow().defaultBlockState());
            machine = level.getBlockEntity(pos);
            menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            menu.slots.get(5).safeInsert(new ItemStack(ExampleAddon.HOOK_TEST_UPGRADE.get()));
            menu.slots.get(1).safeInsert(new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
            var hot = machine.saveWithoutMetadata(level.registryAccess()); hot.putInt("heat", 10000); hot.putInt("energy", 10000);
            machine.loadWithComponents(hot, level.registryAccess());
            for (int tick = 0; tick < 12; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            var hooks = menu.slots.get(5).getItem().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            require(hooks.getInt("upgrade_ticks") == 12 && hooks.getInt("upgrade_recipes") == 1
                && hooks.getString("last_result").equals("minecraft:iron_ingot"), "real addon upgrade receives working tick and completed-output callbacks");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("centrifugal_rare_earth_extractor")).orElseThrow().defaultBlockState());
            machine = level.getBlockEntity(pos); menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            menu.slots.get(3).safeInsert(new ItemStack(ExampleAddon.HOOK_TEST_UPGRADE.get()));
            menu.slots.get(1).safeInsert(new ItemStack(net.minecraft.world.item.Items.CLAY_BALL, 2));
            hot = machine.saveWithoutMetadata(level.registryAccess()); hot.putInt("heat", 10000); hot.putInt("energy", 5120); machine.loadWithComponents(hot, level.registryAccess());
            for (int tick = 0; tick < 6; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            hooks = menu.slots.get(3).getItem().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            require(hooks.getInt("upgrade_recipes") == 2 && hooks.getString("last_result").equals("ic2:dust_aluminium"), "rare-earth partial and completed batches both notify the real addon recipe hook");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("slow_grinder")).orElseThrow().defaultBlockState());
            menu = ((MenuProvider) level.getBlockEntity(pos)).createMenu(0, player.getInventory(), player);
            require(menu.slots.size() == 49 && !menu.slots.get(11).mayPlace(stack("upgrade_energy_storage", 1)), "slow grinder retains source prohibition on machine-stat modifiers");
        } finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); player.setPos(previous); }
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ic2", path); }
    private static ItemStack stack(String path, int count) { return new ItemStack(Ic2Api.item(id(path)).orElseThrow(), count); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
