package ic2.core.machine;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Runs the shared electric reactor calculation on the portable 5×5 inventory. */
public final class PortableReactor {
    private PortableReactor() {}
    public static boolean accepts(ItemStack stack) { return ReactorGrid.accepts(stack, false); }

    public static void tick(ItemStack pack, ItemStack armor, net.minecraft.world.entity.player.Player player) {
        var power = (ic2.api.item.ElectricItem) armor.getItem();
        var state = pack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        int tick = Math.max(0, state.getInt("reactor_tick"));
        boolean active = state.getBoolean("reactor_active");
        if (tick % 20 == 0) {
            active = active(power, armor, active);
            var result = cycle(pack, state.getInt("reactor_heat"), player.getRandom(), active);
            state.putInt("reactor_heat", result.heat()); state.putInt("reactor_output", result.output());
            state.putInt("reactor_max", result.capacity()); state.putInt("reactor_component_heat", result.componentHeat());
            if (result.heat() >= result.capacity() && GenerationConfig.REACTOR_DAMAGE.get() > 0) {
                armor.shrink(1);
                ReactorExplosion.detonatePortable((net.minecraft.server.level.ServerLevel) player.level(), player, result.blastPower());
                return;
            }
            ReactorHeatEffects.apply(player.level(), player.blockPosition(), result.heat(), result.capacity(), result.containment());
        }
        if (active) {
            power.setCharge(armor, (int) Math.min(power.capacity(armor), (long) power.charge(armor) + Math.max(0, state.getInt("reactor_output"))));
            active = active(power, armor, true);
        }
        state.putInt("reactor_tick", (tick + 1) % 20); state.putBoolean("reactor_active", active);
        pack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(state));
    }
    private static boolean active(ic2.api.item.ElectricItem item, ItemStack stack, boolean active) {
        int capacity = item.capacity(stack);
        return capacity > 0 && (active ? item.charge(stack) < capacity : item.charge(stack) <= capacity * .3);
    }

    public static Result cycle(ItemStack pack, int heat, RandomSource random) {
        return cycle(pack, heat, random, true);
    }
    private static Result cycle(ItemStack pack, int heat, RandomSource random, boolean active) {
        var inventory = new ItemStackHandler(ReactorRules.SLOT_COUNT);
        var parts = pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int i = 0; i < Math.min(25, parts.getSlots()); i++) {
            var part = parts.getStackInSlot(i);
            if (ReactorGrid.accepts(part, false)) inventory.setStackInSlot(gridSlot(i), part);
        }
        var grid = new ReactorGrid(inventory, null);
        var hull = grid.hull(5);
        int capacity = hull.capacity();
        var cycle = grid.run(5, heat, capacity, active, true, true, random);
        var output = new java.util.ArrayList<ItemStack>(27);
        for (int i = 0; i < 25; i++) {
            output.add(i < parts.getSlots() && !ReactorGrid.accepts(parts.getStackInSlot(i), false)
                ? parts.getStackInSlot(i) : inventory.getStackInSlot(gridSlot(i)));
        }
        for (int i = 25; i < parts.getSlots(); i++) output.add(parts.getStackInSlot(i));
        pack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(output));
        double componentHeat = 0; int components = 0;
        for (var stack : output) if (stack.getItem() instanceof ic2.core.item.ReactorHeatItem) {
            componentHeat += stack.getDamageValue() / (double) stack.getMaxDamage(); components++;
        }
        return new Result(cycle.output(), cycle.heat(), capacity, hull.containment(), grid.blastPower(hull.containment()),
            components == 0 ? 0 : (int) (componentHeat * 100 / components));
    }

    private static int gridSlot(int index) { return index / 5 * ReactorRules.COLUMNS + index % 5; }

    public record Result(int output, int heat, int capacity, float containment, float blastPower, int componentHeat) {}
}
