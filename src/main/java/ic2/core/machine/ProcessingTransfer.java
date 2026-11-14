package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-validated recipe loading shared by all processing inventories. */
final class ProcessingTransfer {
    private static final int BUTTON_BASE = 1000;
    private ProcessingTransfer() {}
    record Plan(List<Integer> targets, List<ItemStack> inputs, List<ItemStack> player) {}

    static int button(int index, boolean maximum) { return BUTTON_BASE + index * 2 + (maximum ? 1 : 0); }

    static boolean handle(AbstractContainerMenu menu, ProcessingMenu processing, Player player, int button) {
        if (button < BUTTON_BASE || player.level().isClientSide || !menu.stillValid(player)) return false;
        int index = (button - BUTTON_BASE) / 2;
        Plan plan = plan(menu, processing, player.level(), index, (button - BUTTON_BASE) % 2 == 1);
        if (plan == null) return false;
        for (int i = 0; i < plan.player.size(); i++) menu.getSlot(processing.machineSlotCount() + i).set(plan.player.get(i));
        for (int i = 0; i < plan.targets.size(); i++) menu.getSlot(plan.targets.get(i)).set(plan.inputs.get(i));
        menu.broadcastChanges();
        return true;
    }

    static Plan plan(AbstractContainerMenu menu, ProcessingMenu processing, Level level, int index, boolean maximum) {
        var recipes = processing.recipeViews(level);
        if (index < 0 || index >= recipes.size()) return null;
        var recipe = recipes.get(index);
        var targets = maximum ? processing.bulkInputs(recipe) : processing.recipeInputs(recipe);
        if (recipe.inputs().isEmpty()
                || recipe.inputs().stream().anyMatch(slot -> !slot.fluid().isEmpty() || slot.items().isEmpty())) return null;
        if (maximum && recipe.inputs().size() == 1 && targets.size() > 1)
            return manyInputs(menu, processing, recipe, targets);
        if (targets.size() != recipe.inputs().size()) return null;
        int limit = maximum ? 64 : 1;
        for (int batches = limit; batches >= 1; batches--) {
            Plan plan = batch(menu, processing, recipe, targets, batches);
            if (plan != null && processing.acceptsTransfer(recipe, plan.inputs(), level)) return plan;
        }
        return null;
    }

    private static Plan batch(AbstractContainerMenu menu, ProcessingMenu processing, MachineRecipeView recipe,
                              List<Integer> targets, int batches) {
        var pool = new ArrayList<ItemStack>();
        targets.forEach(index -> pool.add(menu.getSlot(index).getItem().copy()));
        menu.slots.subList(processing.machineSlotCount(), menu.slots.size()).forEach(slot -> pool.add(slot.getItem().copy()));
        var loaded = new ArrayList<ItemStack>();
        for (int i = 0; i < targets.size(); i++) {
            ItemStack stack = take(pool, recipe.inputs().get(i), batches, menu.getSlot(targets.get(i)).getMaxStackSize());
            if (stack.isEmpty()) return null;
            loaded.add(stack);
        }
        var player = new ArrayList<>(pool.subList(targets.size(), pool.size()));
        for (int i = 0; i < targets.size(); i++) if (!putBack(player, pool.get(i))) return null;
        return new Plan(targets, loaded, player);
    }

    private static Plan manyInputs(AbstractContainerMenu menu, ProcessingMenu processing, MachineRecipeView recipe, List<Integer> targets) {
        var pool = new ArrayList<ItemStack>();
        targets.forEach(index -> pool.add(menu.getSlot(index).getItem().copy()));
        menu.slots.subList(processing.machineSlotCount(), menu.slots.size()).forEach(slot -> pool.add(slot.getItem().copy()));
        var loaded = new ArrayList<ItemStack>();
        for (int index : targets) {
            ItemStack next = ItemStack.EMPTY;
            for (int batches = 64; batches >= 1 && next.isEmpty(); batches--)
                next = take(pool, recipe.inputs().getFirst(), batches, menu.getSlot(index).getMaxStackSize());
            loaded.add(next);
        }
        if (loaded.stream().allMatch(ItemStack::isEmpty)) return null;
        for (int i = 0; i < targets.size(); i++) if (!loaded.get(i).isEmpty() && !menu.getSlot(targets.get(i)).mayPlace(loaded.get(i))) return null;
        var player = new ArrayList<>(pool.subList(targets.size(), pool.size()));
        for (int i = 0; i < targets.size(); i++) if (!putBack(player, pool.get(i))) return null;
        return new Plan(targets, loaded, player);
    }

    private static ItemStack take(List<ItemStack> pool, MachineRecipeView.Slot ingredient, int batches, int slotLimit) {
        for (ItemStack candidate : pool) {
            var matching = ingredient.items().stream().filter(item -> item.is(candidate.getItem()) && (!item.isDamaged() || candidate.isDamaged())).findFirst();
            if (candidate.isEmpty() || matching.isEmpty()) continue;
            int count = matching.get().getCount() * batches;
            if (count > Math.min(slotLimit, candidate.getMaxStackSize())) continue;
            int available = pool.stream().filter(item -> ItemStack.isSameItemSameComponents(item, candidate)).mapToInt(ItemStack::getCount).sum();
            if (available < count) continue;
            ItemStack result = candidate.copyWithCount(count);
            int needed = count;
            for (ItemStack item : pool) {
                if (!ItemStack.isSameItemSameComponents(item, result)) continue;
                int removed = Math.min(needed, item.getCount());
                item.shrink(removed);
                needed -= removed;
                if (needed == 0) break;
            }
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean putBack(List<ItemStack> inventory, ItemStack stack) {
        for (ItemStack target : inventory) {
            if (!ItemStack.isSameItemSameComponents(stack, target)) continue;
            int count = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
            target.grow(count); stack.shrink(count);
        }
        for (int i = 0; i < inventory.size() && !stack.isEmpty(); i++) {
            if (!inventory.get(i).isEmpty()) continue;
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            inventory.set(i, stack.copyWithCount(count)); stack.shrink(count);
        }
        return stack.isEmpty();
    }
}
