package ic2.core.machine;

import ic2.api.item.ElectricEnchantable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

final class ElectricEnchantments {
    private ElectricEnchantments() {}
    record Removed(ItemStack remainder, ItemStack book) {}
    static boolean book(ItemStack stack) { return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK); }
    static boolean target(ItemStack stack) { return !stack.isEmpty() && (book(stack) || stack.getEnchantmentValue() > 0 || stack.getItem() instanceof ElectricEnchantable); }
    static ItemEnchantments enchantments(ItemStack stack) { return EnchantmentHelper.getEnchantmentsForCrafting(stack); }
    static int count(ItemStack stack) { return (int) Math.min(Integer.MAX_VALUE, (long) enchantments(stack).size() + Math.max(0, stack.getOrDefault(DataComponents.REPAIR_COST, 0))); }

    private static boolean accepts(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) {
        if (book(stack)) return true;
        if (stack.getItem() instanceof ElectricEnchantable custom) return custom.acceptsElectricEnchantment(stack, enchantment, anvil);
        return anvil ? enchantment.value().isSupportedItem(stack) : enchantment.value().isPrimaryItem(stack);
    }
    private static boolean compatible(Holder<Enchantment> enchantment, Collection<Holder<Enchantment>> existing) {
        for (var other : existing) if (!other.equals(enchantment) && !Enchantment.areCompatible(enchantment, other)) return false;
        return true;
    }
    private static List<EnchantmentInstance> fromBooks(List<ItemStack> books) {
        var result = new ArrayList<EnchantmentInstance>();
        for (var stack : books) for (var entry : enchantments(stack).entrySet()) result.add(new EnchantmentInstance(entry.getKey(), entry.getIntValue()));
        return result;
    }
    private static void merge(ItemEnchantments.Mutable result, List<EnchantmentInstance> additions, ItemStack stack, boolean anvil) {
        for (var entry : additions) {
            if (!accepts(stack, entry.enchantment, anvil)) continue;
            int previous = result.getLevel(entry.enchantment);
            if (previous == 0) {
                if (book(stack) || compatible(entry.enchantment, result.keySet())) result.set(entry.enchantment, entry.level);
            } else if (previous < entry.level) result.set(entry.enchantment, entry.level);
            else if (previous == entry.level && previous < entry.enchantment.value().getMaxLevel()) result.set(entry.enchantment, previous + 1);
        }
    }
    static ItemStack anvil(ItemStack input, List<ItemStack> books, int seed) {
        var stack = input.copyWithCount(1);
        var result = new ItemEnchantments.Mutable(enchantments(stack));
        merge(result, fromBooks(books), stack, true);
        if (book(stack)) stack = new ItemStack(Items.ENCHANTED_BOOK);
        else stack.set(DataComponents.REPAIR_COST, (int) Math.min(Integer.MAX_VALUE,
            (long) stack.getOrDefault(DataComponents.REPAIR_COST, 0) + RandomSource.create(seed).nextInt(result.keySet().size() + 1)));
        EnchantmentHelper.setEnchantments(stack, result.toImmutable());
        return stack;
    }
    static ItemStack enchant(ServerLevel level, ItemStack input, List<ItemStack> books, int enchantLevel, int seed) {
        var random = RandomSource.create((long) seed + enchantLevel);
        int enchantability = input.getItem() instanceof ElectricEnchantable custom ? Math.max(1, custom.electricEnchantability(input)) : input.is(Items.ENCHANTED_BOOK) ? 1 : input.getEnchantmentValue();
        enchantability = Math.clamp(enchantability, 1, 255);
        ItemStack costing = input.getEnchantmentValue() == 0 ? new ItemStack(Items.BOOK) : input;
        int power = EnchantmentHelper.getEnchantmentCost(random, books.size(), enchantLevel, costing);
        power += 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1);
        power = Math.max(1, Math.round(power * (1 + (random.nextFloat() + random.nextFloat() - 1) * .15F)));
        var candidates = candidates(level, input, power);
        random.setSeed(random.nextLong());
        for (int end = candidates.size(); end > 1; end--) java.util.Collections.swap(candidates, end - 1, random.nextInt(end));
        var supplied = fromBooks(books);
        var bookTypes = supplied.stream().map(entry -> entry.enchantment).toList();
        var selected = new ArrayList<EnchantmentInstance>();
        for (int index = 0; index < Math.min(6, candidates.size()); index++) {
            var entry = candidates.get(index);
            if (compatible(entry.enchantment, bookTypes) && compatible(entry.enchantment, selected.stream().map(value -> value.enchantment).toList())) selected.add(entry);
        }
        var result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var entry : selected.subList(0, Math.min(books.size() * 2, selected.size()))) result.set(entry.enchantment, entry.level);
        merge(result, supplied, input, false);
        var output = book(input) ? new ItemStack(Items.ENCHANTED_BOOK) : input.copyWithCount(1);
        EnchantmentHelper.setEnchantments(output, result.toImmutable());
        return output;
    }
    private static ArrayList<EnchantmentInstance> candidates(ServerLevel level, ItemStack stack, int power) {
        var result = new ArrayList<EnchantmentInstance>();
        level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().forEach(holder -> {
            if (!holder.is(EnchantmentTags.IN_ENCHANTING_TABLE) || holder.is(EnchantmentTags.CURSE) || holder.is(EnchantmentTags.TREASURE) || !accepts(stack, holder, false)) return;
            for (int rank = holder.value().getMaxLevel(); rank >= holder.value().getMinLevel(); rank--) {
                if (power >= holder.value().getMinCost(rank) && power <= holder.value().getMaxCost(rank)) { result.add(new EnchantmentInstance(holder, rank)); break; }
            }
        });
        return result;
    }
    static Removed removeFirst(ItemStack input) {
        var entries = enchantments(input);
        if (entries.isEmpty()) return new Removed(input.copy(), ItemStack.EMPTY);
        var first = entries.entrySet().iterator().next();
        var remaining = new ItemEnchantments.Mutable(entries);
        remaining.set(first.getKey(), 0);
        var output = new ItemStack(Items.ENCHANTED_BOOK);
        output.enchant(first.getKey(), first.getIntValue());
        var remainder = input.is(Items.ENCHANTED_BOOK) && remaining.keySet().isEmpty() ? new ItemStack(Items.BOOK) : input.copyWithCount(1);
        EnchantmentHelper.setEnchantments(remainder, remaining.toImmutable());
        return new Removed(remainder, output);
    }
}
