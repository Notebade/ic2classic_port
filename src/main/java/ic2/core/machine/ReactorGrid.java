package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.UraniumFuelRod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorGrid {
    private final ItemStackHandler inventory;

    ReactorGrid(ItemStackHandler inventory) {
        this.inventory = inventory;
    }

    static boolean accepts(ItemStack stack) {
        return UraniumFuelRod.cellsIn(stack) > 0 || stack.is(IC2.HEAT_VENT.get());
    }

    Cycle run(int width, boolean powered, RandomSource random) {
        int output = 0;
        int heat = 0;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            if (slot % ReactorRules.COLUMNS >= width) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(IC2.HEAT_VENT.get())) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - ReactorRules.VENT_COOLING));
                continue;
            }
            int cells = UraniumFuelRod.cellsIn(stack);
            if (powered && cells > 0) {
                Cycle rod = runRod(slot, width, cells);
                output += rod.output();
                heat += rod.heat();
            }
        }
        if (powered) {
            ageFuel(width, random);
        }
        return new Cycle(output, heat);
    }

    private Cycle runRod(int slot, int width, int cells) {
        var vents = new ArrayList<Integer>(4);
        int neighboringRods = 0;
        for (int neighbor : ReactorRules.neighbors(slot, width)) {
            if (neighbor < 0) {
                continue;
            }
            ItemStack adjacent = inventory.getStackInSlot(neighbor);
            if (UraniumFuelRod.cellsIn(adjacent) > 0) {
                neighboringRods++;
            } else if (adjacent.is(IC2.HEAT_VENT.get())) {
                vents.add(neighbor);
            }
        }
        int heatPerCell = ReactorRules.heatPerCell(cells, neighboringRods);
        int hullHeat = 0;
        for (int cell = 0; cell < cells; cell++) {
            hullHeat += transferHeat(vents, heatPerCell);
        }
        return new Cycle(ReactorRules.energy(cells, neighboringRods), hullHeat);
    }

    private int transferHeat(List<Integer> vents, int heat) {
        for (int index = 0; index < vents.size() && heat > 0; index++) {
            int slot = vents.get(index);
            ItemStack stack = inventory.getStackInSlot(slot);
            int toStore = heat / (vents.size() - index);
            int room = stack.getMaxDamage() - stack.getDamageValue();
            heat -= Math.min(room, toStore);
            if (toStore > room) {
                vents.remove(index);
                index--;
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                stack.setDamageValue(stack.getDamageValue() + toStore);
            }
        }
        return heat;
    }

    private void ageFuel(int width, RandomSource random) {
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int cells = UraniumFuelRod.cellsIn(stack);
            if (slot % ReactorRules.COLUMNS < width && cells > 0) {
                int damage = stack.getDamageValue() + 1;
                if (damage > stack.getMaxDamage()) {
                    inventory.setStackInSlot(slot, random.nextInt(3) == 0
                        ? new ItemStack(IC2.DEPLETED_URANIUM_ROD.get(), cells) : ItemStack.EMPTY);
                } else {
                    stack.setDamageValue(damage);
                }
            }
        }
    }

    record Cycle(int output, int heat) {}
}
