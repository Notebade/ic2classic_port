package ic2.core.machine;

import ic2.core.item.ReactorHeatItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorHeatTransfer {
    private final ItemStackHandler inventory;

    ReactorHeatTransfer(ItemStackHandler inventory) {
        this.inventory = inventory;
    }

    static boolean canStore(ItemStack stack) {
        return stack.getItem() instanceof ReactorHeatItem
            && (!isCondensator(stack) || stack.getDamageValue() < stack.getMaxDamage() - 1);
    }

    private static boolean isCondensator(ItemStack stack) {
        return stack.getItem() instanceof ReactorHeatItem.Condensator;
    }

    int store(int slot, ItemStack stack, int heat) {
        boolean condensator = isCondensator(stack);
        int limit = stack.getMaxDamage() - (condensator ? 1 : 0);
        int total = stack.getDamageValue() + heat;
        int stored = Math.clamp(total, 0, limit);
        if (total > limit && !condensator) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            stack.setDamageValue(stored);
        }
        return total - stored;
    }

    void coolNeighbors(int slot, int width) {
        for (int neighbor : ReactorRules.neighbors(slot, width)) {
            if (neighbor >= 0) {
                ItemStack stack = inventory.getStackInSlot(neighbor);
                if (canStore(stack)) {
                    store(neighbor, stack, -4);
                }
            }
        }
    }

    int exchange(int slot, int width, ItemStack stack, ReactorHeatItem.Exchanger exchanger, int hullHeat) {
        List<Neighbor> neighbors = exchanger.componentTransfer() == 0 ? List.of() : heatNeighbors(slot, width);
        if (!exchanger.selective()) {
            return apply(slot, stack, exchanger, neighbors, hullHeat, Band.ALL);
        }
        return apply(slot, stack, exchanger, neighbors, hullHeat, Band.COLD, Band.HOT);
    }

    private List<Neighbor> heatNeighbors(int slot, int width) {
        var neighbors = new ArrayList<Neighbor>(4);
        for (int neighbor : ReactorRules.neighbors(slot, width)) {
            if (neighbor >= 0) {
                ItemStack stack = inventory.getStackInSlot(neighbor);
                if (canStore(stack)) {
                    neighbors.add(new Neighbor(neighbor, stack, (double) storedHeat(stack) / stack.getMaxDamage()));
                }
            }
        }
        return neighbors;
    }

    private int apply(int slot, ItemStack stack, ReactorHeatItem.Exchanger exchanger, List<Neighbor> neighbors,
                      int hullHeat, Band... bands) {
        int stored = stack.getDamageValue();
        int delta = 0;
        for (Band band : bands) {
            Transfer transfer = balance(exchanger, neighbors, (double) stored / stack.getMaxDamage(), hullHeat, band);
            hullHeat += transfer.hull();
            delta += transfer.component();
        }
        store(slot, stack, delta);
        return hullHeat;
    }

    private Transfer balance(ReactorHeatItem.Exchanger exchanger, List<Neighbor> neighbors, double ratio,
                             int hullHeat, Band band) {
        var targets = neighbors.stream().filter(neighbor -> band.includes(neighbor.ratio())).toList();
        double hullRatio = (double) hullHeat / ReactorRules.MAX_HEAT;
        boolean includeHull = exchanger.hullTransfer() > 0 && band.includes(hullRatio);
        double sum = ratio + targets.stream().mapToDouble(Neighbor::ratio).sum();
        double average = (sum + (includeHull ? hullRatio : 0)) / (targets.size() + (includeHull ? 2 : 1));
        int hullDelta = includeHull ? toward(average, ReactorRules.MAX_HEAT, hullHeat, exchanger.hullTransfer()) : 0;
        int componentDelta = -hullDelta;
        for (Neighbor target : targets) {
            ItemStack stack = target.stack();
            int amount = toward(average, stack.getMaxDamage(), storedHeat(stack), exchanger.componentTransfer());
            componentDelta += store(target.slot(), stack, amount) - amount;
        }
        return new Transfer(hullDelta, componentDelta);
    }

    private static int toward(double ratio, int capacity, int stored, int rate) {
        return Math.clamp((int) (ratio * capacity - stored), -rate, rate);
    }

    private static int storedHeat(ItemStack stack) {
        return isCondensator(stack) ? 0 : stack.getDamageValue();
    }

    private enum Band {
        ALL, COLD, HOT;

        boolean includes(double ratio) {
            return switch (this) {
                case ALL -> true;
                case COLD -> ratio <= 0.25;
                case HOT -> ratio >= 0.75;
            };
        }
    }

    private record Neighbor(int slot, ItemStack stack, double ratio) {}

    private record Transfer(int hull, int component) {}
}
