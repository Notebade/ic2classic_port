package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Plans a complete transfer on copies before the server changes any inventory. */
final class AlloyRecipeTransfer {
    private AlloyRecipeTransfer() {}

    record Plan(ItemStack first, ItemStack second, List<ItemStack> player) {}

    static Plan plan(AlloyRecipes.Recipe recipe, ItemStack first, ItemStack second, List<ItemStack> player) {
        if (recipe.matches(first, second) || recipe.matches(second, first))
            return new Plan(first.copy(), second.copy(), copies(player));
        var pool = new ArrayList<ItemStack>();
        pool.add(first.copy());
        pool.add(second.copy());
        pool.addAll(copies(player));
        ItemStack a = take(pool, recipe.first(), recipe.firstCount());
        ItemStack b = take(pool, recipe.second(), recipe.secondCount());
        if (a.isEmpty() || b.isEmpty()) return null;
        var remaining = new ArrayList<>(pool.subList(2, pool.size()));
        if (!returnStack(remaining, pool.get(0)) || !returnStack(remaining, pool.get(1))) return null;
        return new Plan(a, b, remaining);
    }

    private static ArrayList<ItemStack> copies(List<ItemStack> source) {
        var result = new ArrayList<ItemStack>();
        source.forEach(stack -> result.add(stack.copy()));
        return result;
    }

    private static ItemStack take(List<ItemStack> pool, Ingredient ingredient, int count) {
        for (ItemStack candidate : pool) {
            if (!ingredient.test(candidate)) continue;
            int available = pool.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, candidate))
                .mapToInt(ItemStack::getCount).sum();
            if (available < count) continue;
            ItemStack result = candidate.copyWithCount(count);
            int needed = count;
            for (ItemStack stack : pool) {
                if (!ItemStack.isSameItemSameComponents(stack, result)) continue;
                int removed = Math.min(needed, stack.getCount());
                stack.shrink(removed);
                needed -= removed;
                if (needed == 0) break;
            }
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean returnStack(List<ItemStack> player, ItemStack stack) {
        if (stack.isEmpty()) return true;
        for (ItemStack target : player) {
            if (!ItemStack.isSameItemSameComponents(stack, target)) continue;
            int moved = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
            target.grow(moved);
            stack.shrink(moved);
        }
        for (int i = 0; i < player.size() && !stack.isEmpty(); i++) {
            if (!player.get(i).isEmpty()) continue;
            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            player.set(i, stack.copyWithCount(moved));
            stack.shrink(moved);
        }
        return stack.isEmpty();
    }
}
