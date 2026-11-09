package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.ReactorHeatItem;
import ic2.core.item.UraniumFuelRod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorGrid {
    private final ItemStackHandler inventory;
    private final ReactorHeatTransfer heatTransfer;

    ReactorGrid(ItemStackHandler inventory) {
        this.inventory = inventory;
        heatTransfer = new ReactorHeatTransfer(inventory);
    }

    static boolean accepts(ItemStack stack) {
        return UraniumFuelRod.cellsIn(stack) > 0 || stack.getItem() instanceof ReactorHeatItem
            || stack.getItem() instanceof ReactorHeatItem.Spreader || isReflector(stack);
    }

    static boolean isReflector(ItemStack stack) {
        return stack.is(IC2.NEUTRON_REFLECTOR.get()) || stack.is(IC2.THICK_NEUTRON_REFLECTOR.get());
    }

    Cycle run(int width, int hullHeat, boolean powered, RandomSource random) {
        CoolingPass cooling = runHeatPass(width, hullHeat, powered);
        int heat = finishHeatTransfer(width, cooling.hullHeat());
        if (!powered) {
            return new Cycle(0, heat);
        }
        // Reflectors still affect heat in the cycle when an energy pulse breaks them.
        int output = runEnergyPass(width);
        ageFuel(width, random);
        int netOutput = Math.max(0, (output * 10 - cooling.costTenths()) / 10);
        return new Cycle(netOutput, heat);
    }

    private CoolingPass runHeatPass(int width, int heat, boolean powered) {
        int costTenths = 0;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            if (slot % ReactorRules.COLUMNS >= width) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ReactorHeatItem.Vent vent) {
                int drained = Math.min(heat, vent.hullCooling(powered));
                if (heatTransfer.store(slot, stack, drained) == 0) {
                    heat -= drained;
                    costTenths += vent.powerCostTenths();
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - vent.selfCooling(powered)));
                }
                continue;
            }
            if (stack.getItem() instanceof ReactorHeatItem.Exchanger exchanger) {
                heat = heatTransfer.exchange(slot, width, stack, exchanger, heat);
            } else if (stack.getItem() instanceof ReactorHeatItem.Spreader) {
                heatTransfer.coolNeighbors(slot, width);
            }
            int cells = UraniumFuelRod.cellsIn(stack);
            if (powered && cells > 0) {
                heat += runRodHeat(slot, width, cells);
            }
        }
        return new CoolingPass(heat, costTenths);
    }

    private int finishHeatTransfer(int width, int heat) {
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (slot % ReactorRules.COLUMNS < width && stack.getItem() instanceof ReactorHeatItem.Exchanger exchanger) {
                heat = heatTransfer.exchange(slot, width, stack, exchanger, heat);
            }
        }
        return heat;
    }

    private int runRodHeat(int slot, int width, int cells) {
        var vents = new ArrayList<Integer>(4);
        int pulseNeighbors = 0;
        for (int neighbor : ReactorRules.neighbors(slot, width)) {
            if (neighbor < 0) {
                continue;
            }
            ItemStack adjacent = inventory.getStackInSlot(neighbor);
            if (UraniumFuelRod.cellsIn(adjacent) > 0 || isReflector(adjacent)) {
                pulseNeighbors++;
            } else if (ReactorHeatTransfer.canStore(adjacent)) {
                vents.add(neighbor);
            }
        }
        int heatPerCell = ReactorRules.heatPerCell(cells, pulseNeighbors);
        int hullHeat = 0;
        for (int cell = 0; cell < cells; cell++) {
            hullHeat += transferHeat(vents, heatPerCell);
        }
        return hullHeat;
    }

    private int runEnergyPass(int width) {
        int output = 0;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            int cells = UraniumFuelRod.cellsIn(inventory.getStackInSlot(slot));
            if (slot % ReactorRules.COLUMNS < width && cells > 0) {
                output += runRodEnergy(slot, width, cells);
            }
        }
        return output;
    }

    private int runRodEnergy(int slot, int width, int cells) {
        int neighboringRods = 0;
        int reflectedPulses = 0;
        for (int neighbor : ReactorRules.neighbors(slot, width)) {
            if (neighbor < 0) {
                continue;
            }
            ItemStack adjacent = inventory.getStackInSlot(neighbor);
            if (UraniumFuelRod.cellsIn(adjacent) > 0) {
                neighboringRods++;
            } else if (isReflector(adjacent)) {
                reflectedPulses += reflect(neighbor, adjacent, cells);
            }
        }
        return ReactorRules.energy(cells, neighboringRods) + reflectedPulses * ReactorRules.EU_PER_PULSE;
    }

    private int reflect(int slot, ItemStack reflector, int pulses) {
        int durabilityLeft = reflector.getMaxDamage() - reflector.getDamageValue();
        if (pulses > durabilityLeft) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            return durabilityLeft + 1;
        }
        reflector.setDamageValue(reflector.getDamageValue() + pulses);
        return pulses;
    }

    private int transferHeat(List<Integer> vents, int heat) {
        for (int index = 0; index < vents.size() && heat > 0; index++) {
            int slot = vents.get(index);
            ItemStack stack = inventory.getStackInSlot(slot);
            int toStore = heat / (vents.size() - index);
            int overflow = heatTransfer.store(slot, stack, toStore);
            heat -= toStore - overflow;
            if (overflow > 0) {
                vents.remove(index);
                index--;
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

    private record CoolingPass(int hullHeat, int costTenths) {}
}
