package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.ReactorHeatItem;
import ic2.core.item.ReactorPlatingItem;
import ic2.core.item.ReactorReflectorItem;
import ic2.core.item.UraniumFuelRod;
import ic2.core.item.ReactorPulseCell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorGrid {
    private final ItemStackHandler inventory;
    private final ReactorHeatTransfer heatTransfer;
    private final SteamReactorTanks steam;

    ReactorGrid(ItemStackHandler inventory, SteamReactorTanks steam) {
        this.inventory = inventory;
        this.steam = steam;
        heatTransfer = new ReactorHeatTransfer(inventory);
    }

    static boolean accepts(ItemStack stack, boolean steam) {
        if (stack.getItem() instanceof ReactorHeatItem.SteamVent) {
            return steam;
        }
        if (stack.getItem() instanceof ReactorHeatItem.Vent vent && vent.electric()) {
            return !steam;
        }
        return UraniumFuelRod.cellsIn(stack) > 0 || stack.getItem() instanceof ReactorHeatItem
            || stack.getItem() instanceof ReactorHeatItem.Spreader || stack.getItem() instanceof ReactorReflectorItem
            || stack.getItem() instanceof ReactorPlatingItem || stack.getItem() instanceof ReactorPulseCell
            || stack.getItem() instanceof HeatPumpItem
            || !steam && stack.is(IC2.HEAT_PACK.get());
    }

    private static boolean reflects(ItemStack stack) {
        return stack.getItem() instanceof ReactorReflectorItem reflector && reflector.reflects(stack);
    }

    Hull hull(int width) {
        int capacity = ReactorRules.MAX_HEAT;
        float containment = 1;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            if (slot % ReactorRules.COLUMNS < width && inventory.getStackInSlot(slot).getItem() instanceof ReactorPlatingItem plating) {
                capacity += plating.additionalHeat();
                containment *= plating.containment();
            }
        }
        return new Hull(capacity, containment);
    }

    float blastPower(float containment) {
        float power = 10;
        float multiplier = containment * containment;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof UraniumFuelRod rod) {
                power += rod.type().explosionPower() * rod.cells();
            } else if (stack.getItem() instanceof ReactorReflectorItem) {
                power--;
            }
            if (stack.is(IC2.HEAT_PACK.get())) {
                float influence = stack.getCount() / 10.0F;
                if (influence < 1) {
                    multiplier *= influence;
                } else {
                    power += influence;
                }
            }
        }
        return Math.clamp(power * multiplier, 0, GenerationConfig.REACTOR_DAMAGE.get().floatValue());
    }

    Cycle run(int width, int hullHeat, int hullCapacity, boolean powered, boolean damageTick, boolean passiveCooling, RandomSource random) {
        CoolingPass cooling = runHeatPass(width, hullHeat, hullCapacity, powered, passiveCooling);
        // Reflectors still affect heat in the cycle when an energy pulse breaks them.
        Cycle cycle = runEnergyPass(width, cooling.hullHeat(), hullCapacity, powered && damageTick);
        if (powered && damageTick) {
            ageFuel(width, random);
        }
        int netOutput = steam == null ? (int) Math.clamp(((long) cycle.output() * 10 - cooling.costTenths()) * GenerationConfig.NUCLEAR.get() / 200, 0, Integer.MAX_VALUE) : cooling.steamOutput();
        return new Cycle(netOutput, cycle.heat());
    }

    private CoolingPass runHeatPass(int width, int heat, int hullCapacity, boolean powered, boolean passiveCooling) {
        int costTenths = 0;
        int steamOutput = 0;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            if (slot % ReactorRules.COLUMNS >= width) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (steam != null && stack.getItem() instanceof ReactorHeatItem.SteamVent vent) {
                int drained = Math.min(heat, vent.hullCooling());
                if (heatTransfer.store(slot, stack, drained) == 0) {
                    heat -= drained;
                    steamOutput += SteamVentCycle.boil(steam, stack, vent, passiveCooling);
                }
                continue;
            }
            if (stack.getItem() instanceof ReactorHeatItem.Vent vent) {
                int drained = Math.min(heat, vent.hullCooling(powered));
                if (heatTransfer.store(slot, stack, drained) == 0) {
                    heat -= drained;
                    costTenths += vent.powerCostTenths();
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - vent.selfCooling(powered)));
                }
                continue;
            }
            heat = processThermalComponent(slot, width, stack, heat, hullCapacity);
            if (stack.getItem() instanceof ReactorHeatItem.Spreader) {
                heatTransfer.coolNeighbors(slot, width);
            }
            if (powered && stack.getItem() instanceof UraniumFuelRod rod) {
                for (int cell = 0; cell < rod.cells(); cell++) {
                    heat += runFuelCellHeat(slot, width, rod, heat);
                }
            }
        }
        return new CoolingPass(heat, costTenths, steamOutput);
    }

    private int processThermalComponent(int slot, int width, ItemStack stack, int heat, int hullCapacity) {
        if (stack.getItem() instanceof ReactorHeatItem.Exchanger exchanger) {
            return heatTransfer.exchange(slot, width, stack, exchanger, heat, hullCapacity);
        }
        if (stack.getItem() instanceof HeatPumpItem) {
            heatTransfer.pump(slot, width, HeatPumpItem.directions(stack));
        }
        if (stack.is(IC2.HEAT_PACK.get()) && heat < 1_000 * stack.getCount()) {
            return heat + stack.getCount();
        }
        return heat;
    }

    private int runFuelCellHeat(int slot, int width, UraniumFuelRod rod, int hullHeat) {
        int acceptedPulses = 0;
        int pulses = rod.type().pulses();
        for (int neighbor : ReactorRules.pulseTargets(slot, width, rod.type())) {
            if (neighbor < 0) {
                continue;
            }
            ItemStack adjacent = inventory.getStackInSlot(neighbor);
            if (UraniumFuelRod.cellsIn(adjacent) > 0 || reflects(adjacent)) {
                acceptedPulses += pulses;
            } else if (adjacent.getItem() instanceof ReactorPulseCell isotope) {
                acceptedPulses += steam == null ? breed(neighbor, adjacent, isotope, pulses, hullHeat) : pulses;
            }
        }
        var sinks = new ArrayList<Integer>(4);
        for (int neighbor : ReactorRules.heatTargets(slot, width, rod.type())) {
            if (neighbor >= 0 && ReactorHeatTransfer.canStore(inventory.getStackInSlot(neighbor))) {
                sinks.add(neighbor);
            }
        }
        int pulseCount = rod.selfPulsesPerCell() + acceptedPulses * rod.type().connectionPulses();
        return transferHeat(sinks, rod.type().heatPerCell(pulseCount));
    }

    private Cycle runEnergyPass(int width, int heat, int hullCapacity, boolean powered) {
        int output = 0;
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            if (slot % ReactorRules.COLUMNS >= width) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (steam != null && stack.getItem() instanceof ReactorHeatItem.SteamVent vent) {
                SteamVentCycle.refill(steam, stack, vent);
            }
            heat = processThermalComponent(slot, width, stack, heat, hullCapacity);
            if (powered && stack.getItem() instanceof UraniumFuelRod rod) {
                output += runRodEnergy(slot, width, rod, heat);
            }
        }
        return new Cycle(output, heat);
    }

    private int runRodEnergy(int slot, int width, UraniumFuelRod rod, int hullHeat) {
        int output = rod.cells() * rod.selfPulsesPerCell() * rod.type().energyPerPulse();
        int pulses = rod.cells() * rod.type().pulses();
        for (int neighbor : ReactorRules.pulseTargets(slot, width, rod.type())) {
            if (neighbor < 0) {
                continue;
            }
            ItemStack adjacent = inventory.getStackInSlot(neighbor);
            if (adjacent.getItem() instanceof UraniumFuelRod target) {
                output += pulses * target.type().energyPerPulse();
            } else if (adjacent.getItem() instanceof ReactorReflectorItem reflector && reflector.reflects(adjacent)) {
                output += reflect(neighbor, adjacent, reflector, pulses) * ReactorRules.EU_PER_PULSE;
            } else if (adjacent.getItem() instanceof ReactorPulseCell isotope) {
                breed(neighbor, adjacent, isotope, pulses, hullHeat);
            }
        }
        return output;
    }

    private int breed(int slot, ItemStack stack, ReactorPulseCell isotope, int pulses, int hullHeat) {
        int progressPerPulse = isotope.progressPerPulse(hullHeat);
        int remaining = stack.getDamageValue() - pulses * progressPerPulse;
        if (remaining <= 0) {
            int accepted = Math.max(1, Math.ceilDiv(stack.getDamageValue(), progressPerPulse));
            inventory.setStackInSlot(slot, isotope.activated());
            return accepted;
        }
        stack.setDamageValue(remaining);
        return pulses;
    }

    private int reflect(int slot, ItemStack reflector, ReactorReflectorItem item, int pulses) {
        int durabilityLeft = reflector.getMaxDamage() - reflector.getDamageValue();
        if (item.repairable()) {
            pulses = Math.min(pulses, durabilityLeft);
        }
        if (pulses > durabilityLeft) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            return durabilityLeft + 1;
        }
        reflector.setDamageValue(reflector.getDamageValue() + pulses);
        return pulses;
    }

    private int transferHeat(List<Integer> sinks, int heat) {
        for (int index = 0; index < sinks.size() && heat > 0; index++) {
            int slot = sinks.get(index);
            ItemStack stack = inventory.getStackInSlot(slot);
            int toStore = heat / (sinks.size() - index);
            heat -= toStore - heatTransfer.store(slot, stack, toStore);
        }
        return heat;
    }

    private void ageFuel(int width, RandomSource random) {
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (slot % ReactorRules.COLUMNS < width && stack.getItem() instanceof UraniumFuelRod rod) {
                int damage = stack.getDamageValue() + 1;
                if (damage > stack.getMaxDamage()) {
                    inventory.setStackInSlot(slot, random.nextInt(3) == 0
                        ? rod.depletedRods() : ItemStack.EMPTY);
                } else {
                    stack.setDamageValue(damage);
                }
            }
        }
    }

    record Cycle(int output, int heat) {}

    record Hull(int capacity, float containment) {}

    private record CoolingPass(int hullHeat, int costTenths, int steamOutput) {}
}
