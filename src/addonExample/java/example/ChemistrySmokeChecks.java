package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

/** End-to-end production checks against public API/capabilities, in the isolated verification world. */
final class ChemistrySmokeChecks {
    private ChemistrySmokeChecks() {}
    static void verify(ServerLevel level, BlockPos pos) {
        try {
            gasifiers(level, pos);
            biofuel(level, pos);
            whiskyToGold(level, pos);
            blockedOutput(level, pos);
            com.mojang.logging.LogUtils.getLogger().info("IC2 chemistry production checks passed");
        } finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
    }
    private static void gasifiers(ServerLevel level, BlockPos pos) {
        place(level, pos, "wood_gassifier");
        insert(items(level, pos, Direction.UP), 0, new ItemStack(Items.OAK_LOG));
        insert(items(level, pos, Direction.UP), 1, new ItemStack(item("cell_empty")));
        insert(items(level, pos, Direction.DOWN), 0, new ItemStack(Items.COAL));
        run(level, pos, 160, 0);
        var products = items(level, pos, Direction.DOWN);
        require(products.getStackInSlot(1).is(Items.CHARCOAL) && products.getStackInSlot(2).is(item("cell_woodgas")), "solid gasifier produces charcoal and a full gas cell");
        require(tank(level, pos).fill(new FluidStack(fluid("woodgas"), 1000), FluidAction.EXECUTE) == 0, "gasifier tank is output-only");
        place(level, pos, "electric_wood_gassifier");
        insert(items(level, pos, Direction.UP), 0, new ItemStack(Items.OAK_LOG));
        int spent = run(level, pos, 80, 32);
        var state = Ic2Api.machineInfo(level, pos).orElseThrow();
        require(state.values().get("gas_mb") == 810 && spent - state.energyStored() == 80, "electric gasifier spends 80 EU per log");
        require(items(level, pos, Direction.DOWN).getStackInSlot(1).is(Items.CHARCOAL), "electric gasifier returns charcoal");
    }
    private static void biofuel(ServerLevel level, BlockPos pos) {
        place(level, pos, "refinery");
        fill(level, pos, Fluids.WATER, 2000);
        insert(items(level, pos, Direction.UP), 0, new ItemStack(item("plant_ball_compressed"), 2));
        run(level, pos, 250, 128);
        require(tank(level, pos).drain(new FluidStack(fluid("bio_fuel"), 500), FluidAction.SIMULATE).getAmount() == 500, "plant matter refines to biofuel");
        place(level, pos, "refinery");
        fill(level, pos, Fluids.WATER, 1000); fill(level, pos, fluid("woodgas"), 1000);
        int spent = run(level, pos, 500, 128);
        require(tank(level, pos).drain(new FluidStack(fluid("bio_fuel"), 100), FluidAction.SIMULATE).getAmount() == 100, "gas/water recipe accepts inverted input tanks");
        require(spent - Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 10000, "gas biofuel uses doubled operation time");
    }
    private static void whiskyToGold(ServerLevel level, BlockPos pos) {
        place(level, pos, "refinery"); fill(level, pos, Fluids.WATER, 4000);
        insert(items(level, pos, Direction.UP), 0, new ItemStack(item("flour"), 16));
        run(level, pos, 50, 128); reload(level, pos);
        require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("progress_units") == 50, "refinery progress survives reload");
        run(level, pos, 450, 128);
        require(tank(level, pos).drain(new FluidStack(fluid("whisky"), 1000), FluidAction.SIMULATE).getAmount() == 1000, "flour/water produces unrefined whisky");
        button(level, pos, 8); run(level, pos, 250, 128);
        ItemStack grist = items(level, pos, Direction.DOWN).extractItem(1, 1, false);
        require(grist.is(item("whisky_grist")), "unrefined whisky becomes grist");
        place(level, pos, "barrel"); fill(level, pos, Fluids.WATER, 16000);
        insert(items(level, pos, Direction.UP), 0, grist); run(level, pos, 1, 0); button(level, pos, 9);
        // Advance only this fixture to just before the first distillable age boundary.
        var saved = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        saved.getCompound("chemical").putInt("stage", 9); saved.getCompound("chemical").putInt("age", 1209599);
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess()); run(level, pos, 1, 0);
        require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("age_stage") == 10, "sealed barrel advances to ten-year whisky");
        var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
        require(drops.size() == 1 && drops.getFirst().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("ic2_barrel").getCompound("chemical").getInt("stage") == 10,
            "barrel loot preserves long ageing progress");
        reload(level, pos); button(level, pos, 10);
        require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("water_mb") == 900, "opening applies age and grist losses");
        insert(items(level, pos, Direction.UP), 2, new ItemStack(item("mug"))); run(level, pos, 1, 0);
        ItemStack whisky = items(level, pos, Direction.DOWN).extractItem(0, 1, false);
        require(whisky.is(item("mug_whisky")), "aged barrel bottles a whisky mug");
        place(level, pos, "refinery"); fill(level, pos, Fluids.WATER, 4000);
        insert(items(level, pos, Direction.UP), 0, whisky); run(level, pos, 250, 128);
        require(tank(level, pos).drain(new FluidStack(fluid("alcohol"), 10000), FluidAction.SIMULATE).getAmount() == 10000, "aged whisky distills to alcohol");
        button(level, pos, 8);
        insert(items(level, pos, Direction.UP), 0, new ItemStack(Items.GOLD_INGOT)); run(level, pos, 250, 128);
        var output = items(level, pos, Direction.DOWN);
        boolean gold = false;
        for (int slot = 0; slot < output.getSlots(); slot++) gold |= output.getStackInSlot(slot).is(item("ingot_polished_gold"));
        require(gold && Ic2Api.machineInfo(level, pos).orElseThrow().values().get("input_1_mb") == 9500, "distilled alcohol polishes gold for 500 mB per ingot");
    }
    private static void blockedOutput(ServerLevel level, BlockPos pos) {
        place(level, pos, "refinery");
        for (int batch = 0; batch < 4; batch++) { fill(level, pos, Fluids.WATER, 16000); button(level, pos, 6); }
        fill(level, pos, Fluids.WATER, 2000);
        insert(items(level, pos, Direction.UP), 0, new ItemStack(item("plant_ball_compressed"), 2));
        run(level, pos, 250, 128);
        require(items(level, pos, Direction.UP).getStackInSlot(0).getCount() == 2
            && Ic2Api.machineInfo(level, pos).orElseThrow().values().get("input_1_mb") == 2000, "full layered output preserves recipe inputs");
        require(tank(level, pos).drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).getAmount() == 1000, "output can be recovered");
        run(level, pos, 250, 128); reload(level, pos);
        require(tank(level, pos).drain(new FluidStack(fluid("bio_fuel"), 500), FluidAction.SIMULATE).getAmount() == 500
            && tank(level, pos).drain(new FluidStack(Fluids.WATER, 64000), FluidAction.SIMULATE).getAmount() == 63000, "distinct fluid layers survive reload without mixing");
    }
    private static Item item(String path) { return Ic2Api.item(ResourceLocation.parse("ic2:" + path)).orElseThrow(); }
    private static Fluid fluid(String path) { return Ic2Api.fluid(ResourceLocation.parse("ic2:" + path)).orElseThrow(); }
    private static IItemHandler items(ServerLevel level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler == null) throw new AssertionError("Missing item handler"); return handler;
    }
    private static IFluidHandler tank(ServerLevel level, BlockPos pos) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
        if (handler == null) throw new AssertionError("Missing fluid handler"); return handler;
    }
    private static void insert(IItemHandler inventory, int slot, ItemStack stack) { require(inventory.insertItem(slot, stack, false).isEmpty(), "fixture input accepted"); }
    private static void fill(ServerLevel level, BlockPos pos, Fluid fluid, int amount) { require(tank(level, pos).fill(new FluidStack(fluid, amount), FluidAction.EXECUTE) == amount, "fixture fluid accepted"); }
    private static void place(ServerLevel level, BlockPos pos, String id) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState());
    }
    private static int run(ServerLevel level, BlockPos pos, int ticks, int packet) {
        int sent = 0;
        for (int count = 0; count < ticks; count++) {
            if (packet > 0) sent += EuNetwork.send(level, pos.above(), packet, Set.of(Direction.DOWN), 0);
            ApiSmokeChecks.tick(level, pos, level.getBlockEntity(pos).getType());
        }
        return sent;
    }
    private static void button(ServerLevel level, BlockPos pos, int id) {
        var player = FakePlayerFactory.getMinecraft(level); player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider)) throw new AssertionError("Missing menu");
        var menu = provider.createMenu(1, player.getInventory(), player);
        require(menu != null && menu.clickMenuButton(player, id), "server menu button accepted");
    }
    private static void reload(ServerLevel level, BlockPos pos) {
        var entity = level.getBlockEntity(pos); var state = entity.getBlockState(); var saved = entity.saveWithFullMetadata(level.registryAccess());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, state);
        level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
