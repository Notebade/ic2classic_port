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
            checks.monitorWall(new BlockPos(48, 300, 54));
            checks.planner(new BlockPos(56, 300, 48));
            checks.reactorSchemes(new BlockPos(56, 300, 56));
            checks.pipes(new BlockPos(64, 300, 48));
            checks.generators(new BlockPos(72, 300, 48));
            checks.fusion(new BlockPos(16, 300, 15));
            checks.tunnel(new BlockPos(32, 300, 15));
            checks.boiler(new BlockPos(40, 300, 15));
            checks.competingFusion(new BlockPos(112, 300, 112));
            checks.storageDependencies(new BlockPos(128, 300, 128));
            checks.blast(new BlockPos(160, 300, 160));
        } finally {
            for (BlockPos pos : checks.placed) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private void blast(BlockPos pos) {
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            var block = pos.offset(x, y, z); placed.add(block); level.getChunkAt(block);
            level.setBlockAndUpdate(block, x == 0 && y == 0 && z == 0 ? Blocks.AIR.defaultBlockState() : Blocks.BEDROCK.defaultBlockState());
        }
        var protectedBlock = pos.east(3); placed.add(protectedBlock);
        level.setBlockAndUpdate(protectedBlock, Blocks.STONE.defaultBlockState());
        ic2.core.machine.ReactorExplosion.detonate(level, pos, 8);
        require(level.getBlockState(pos.east()).is(Blocks.BEDROCK) && level.getBlockState(protectedBlock).is(Blocks.STONE), "original bedrock whitelist blocks nuclear rays");
        var open = pos.east(16); var stone = open.east();
        placed.add(stone); level.getChunkAt(stone); level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        ic2.core.machine.ReactorExplosion.detonate(level, open, 8);
        require(level.getBlockState(stone).isAir(), "original nuclear rays destroy unshielded stone");
        var border = new BlockPos(48015, 300, 48015); level.getChunkAt(border);
        require(!level.hasChunkAt(border.east(2)), "blast neighbour fixture starts unloaded");
        ic2.core.machine.ReactorExplosion.detonate(level, border, 2);
        require(!level.hasChunkAt(border.east(2)), "nuclear blast never forces unloaded chunks");
    }

    private void storageDependencies(BlockPos pos) {
        place(pos, "machine_buffer");
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(items != null && items.getSlots() == 9, "original machine buffer has nine storage slots");
        require(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 7), false).isEmpty(), "buffer accepts actual items");
        require(feed(pos, 32) == 32, "buffer accepts LV power");
        reload(pos);
        items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(items.getStackInSlot(0).getCount() == 7, "buffer inventory persists");
        place(pos.east(2), "personal_chest");
        var owner = FakePlayerFactory.getMinecraft(level);
        var chest = level.getBlockState(pos.east(2));
        chest.getBlock().setPlacedBy(level, pos.east(2), chest, owner, stack("personal_chest"));
        var stranger = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("f3df69b5-3179-4e6b-9417-503a967c2a65"), "StorageStranger"));
        var provider = (MenuProvider) level.getBlockEntity(pos.east(2));
        require(provider.createMenu(1, stranger.getInventory(), stranger) == null, "personal chest rejects another player");
        require(level.getCapability(Capabilities.ItemHandler.BLOCK, pos.east(2), Direction.UP) == null, "personal chest exposes no public automation");
        var broken = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos.east(2), chest, stranger);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(broken);
        require(broken.isCanceled(), "personal chest rejects another player's breaking action");
        var box = stack("toolbox");
        var savedItems = net.minecraft.core.NonNullList.withSize(8, ItemStack.EMPTY);
        savedItems.set(0, new ItemStack(Items.IRON_INGOT, 2)); savedItems.set(1, new ItemStack(Items.GOLD_INGOT, 3));
        box.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(savedItems));
        var toolboxUser = new net.neoforged.neoforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.fromString("90588517-51e5-4dab-835e-21027a3c72ae"), "ToolboxTester")) {
            @Override public java.util.OptionalInt openMenu(MenuProvider provider) {
                containerMenu = provider.createMenu(42, getInventory(), this);
                return java.util.OptionalInt.of(42);
            }
        };
        toolboxUser.setItemInHand(InteractionHand.MAIN_HAND, box);
        box.getItem().use(level, toolboxUser, InteractionHand.MAIN_HAND);
        var menu = toolboxUser.containerMenu;
        require(menu.getSlot(0).getItem().is(Items.IRON_INGOT) && menu.getSlot(1).getItem().is(Items.GOLD_INGOT), "toolbox preserves distinct stacks on opening");
        require(!menu.getSlot(8).mayPlace(new ItemStack(Items.DIRT)) && !menu.getSlot(0).mayPlace(box), "toolbox has eight usable slots and cannot contain itself");
        toolboxUser.closeContainer();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("ic2:machine_buffer")).orElseThrow().value();
        var grid = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(1, stack("upgrade_base")); grid.set(3, stack("circuit")); grid.set(4, stack("toolbox")); grid.set(5, stack("circuit")); grid.set(7, stack("machine_block"));
        require(recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid), level), "original machine buffer recipe accepts an empty toolbox");
        grid.set(4, box);
        require(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid), level), "crafting never destroys a filled toolbox");
        var iron = stack("ingot_refined_iron");
        require(crafts("toolbox", 3, 2, java.util.List.of(iron, new ItemStack(Items.CHEST), iron, iron, iron, iron)), "original toolbox recipe resolves live chest and iron ingredients");
        require(crafts("personal_chest", 1, 3, java.util.List.of(stack("circuit"), stack("machine_block"), new ItemStack(Items.CHEST))), "original personal chest recipe resolves live ingredients");
        for (var tank : new String[]{"machine_tank", "personal_tank"}) {
            var tankGrid = net.minecraft.core.NonNullList.withSize(9, new ItemStack(Items.GLASS));
            tankGrid.set(4, stack(tank.equals("machine_tank") ? "machine_buffer" : "personal_chest"));
            require(crafts(tank, 3, 3, tankGrid), "original tank recipe uses its working dependency: " + tank);
        }
    }

    private boolean crafts(String id, int width, int height, java.util.List<ItemStack> grid) {
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("ic2:" + id)).orElseThrow().value();
        return recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(width, height, grid), level);
    }

    private void reactorSchemes(BlockPos pos) {
        String[] families = {"", "blaze_", "charcoal_", "ender_pearl_", "nether_star_", "redstone_"};
        String[] sizes = {"single", "dual", "quad"};
        int[] cells = {1, 2, 4}, pulses = {1, 1, 1, 1, 2, 2}, outputs = {20, 20, 12, 40, 100, 20};
        int[] heatPercent = {100, 400, 60, 25, 120, 50};
        for (int family = 0; family < families.length; family++) for (int size = 0; size < sizes.length; size++) {
            place(pos, "reactor_planner");
            var rod = stack("uranium_rod_" + families[family] + sizes[size]);
            var layout = new CompoundTag(); layout.putInt("Size", 54);
            var items = new ListTag(); var item = (CompoundTag) rod.save(level.registryAccess());
            item.putInt("Slot", 0); items.add(item); layout.put("Items", items);
            var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
            saved.getCompound("reactor_plan").put("layout", layout);
            level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
            feed(pos, 128); button(pos, 4); button(pos, 2); tick(pos);
            var result = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()).getCompound("reactor_plan");
            int self = (1 + cells[size] / 2) * pulses[family];
            int expectedOutput = cells[size] * self * outputs[family];
            int expectedHeat = cells[size] * (self * (self + 1) * 2 * heatPercent[family] / 100);
            require(result.getInt("output") == expectedOutput && result.getInt("heat") == expectedHeat,
                "original first cycle output and heat: " + families[family] + sizes[size]
                    + " actual=" + result.getInt("output") + "/" + result.getInt("heat")
                    + " expected=" + expectedOutput + "/" + expectedHeat);
            require(result.getCompound("simulation").getList("Items", 10).getCompound(0)
                .getCompound("components").getInt("minecraft:damage") == 1, "original one fuel wear per electric cycle");
        }
    }

    private void competingFusion(BlockPos first) {
        BlockPos second = first.offset(-8, 0, 4), valve = first.offset(-4, -2, 2);
        for (var controller : new BlockPos[]{first, second}) {
            boolean reverse = controller.equals(second);
            for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
                if (x == 0 && y == 0 && z == 0) continue;
                int edges = (x == 0 || x == 4 ? 1 : 0) + (y == 0 || y == 4 ? 1 : 0) + (z == 0 || z == 4 ? 1 : 0);
                place(controller.offset(reverse ? x : -x, -y, reverse ? -z : z), edges == 0 ? "air" : edges >= 2 || y == 4 ? "reinforced_stone" : "reinforced_glass");
            }
            place(controller, "fusion_reactor");
            if (reverse) level.setBlockAndUpdate(controller, level.getBlockState(controller).setValue(BlockStateProperties.FACING, Direction.SOUTH));
        }
        place(valve, "valve");
        tick(first); tick(second);
        require(info(first, "formed") == 1 && info(second, "formed") == 0, "competing controllers cannot claim the same valve");
        place(first, "air"); tick(second);
        require(info(second, "formed") == 1, "surviving controller reclaims a valve after the old owner is removed");
    }

    private void monitorWall(BlockPos pos) {
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) place(pos.offset(x, -y, 0), "monitor");
        // A north-facing screen's left edge is east in world coordinates.
        BlockPos master = pos.east();
        button(master, 0);
        button(master, 1);
        button(master, 3);
        button(master, 4);
        require(feed(pos, 128) == 128, "wall accepts EU through a non-master panel");
        tick(master);
        var tag = level.getBlockEntity(master).getUpdateTag(level.registryAccess());
        require(tag.getInt("screen_width") == 2 && tag.getInt("screen_height") == 2 && tag.getBoolean("screen_master"), "four panels form a 2x2 screen");
        require(energy(pos) == 126, "wall charges max(width,height) from pooled panel energy");
        reload(master);
        tag = level.getBlockEntity(master).getUpdateTag(level.registryAccess());
        require(tag.getInt("color") == 6 && tag.getInt("scale") == 5 && tag.getInt("alignment") == 1, "monitor appearance persists");
        place(pos.below(), "air");
        tick(master);
        tag = level.getBlockEntity(master).getUpdateTag(level.registryAccess());
        require(tag.getInt("screen_width") == 1 && tag.getInt("screen_height") == 1, "incomplete rectangles fall back to single panels");
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
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        require(items != null && items.insertItem(6, stack("cobblegen_upgrade"), false).isEmpty(), "fusion accepts cobblestone generator upgrade as fuel");
        for (int i = 0; i < 10; i++) tick(pos);
        require(items.getStackInSlot(6).getCount() == 1 && info(pos, "material_units") == 10000, "infinite cobblestone fills material without consuming upgrade");
    }

    private void tunnel(BlockPos pos) {
        place(pos, "steam_tunnel");
        tick(pos);
        require(!level.getBlockState(pos).getValue((net.minecraft.world.level.block.state.properties.BooleanProperty) level.getBlockState(pos).getBlock().getStateDefinition().getProperty("formed")), "unbuilt steam tunnel never renders a 3x3 turbine");
        for (int z = 0; z < 5; z++) for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            int edges = (Math.abs(x) == 1 ? 1 : 0) + (Math.abs(y) == 1 ? 1 : 0) + (z == 0 || z == 4 ? 1 : 0);
            place(pos.offset(-x, -y, z), edges == 0 ? "air" : "steam_tunnel_structure");
        }
        BlockPos valve = pos.offset(-1, 0, 2);
        place(valve, "valve");
        tick(pos);
        require(info(pos, "formed") == 1, "steam tunnel forms across chunk boundary");
        require(level.getBlockState(pos).getValue((net.minecraft.world.level.block.state.properties.BooleanProperty) level.getBlockState(pos).getBlock().getStateDefinition().getProperty("formed")), "formed tunnel publishes its assembled render state");
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
