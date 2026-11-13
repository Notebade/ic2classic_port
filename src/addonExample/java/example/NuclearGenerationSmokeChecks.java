package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Isolated fixtures exercise real menus, public telemetry, packet input and cross-chunk structures.
 */
final class NuclearGenerationSmokeChecks {
    private final ServerLevel level;
    private final Set<BlockPos> placed = new HashSet<>();

    private NuclearGenerationSmokeChecks(ServerLevel level) {
        this.level = level;
    }

    static void verify(ServerLevel level) {
        var checks = new NuclearGenerationSmokeChecks(level);
        try {
            checks.windSave();
            checks.monitor(new BlockPos(48, 300, 48));
            checks.planner(new BlockPos(56, 300, 48));
            checks.pipes(new BlockPos(64, 300, 48));
            checks.generators(new BlockPos(72, 300, 48));
            checks.fusion(new BlockPos(16, 300, 15));
            checks.tunnel(new BlockPos(32, 300, 15));
            checks.boiler(new BlockPos(40, 300, 15));
        } finally {
            for (BlockPos pos : checks.placed) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    private void windSave() {
        level.getDataStorage().save();
        net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
        var file = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/ic2_wind.dat");
        try {
            var tag = net.minecraft.nbt.NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.unlimitedHeap()).getCompound("data");
            require(tag.contains("seed") && Math.abs(tag.getDouble("current")) <= 100 && tag.contains("remaining"), "shared wind state is saved in the actual world data");
        } catch (java.io.IOException error) { throw new AssertionError("wind world data did not persist", error); }
    }

    private void monitor(BlockPos pos) {
        BlockPos reactor = pos.east(3);
        place(reactor, "nuclear_reactor");
        place(pos, "monitor");
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(reactor.getX() + 0.5, 300, reactor.getZ() + 0.5);
        var card = stack("reactor_card");
        player.setItemInHand(InteractionHand.MAIN_HAND, card);
        card.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(reactor), Direction.UP, reactor, false)));
        require(card.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong("reactor_position") == reactor.asLong(), "reactor card binds through normal use");
        var slots = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(slots != null && slots.getSlots() == 8 && slots.insertItem(0, card.copy(), false).isEmpty(), "monitor accepts eight reactor-card slots");
        require(slots.insertItem(1, new ItemStack(Items.DIRT), true).is(Items.DIRT), "monitor rejects non-card items");
        require(feed(pos, 128) == 128, "monitor accepts EU");
        tick(pos);
        require(info(pos, "requires_redstone") == 1 && energy(pos) == 128, "monitor waits for redstone by default");
        player.setPos(pos.getX() + 0.5, 300, pos.getZ() + 0.5);
        var menu = ((MenuProvider)level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
        require(menu.clickMenuButton(player, 0), "monitor redstone mode toggles through GUI");
        tick(pos);
        require(energy(pos) == 127 && level.getBlockEntity(pos).getUpdateTag(level.registryAccess()).getInt("line_count") == 1, "powered monitor publishes reactor status for world rendering");
        reload(pos);
        require(energy(pos) == 127 && info(pos, "requires_redstone") == 0, "monitor card, EU and control persist");
        var far = new BlockPos(9000000, 300, 9000000);
        require(!level.hasChunkAt(far), "distant monitor fixture is unloaded");
        var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        var entries = saved.getCompound("cards").getList("Items", 10);
        entries.getCompound(0).getCompound("components").getCompound("minecraft:custom_data").putLong("reactor_position", far.asLong());
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        menu = ((MenuProvider)level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
        menu.clickMenuButton(player, 0);
        tick(pos);
        menu.clickMenuButton(player, 0);
        tick(pos);
        require(!level.hasChunkAt(far), "monitor never loads linked reactor chunks");
        player.setPos(pos.getX() + 100, 300, pos.getZ());
        require(!menu.clickMenuButton(player, 0), "remote monitor configuration is rejected");
    }

    private void planner(BlockPos pos) {
        place(pos, "reactor_planner");
        var rod = stack("uranium_rod_single");
        rod.setDamageValue(rod.getMaxDamage() - 2);
        var layout = new CompoundTag();
        layout.putInt("Size", 54);
        var items = new ListTag();
        var item = (CompoundTag)rod.save(level.registryAccess());
        item.putInt("Slot", 0);
        items.add(item);
        layout.put("Items", items);
        var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        saved.getCompound("reactor_plan").put("layout", layout);
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        require(feed(pos, 128) > 0, "planner receives simulation energy");
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(pos.getX() + 0.5, 300, pos.getZ() + 0.5);
        var menu = ((MenuProvider)level.getBlockEntity(pos)).createMenu(2, player.getInventory(), player);
        require(menu.clickMenuButton(player, 14), "full fuel run starts through planner GUI");
        tick(pos);
        require(info(pos, "full_run_state") == 2 && info(pos, "simulation_cycles") > 0 && info(pos, "simulation_cycles") < 10, "full run stops when the actual fuel is exhausted");
        require(info(pos, "fuel_runtime_ticks") == info(pos, "simulation_cycles") * 20 && info(pos, "fuel_remaining_ticks") == 0, "planner reports complete fuel runtime and zero remaining fuel");
        require(info(pos, "total_production") > 0 && info(pos, "efficiency_permille") > 0, "full-run totals include production and efficiency");
        long total = info(pos, "total_production");
        reload(pos);
        require(info(pos, "total_production") == total && info(pos, "full_run_state") == 2, "full-run results persist without restarting simulation");
        saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        saved.getCompound("reactor_plan").putInt("starting_heat", 9999);
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        feed(pos, 128);
        button(pos, 14);
        tick(pos);
        require(info(pos, "full_run_state") == 3 && info(pos, "fuel_remaining_ticks") > 0, "full run reports overheating instead of claiming a complete fuel burn");
        saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        saved.getCompound("reactor_plan").putInt("starting_heat", 0);
        saved.getCompound("reactor_plan").putBoolean("steam", true);
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        feed(pos, 128);
        button(pos, 14);
        tick(pos);
        require(info(pos, "full_run_state") == 2 && info(pos, "simulation_cycles") > 3 && info(pos, "fuel_runtime_ticks") == info(pos, "simulation_cycles"), "steam full run uses one-tick cycles and twenty-tick fuel wear");
        require(info(pos, "efficiency_permille") == -1, "steam volume is not mislabeled as electrical efficiency");
    }

    private void pipes(BlockPos pos) {
        place(pos, "pressure_pipe");
        place(pos.east(), "pipe");
        place(pos.east(2), "machine_tank");
        var source = tank(pos);
        require(source.fill(new FluidStack(Fluids.WATER, 3000), FluidAction.EXECUTE) == 3000, "pipe buffer accepts fluid");
        require(source.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE) == 0, "pipe does not mix fluids");
        tick(pos);
        require(tank(pos.east(2)).getFluidInTank(0).getAmount() == 1000 && source.getFluidInTank(0).getAmount() == 2000, "pressure pipe routes exact fluid through a connected pipe");
        reload(pos);
        tick(pos);
        require(tank(pos.east(2)).getFluidInTank(0).getAmount() == 2000 && tank(pos).getFluidInTank(0).getAmount() == 1000, "pipe fluid persists across reload");
        place(pos.east(2), "air");
        tick(pos);
        require(tank(pos).getFluidInTank(0).getAmount() == 1000, "blocked pipe preserves fluid");
    }

    private void generators(BlockPos pos) {
        place(pos, "solar_turbine");
        long oldTime = level.getDayTime();
        level.setDayTime(6000);
        level.updateSkyBrightness();
        try {
            require(tank(pos).fill(new FluidStack(Fluids.WATER, 20000), FluidAction.EXECUTE) == 20000, "solar turbine accepts original physical water volume");
            for (int i = 0; i < 256; i++) tick(pos);
            require(info(pos, "heat") > 0 && energy(pos) > 0, "solar turbine heats and generates actual EU in sunlight");
            long heat = info(pos, "heat");
            reload(pos);
            require(info(pos, "heat") == heat, "solar turbine heat persists");
            level.setDayTime(18000);
            level.updateSkyBrightness();
            for (int i = 0; i < 128; i++) tick(pos);
            long nightHeat = info(pos, "heat");
            tick(pos);
            require(info(pos, "heat") < nightHeat, "solar turbine cools after night resampling");
        } finally {
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        place(pos, "slag_generator");
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(items != null, "slag generator exposes automation");
        boolean inserted = false;
        for (int slot = 0; slot < items.getSlots(); slot++) if (items.insertItem(slot, new ItemStack(Items.COAL), true).isEmpty()) {
            items.insertItem(slot, new ItemStack(Items.COAL), false);
            inserted = true;
            break;
        }
        require(inserted, "slag generator accepts furnace fuel");
        tick(pos);
        tick(pos);
        require(energy(pos) > 0 && info(pos, "fuel_ticks") > 0, "slag generator burns fuel and produces EU");
        long stored = energy(pos);
        long fuel = info(pos, "fuel_ticks");
        reload(pos);
        require(energy(pos) == stored && info(pos, "fuel_ticks") == fuel, "slag generator burning state persists");
    }

    private void fusion(BlockPos pos) {
        place(pos, "fusion_reactor");
        BlockPos valve = pos.offset(-2, 0, 2);
        for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            int edges = (x == 0 || x == 4 ? 1 : 0) + (y == 0 || y == 4 ? 1 : 0) + (z == 0 || z == 4 ? 1 : 0);
            place(pos.offset(-x, -y, z), edges == 0 ? "air" : edges >= 2 || y == 4 ? "reinforced_stone" : "reinforced_glass");
        }
        place(valve, "valve");
        tick(pos);
        require(info(pos, "formed") == 1, "fusion structure forms across two chunk boundaries with original valve");
        var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        int[] state = saved.getIntArray("state");
        state[6] = 11000;
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        var cached = tank(valve);
        require(cached.drain(1000, FluidAction.EXECUTE).getAmount() == 1000, "fusion valve drains lava above 10000 mB heat reserve");
        require(cached.drain(1000, FluidAction.EXECUTE).isEmpty(), "fusion valve preserves heat reserve");
        BlockPos glass = pos.offset(-1, 0, 1);
        place(glass, "air");
        tick(pos);
        require(info(pos, "formed") == 0 && cached.drain(1000, FluidAction.EXECUTE).isEmpty(), "broken structure invalidates a cached valve capability");
        require(info(pos, "lava_mb") == 10000, "broken fusion structure preserves controller reserve");
        place(glass, "reinforced_glass");
        tick(pos);
        require(info(pos, "formed") == 1, "fusion structure reforms without controller replacement");
    }

    private void tunnel(BlockPos pos) {
        place(pos, "steam_tunnel");
        for (int z = 0; z < 5; z++) for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            int edges = (Math.abs(x) == 1 ? 1 : 0) + (Math.abs(y) == 1 ? 1 : 0) + (z == 0 || z == 4 ? 1 : 0);
            place(pos.offset(-x, -y, z), edges == 0 ? "air" : "steam_tunnel_structure");
        }
        BlockPos valve = pos.offset(-1, 0, 2);
        place(valve, "valve");
        tick(pos);
        require(info(pos, "formed") == 1, "steam tunnel forms across chunk boundary");
        var steam = Ic2Api.fluid(ResourceLocation.parse("ic2:steam")).orElseThrow();
        var cached = tank(valve);
        require(cached.fill(new FluidStack(steam, 4096), FluidAction.EXECUTE) == 4096, "generic valve feeds steam tunnel");
        tick(pos);
        require(energy(pos) == 2048 && info(pos, "steam_mb") == 0 && info(pos, "water_mb") == 25, "steam tunnel converts four real packets and preserves water remainder");
        reload(pos);
        require(energy(pos) == 2048, "steam tunnel EU persists");
        place(pos.offset(-1, -1, 0), "air");
        tick(pos);
        require(info(pos, "formed") == 0 && cached.fill(new FluidStack(steam, 1024), FluidAction.EXECUTE) == 0, "broken tunnel rejects input through cached valve");
    }

    private void boiler(BlockPos pos) {
        place(pos, "fuel_boiler");
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) if (x != 0 || y != 0 || z != 0) place(pos.offset(-x, -y, z), "fuel_boiler_structure");
        tick(pos);
        require(info(pos, "formed") == 1, "fuel boiler forms across chunk boundary");
        var cached = tank(pos.below());
        require(cached.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE) == 4000, "boiler casing forwards water input");
        var can = stack("fuel_can");
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, can, (tag)->tag.putInt("fuel", 1000));
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(items != null && items.insertItem(2, can, false).isEmpty(), "boiler accepts filled fuel can");
        tick(pos);
        require(info(pos, "fuel_units") == 1000 && items.getStackInSlot(3).is(can.getItem()), "boiler consumes fuel and returns empty can");
        var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        int[] state = saved.getIntArray("state");
        state[1] = 24000;
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
        for (int i = 0; i < 5; i++) tick(pos);
        require(info(pos, "steam_mb") == 5120 && info(pos, "water_mb") == 3968 && info(pos, "fuel_units") == 960, "boiler preserves exact 1:160 water-to-steam conversion and fuel consumption");
        reload(pos);
        require(info(pos, "steam_mb") == 5120, "boiler water and steam persist");
        place(pos.offset(-1, -1, 1), "air");
        tick(pos);
        require(info(pos, "formed") == 0 && cached.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0, "broken boiler rejects cached casing access");
        require(info(pos, "steam_mb") == 5120 && info(pos, "fuel_units") == 960, "broken boiler pauses without discarding fuel or steam");
    }

    private void place(BlockPos pos, String id) {
        placed.add(pos.immutable());
        level.getChunkAt(pos);
        BlockState state = id.equals("air") ? Blocks.AIR.defaultBlockState() : Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (!state.isAir()) level.setBlockAndUpdate(pos, state);
    }

    private ItemStack stack(String id) {
        return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow());
    }

    private int feed(BlockPos pos, int amount) {
        level.getChunkAt(pos.above());
        return EuNetwork.send(level, pos.above(), amount, Set.of(Direction.DOWN), 0);
    }

    private IFluidHandler tank(BlockPos pos) {
        return java.util.Objects.requireNonNull(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP));
    }

    private long energy(BlockPos pos) {
        return Ic2Api.machineInfo(level, pos).orElseThrow().energyStored();
    }

    private long info(BlockPos pos, String key) {
        return java.util.Objects.requireNonNull(Ic2Api.machineInfo(level, pos).orElseThrow().values().get(key), key);
    }

    private void tick(BlockPos pos) {
        ApiSmokeChecks.tick(level, pos, level.getBlockEntity(pos).getType());
    }

    private void button(BlockPos pos, int command) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var menu = ((MenuProvider)level.getBlockEntity(pos)).createMenu(3, player.getInventory(), player);
        require(menu.clickMenuButton(player, command), "device GUI command is accepted");
    }

    private void reload(BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        var tag = entity.saveWithFullMetadata(level.registryAccess());
        var state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        level.setBlockEntity(java.util.Objects.requireNonNull(BlockEntity.loadStatic(pos, state, tag, level.registryAccess())));
    }

    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
