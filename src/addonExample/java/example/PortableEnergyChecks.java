package example;

import ic2.api.Ic2Api;
import ic2.core.item.RechargeableItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

final class PortableEnergyChecks {
    static void run(ServerLevel level) {
        String[] names = {"re_battery", "energy_crystal", "lapatron_crystal", "glowtronic_crystal", "uesc", "pesd", "quantum_accumulator", "quantum_accumulator_big"};
        int[] capacities = {10000, 100000, 1000000, 7500000, 17500000, 50000000, 900000, 5400000};
        for (int i = 0; i < names.length; i++) require(((RechargeableItem) stack(names[i]).getItem()).capacity() == capacities[i], "source portable energy catalog: " + names[i]);
        require(Ic2Api.item(ResourceLocation.parse("ic2:su_battery")).isPresent(), "source disposable battery exists in addition to all eight rechargeable stores");
        String[] dusts = {"dust_coal", "dust_hydrated_coal", "dust_charcoal"}; int[] outputs = {6, 8, 4};
        for (int material = 0; material < dusts.length; material++) for (boolean reversed : new boolean[]{false, true}) {
            var redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE); var dust = stack(dusts[material]);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 3, java.util.List.of(stack("copper_insulated_cable_item"), reversed ? dust : redstone, reversed ? redstone : dust));
            var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow();
            var result = recipe.value().assemble(input, level.registryAccess());
            require(result.is(stack("su_battery").getItem()) && result.getCount() == outputs[material], "disposable battery source recipe yield for " + dusts[material]);
        }
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(index -> player.getInventory().getItem(index).copy()).toList();
        int selected = player.getInventory().selected;
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            var disposable = stack("su_battery").copyWithCount(2);
            var drill = stack("drill"); var scanner = stack("od_scanner");
            ((RechargeableItem) drill.getItem()).setCharge(drill, 9900);
            player.getInventory().setItem(0, disposable); player.getInventory().setItem(1, drill); player.getInventory().setItem(2, scanner);
            disposable.getItem().use(level, player, InteractionHand.MAIN_HAND);
            require(disposable.getCount() == 1 && ((RechargeableItem) drill.getItem()).charge(drill) == 10000
                && ((RechargeableItem) scanner.getItem()).charge(scanner) == 900, "one disposable supplies exactly 1000 EU over the hotbar");
            ((RechargeableItem) scanner.getItem()).setCharge(scanner, 10000);
            disposable.getItem().use(level, player, InteractionHand.MAIN_HAND);
            require(disposable.getCount() == 1, "disposable is preserved when no eligible item needs charge");
            var portable = stack("re_battery"); var cell = (RechargeableItem) portable.getItem(); cell.setCharge(portable, 1000);
            ((RechargeableItem) drill.getItem()).setCharge(drill, 0); ((RechargeableItem) scanner.getItem()).setCharge(scanner, 0);
            player.getInventory().setItem(0, portable);
            cell.use(level, player, InteractionHand.MAIN_HAND); cell.onUseTick(level, player, portable, 19999);
            require(cell.charge(portable) == 800 && ((RechargeableItem) drill.getItem()).charge(drill) == 200
                && ((RechargeableItem) scanner.getItem()).charge(scanner) == 0, "portable hold-use supplies one hotbar target at twice the source transfer rate");
            player.getInventory().setItem(1, new ItemStack(ExampleAddon.EU_TEST_ITEM.get())); player.getInventory().setItem(2, ItemStack.EMPTY);
            var addon = player.getInventory().getItem(1); var addonCell = (ic2.api.item.ElectricItem) addon.getItem();
            cell.onUseTick(level, player, portable, 19998);
            require(addonCell.charge(addon) == 200 && cell.charge(portable) == 600, "portable battery charges a plain addon ElectricItem through the public contract");
            var pos = new net.minecraft.core.BlockPos(530, 300, 530); level.getChunkAt(pos);
            try {
                level.setBlock(pos, Ic2Api.block(ResourceLocation.parse("ic2:batbox")).orElseThrow().defaultBlockState(), 2 | 16);
                var entity = level.getBlockEntity(pos); ((ic2.api.energy.EnergySink) entity).receiveEnergy(1000);
                var menu = ((net.minecraft.world.MenuProvider) entity).createMenu(0, player.getInventory(), player);
                require(menu.slots.get(0).safeInsert(addon.copy()).isEmpty(), "native storage accepts a plain addon EU item");
                ApiSmokeChecks.tick(level, pos, entity.getType());
                require(addonCell.charge(menu.slots.get(0).getItem()) == 300 && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 900, "real batbox charging conserves addon and machine EU");
            } finally { level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16); }
        } finally { player.stopUsingItem(); for (int i = 0; i < saved.size(); i++) player.getInventory().setItem(i, saved.get(i)); player.getInventory().selected = selected; }
    }
    private static ItemStack stack(String name) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
