package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;

/** Real device ticks, inventories, public EU delivery and persistence in an isolated world. */
final class ChargingSmokeChecks {
    private ChargingSmokeChecks() {}
    static void verify(ServerLevel level, BlockPos pos) {
        try {
            for (int x : new int[]{-1,3}) for (int z : new int[]{-1,3}) level.getChunkAt(pos.offset(x,0,z));
            benches(level, pos); stations(level, pos); pads(level, pos); redirectors(level, pos);
        } finally {
            for (int x = -1; x <= 3; x++) for (int z = -1; z <= 3; z++) level.setBlockAndUpdate(pos.offset(x, 0, z), Blocks.AIR.defaultBlockState());
        }
    }
    private static void benches(ServerLevel level, BlockPos pos) {
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        int[] packets = {32,128,512,2048,8192};
        for (int tier = 0; tier < tiers.length; tier++) {
            place(level, pos, "charging_bench_" + tiers[tier]); var items = items(level, pos);
            for (int slot = 0; slot < 16; slot++) insert(items, slot, battery(level, "re_battery", 0));
            int input = 0;
            while (input < 1600) { int sent = feed(level, pos, Math.min(packets[tier], 1600 - input), Direction.DOWN); require(sent > 0, "bench accepts EU"); input += sent; }
            tick(level, pos);
            for (int slot = 0; slot < 16; slot++) require(charge(level, items.getStackInSlot(slot)) == 100, "all sixteen bench slots charge at the item transfer rate");
            require(stored(level, pos) == 0 && items.extractItem(0, 1, true).isEmpty(), "bench preserves energy and automation holds partially charged items");
            reload(level, pos); require(charge(level, items(level, pos).getStackInSlot(15)) == 100, "bench contents survive reload");
        }
        place(level, pos, "charging_bench_lv");
        insert(items(level, pos), 0, battery(level, "energy_crystal", 0));
        for (int i = 0; i < 4; i++) require(feed(level, pos, 32, Direction.DOWN) == 32, "LV bench accepts LV EU");
        tick(level, pos); require(charge(level, items(level, pos).getStackInSlot(0)) == 128, "LV bench ignores item tier as in original");
        var player = FakePlayerFactory.getMinecraft(level); player.setPos(pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5);
        var menu = ((net.minecraft.world.MenuProvider) level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
        require(menu.clickMenuButton(player, 0) && Ic2Api.machineInfo(level, pos).orElseThrow().values().get("sorted") == 1, "bench GUI toggles sorting on server");
        insert(items(level, pos), 5, battery(level, "re_battery", 500));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, battery(level, "re_battery", 1000));
        menu.clickMenuButton(player, 1);
        require(charge(level, player.getMainHandItem()) == 500 && charge(level, items(level, pos).getStackInSlot(5)) == 1000, "sorted bench swaps held battery without changing charge");
        menu.clickMenuButton(player, 2);
        require(charge(level, player.getMainHandItem()) == 500, "take button cannot overwrite occupied hand");
        insert(items(level, pos), 1, battery(level, "re_battery", 10000));
        require(charge(level, items(level, pos).extractItem(1, 1, false)) == 10000, "automation recovers fully charged items");
        player.setPos(pos.getX() + 100, pos.getY(), pos.getZ());
        require(!menu.clickMenuButton(player, 0), "remote menu clicks cannot change bench configuration");
    }
    private static void stations(ServerLevel level, BlockPos pos) {
        String[] tiers = {"lv","mv","hv","ev","iv"};
        long[] capacities = {40000,600000,75000000,75000000,500000000};
        for (int i = 0; i < tiers.length; i++) {
            place(level, pos, "battery_station_" + tiers[i]);
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity() == capacities[i], "station uses original buffer capacity");
            require(feed(level, pos, 32, Direction.DOWN) == 0, "station refuses network EU input");
        }
        place(level, pos, "battery_station_lv"); place(level, pos.east(), "charging_bench_iv");
        var items = items(level, pos);
        for (int slot = 0; slot < 16; slot++) insert(items, slot, battery(level, "re_battery", 1000));
        insert(items, 16, battery(level, "re_battery", 0)); tick(level, pos);
        require(stored(level, pos.east()) == 128 && stored(level, pos) == 1472, "LV station emits four 32 EU packets after discharging sixteen source cells");
        tick(level, pos);
        require(charge(level, items.getStackInSlot(16)) == 100 && stored(level, pos) == 2844 && stored(level, pos.east()) == 256, "station charges target before refilling and emitting");
        require(charge(level, items.getStackInSlot(0)) == 800, "station discharge respects item transfer rate");
        reload(level, pos); require(stored(level, pos) == 2844, "station buffer survives reload");
        place(level, pos.east(), "air");
    }
    private static void pads(ServerLevel level, BlockPos pos) {
        String[] tiers = {"lv","mv","hv","fission"};
        int[] packets = {32,128,512,2048};
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("51383b17-5fd8-44e0-84d0-2132d63881f2"), "ChargingTest"));
        player.setPos(pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5);
        level.addNewPlayer(player);
        try {
            for (int i = 0; i < tiers.length; i++) {
                place(level, pos, "chargepad_" + tiers[i]);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, battery(level, "re_battery", 0));
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, battery(level, "re_battery", 0));
                require(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP) == null, "pad does not expose item automation");
                require(feed(level, pos, packets[i], Direction.DOWN) == 0, "pad refuses top input");
                require(feed(level, pos, packets[i], Direction.UP) == packets[i], "pad accepts bottom input");
                level.getBlockState(pos).getBlock().stepOn(level, pos, level.getBlockState(pos), player); tick(level, pos);
                int each = Math.min(100, packets[i] / 2);
                require(charge(level, player.getMainHandItem()) == each && charge(level, player.getOffhandItem()) == each, "pad shares one packet between eligible held items");
                require(stored(level, pos) == packets[i] - each * 2, "pad only spends energy actually delivered");
                reload(level, pos); require(stored(level, pos) == packets[i] - each * 2, "pad buffer survives reload");
            }
            place(level, pos, "chargepad_lv");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, battery(level, "energy_crystal", 0)); player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            feed(level, pos, 32, Direction.UP); level.getBlockState(pos).getBlock().stepOn(level, pos, level.getBlockState(pos), player); tick(level, pos);
            require(charge(level, player.getMainHandItem()) == 0 && stored(level, pos) == 32, "LV pad honors item tier and preserves EU with no target");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("cooldown_ticks") == 20, "idle pad enters original cooldown");
        } finally { level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
    }
    private static void redirectors(ServerLevel level, BlockPos pos) {
        place(level, pos, "redirector_master");
        facing(level, pos.east(), "redirector_slave", Direction.WEST);
        facing(level, pos.south(), "redirector_slave", Direction.NORTH);
        place(level, pos.east(2), "charging_bench_iv"); place(level, pos.south(2), "charging_bench_iv");
        var player = FakePlayerFactory.getMinecraft(level); player.setPos(pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5);
        var menu = ((net.minecraft.world.MenuProvider) level.getBlockEntity(pos)).createMenu(2, player.getInventory(), player);
        require(menu.clickMenuButton(player, 12), "redirector GUI distributes shares");
        require(feed(level, pos, 101, Direction.SOUTH) == 101, "master receives on front");
        require(stored(level, pos.east()) + stored(level, pos.south()) == 101, "split queues conserve odd packet");
        reload(level, pos.east()); reload(level, pos.south()); reload(level, pos);
        tick(level, pos.east()); tick(level, pos.south());
        require(stored(level, pos.east(2)) == 50 && stored(level, pos.south(2)) == 51, "slave queues persist and emit exact shares");
        require(feed(level, pos, 32, Direction.NORTH) == 0, "master rejects input from rear");
        require(feed(level, pos, 2, Direction.SOUTH) == 2 && stored(level, pos) == 2, "tiny packet is buffered");
        reload(level, pos); require(feed(level, pos, 4, Direction.SOUTH) == 4 && stored(level, pos) == 0, "saved tiny packet combines with next input");
        tick(level, pos.east()); tick(level, pos.south());
        require(stored(level, pos.east(2)) + stored(level, pos.south(2)) == 107, "tiny packet buffering conserves EU");
        place(level, pos.east(2), "air"); place(level, pos.south(2), "air");
        require(feed(level, pos, 128, Direction.SOUTH) == 128, "blocked slaves retain delivered EU"); tick(level, pos.east()); tick(level, pos.south());
        require(stored(level, pos.east()) == 64 && stored(level, pos.south()) == 64, "failed delivery stores remainder");
        require(feed(level, pos, 128, Direction.SOUTH) == 0, "blocked slave buffers apply backpressure");
        reload(level, pos.east()); place(level, pos.east(2), "charging_bench_iv"); tick(level, pos.east()); tick(level, pos.east());
        require(stored(level, pos.east(2)) == 64 && stored(level, pos.east()) == 0, "failed packet retries in 32 EU packets after reload");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        require(level.isEmptyBlock(pos.east()) && level.isEmptyBlock(pos.south()), "removing master detaches its slave blocks");
    }
    private static void place(ServerLevel level, BlockPos pos, String id) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (!id.equals("air")) level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState());
    }
    private static void facing(ServerLevel level, BlockPos pos, String id, Direction direction) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState().setValue(BlockStateProperties.FACING, direction));
    }
    private static ItemStack battery(ServerLevel level, String id, int charge) {
        CompoundTag tag = new CompoundTag(), components = new CompoundTag(); tag.putString("id", "ic2:" + id); tag.putInt("count", 1);
        components.putInt("ic2:battery_charge", charge); tag.put("components", components);
        return ItemStack.parseOptional(level.registryAccess(), tag);
    }
    private static int charge(ServerLevel level, ItemStack stack) { return ((CompoundTag) stack.save(level.registryAccess())).getCompound("components").getInt("ic2:battery_charge"); }
    private static IItemHandler items(ServerLevel level, BlockPos pos) { return java.util.Objects.requireNonNull(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP)); }
    private static void insert(IItemHandler items, int slot, ItemStack stack) { require(!stack.isEmpty() && items.insertItem(slot, stack, false).isEmpty(), "device accepts valid battery"); }
    private static int feed(ServerLevel level, BlockPos pos, int amount, Direction direction) { return EuNetwork.send(level, pos.relative(direction.getOpposite()), amount, Set.of(direction), 0); }
    private static long stored(ServerLevel level, BlockPos pos) { return Ic2Api.machineInfo(level, pos).orElseThrow().energyStored(); }
    private static void tick(ServerLevel level, BlockPos pos) { ApiSmokeChecks.tick(level, pos, level.getBlockEntity(pos).getType()); }
    private static void reload(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos); var saved = entity.saveWithFullMetadata(level.registryAccess()); var state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        level.setBlockEntity(java.util.Objects.requireNonNull(BlockEntity.loadStatic(pos, state, saved, level.registryAccess())));
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
