package ic2.core.equipment;

import ic2.api.item.*;
import ic2.core.IC2;
import ic2.core.item.EuCharge;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module custody and derived EU configuration change together; rejected operations preserve caller stacks. */
final class ModularSuitItem extends ArmorItem implements ModularArmor, ArmorTickItem, ProtectiveArmor, FallProtectiveArmor, ElectricEnchantable {
    private static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("ic2:armor_magic"));
    private static final net.minecraft.resources.ResourceKey<DamageType> ELECTRICITY = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("ic2:electricity"));
    private final Type piece;
    ModularSuitItem(Holder<ArmorMaterial> material, Type piece, Properties properties) { super(material, piece, properties); this.piece = piece; }
    static void register(DeferredRegister.Items items) {
        for (var type : List.of(Type.HELMET, Type.CHESTPLATE, Type.LEGGINGS, Type.BOOTS)) {
            items.registerItem("nano_suit_" + type.getName(), p -> new ModularSuitItem(ArmorMaterials.DIAMOND, type, p), new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }
    }
    @Override public EquipmentSlot equipmentSlot() { return piece.getSlot(); }
    @Override public int capacity() { return 0; }
    @Override public int transferRate() { return 0; }
    @Override public int minimumPacket() { return 32; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public int capacity(ItemStack stack) { return ArmorModules.configuration(stack).capacity(); }
    @Override public int transferRate(ItemStack stack) { return ArmorModules.configuration(stack).rate(); }
    @Override public int minimumPacket(ItemStack stack) { return ArmorModules.configuration(stack).packet(); }
    @Override public boolean canProvideEnergy(ItemStack stack) { return ArmorModules.configuration(stack).provider(); }
    @Override public int charge(ItemStack stack) { return EuCharge.get(stack, capacity(stack)); }
    @Override public void setCharge(ItemStack stack, int amount) { EuCharge.set(stack, capacity(stack), amount); }
    @Override public List<InstalledArmorModule> modules(ItemStack stack) { return ArmorModules.read(stack); }
    @Override public Set<ResourceLocation> features(ItemStack stack) { return Set.copyOf(ArmorModules.configuration(stack).features()); }
    @Override public boolean enhanced(ItemStack stack) { return ArmorModules.configuration(stack).enhanced(); }
    @Override public Map<ResourceLocation, Integer> moduleLimits(ItemStack stack) { return limits(enhanced(stack)); }
    private Map<ResourceLocation, Integer> limits(boolean enhanced) {
        var slots = new LinkedHashMap<ResourceLocation, Integer>();
        slots.put(ArmorModule.BATTERY, 1); slots.put(ArmorModule.CHARGER, 1);
        switch (piece) {
            case HELMET -> { slots.put(ArmorModule.HUD, 9); slots.put(ArmorModule.GENERIC, enhanced ? 3 : 2); }
            case CHESTPLATE -> { slots.put(ArmorModule.STORAGE, enhanced ? 2 : 1); slots.put(ArmorModule.BACK, 1); }
            case LEGGINGS -> { slots.put(ArmorModule.STORAGE, enhanced ? 2 : 1); slots.put(ArmorModule.MOVEMENT, 1); }
            case BOOTS -> slots.put(ArmorModule.MOVEMENT, 2);
            default -> { }
        }
        if (enhanced && piece != Type.HELMET) slots.put(ArmorModule.GENERIC, 1);
        return Map.copyOf(slots);
    }
    @Override public boolean canInstall(ItemStack armor, ResourceLocation category, ItemStack module) {
        if (!armor.is(this) || armor.getCount() != 1 || module.isEmpty()) return false;
        var declared = ArmorModules.type(module);
        if (declared == null || !declared.equals(category) && !declared.equals(ArmorModule.ANY)) return false;
        long count = modules(armor).stream().filter(m -> m.category().equals(category)).count();
        if (count >= moduleLimits(armor).getOrDefault(category, 0)) return false;
        return !(module.getItem() instanceof ArmorModule item) || item.supportsArmor(module, armor, equipmentSlot());
    }
    @Override public boolean install(ItemStack armor, ResourceLocation category, ItemStack module) {
        if (!canInstall(armor, category, module)) return false;
        var installed = module.copyWithCount(1); int moved = 0;
        if (ArmorModules.shares(installed, armor) && installed.getItem() instanceof ElectricItem battery) {
            moved = battery.charge(installed); battery.setCharge(installed, 0);
        }
        var entries = new ArrayList<>(modules(armor)); entries.add(new InstalledArmorModule(category, installed));
        var configuration = ArmorModules.derive(armor, entries, enhanced(armor));
        long charge = (long) charge(armor) + moved;
        if (charge > configuration.capacity()) return false;
        ArmorModules.write(armor, entries, configuration); setCharge(armor, (int) charge); module.shrink(1);
        if (installed.getItem() instanceof ArmorModule item) {
            item.installed(installed, armor);
            entries.set(entries.size() - 1, new InstalledArmorModule(category, installed));
            ArmorModules.write(armor, entries, configuration);
        }
        return true;
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && FoodStorageModule.enabled(stack)) {
            return FoodStorageModule.open(level, player, hand, stack);
        }
        return super.use(level, player, hand);
    }

    @Override public ItemStack removeModule(ItemStack armor, int index, boolean simulate) {
        if (!armor.is(this) || armor.getCount() != 1) return ItemStack.EMPTY;
        var entries = new ArrayList<>(modules(armor));
        if (index < 0 || index >= entries.size()) return ItemStack.EMPTY;
        var removed = entries.remove(index).stack(); int stored = charge(armor), moved = 0;
        if (removed.getItem() instanceof FoodStorageModule && FoodStorageModule.hasContents(armor)) return ItemStack.EMPTY;
        if (ArmorModules.shares(removed, armor) && removed.getItem() instanceof ElectricItem battery) {
            moved = Math.min(stored, battery.capacity(removed) - battery.charge(removed));
            battery.setCharge(removed, battery.charge(removed) + moved);
        }
        var configuration = ArmorModules.derive(armor, entries, enhanced(armor));
        if (stored - moved > configuration.capacity()) return ItemStack.EMPTY;
        if (!simulate) {
            ArmorModules.write(armor, entries, configuration); setCharge(armor, stored - moved);
            if (removed.getItem() instanceof ArmorModule item) item.removed(removed, armor);
        }
        return removed;
    }
    @Override public boolean setEnhanced(ItemStack armor, boolean enhanced) {
        if (!armor.is(this) || armor.getCount() != 1) return false;
        var entries = modules(armor); var limits = limits(enhanced);
        for (var category : entries.stream().map(InstalledArmorModule::category).distinct().toList()) {
            if (entries.stream().filter(m -> m.category().equals(category)).count() > limits.getOrDefault(category, 0)) return false;
        }
        ArmorModules.write(armor, entries, ArmorModules.derive(armor, entries, enhanced)); return true;
    }
    @Override public ItemStack defaultArmor() {
        var result = new ItemStack(this); install(result, ArmorModule.BATTERY, new ItemStack(IC2.ENERGY_CRYSTAL.get())); return result;
    }
    @Override public void tickArmor(ItemStack armor, Player wearer, EquipmentSlot slot) {
        if (slot != equipmentSlot()) return;
        var entries = new ArrayList<>(modules(armor)); boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i); var stack = entry.stack();
            if (stack.getItem() instanceof ArmorModule item) {
                item.tickModule(stack, armor, wearer);
                if (!ItemStack.matches(stack, entry.stack())) { entries.set(i, new InstalledArmorModule(entry.category(), stack)); changed = true; }
            }
        }
        if (changed) ArmorModules.write(armor, entries, ArmorModules.derive(armor, entries, enhanced(armor)));
    }
    private boolean protects(DamageSource source, LivingEntity wearer) {
        if (source.is(DamageTypes.SONIC_BOOM)) {
            for (var slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !(wearer.getItemBySlot(slot).getItem() instanceof ModularArmor)) return false;
            return true;
        }
        return !source.is(MAGIC) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);
    }
    @Override public Absorption absorption(ItemStack stack, LivingEntity wearer, DamageSource source, double damage, EquipmentSlot slot) {
        if (!protects(source, wearer)) return Absorption.NONE;
        if (source.is(ELECTRICITY) && EquipmentConfig.SPECIAL_ELECTRIC.get()) return new Absorption(0, 1, Integer.MAX_VALUE);
        double share = switch (slot) { case HEAD -> .15; case CHEST -> .4; case LEGS, FEET -> .3; default -> 0; };
        return new Absorption(0, .9 * share * EquipmentConfig.ARMOR_ABSORPTION.get(), (int) (charge(stack) / (800 * EquipmentConfig.ARMOR_ENERGY_COST.get())));
    }
    @Override public void absorbed(ItemStack stack, LivingEntity wearer, DamageSource source, int damage, EquipmentSlot slot, Wear kind) {
        if (source.is(ELECTRICITY) && EquipmentConfig.SPECIAL_ELECTRIC.get()) {
            if (capacity(stack) > charge(stack)) { setCharge(stack, (int) Math.min(capacity(stack), (long) charge(stack) + damage)); return; }
            if (wearer.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ElectricItem && !wearer.level().isEmptyBlock(wearer.blockPosition().below())) return;
        }
        int cost = (int) Math.min(Integer.MAX_VALUE, damage * 800.0 * EquipmentConfig.ARMOR_ENERGY_COST.get());
        setCharge(stack, charge(stack) - ElectricEquipment.energyCost(stack, cost));
    }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
    @Override public int absorbFall(ItemStack stack, Player wearer, int damage, boolean simulate) {
        if (!stack.is(this) || piece != Type.BOOTS || stack.getCount() != 1 || damage <= 0) return 0;
        int absorbed = Math.min(damage, Math.min(enhanced(stack) ? Integer.MAX_VALUE : 11, charge(stack) / 800));
        if (!simulate && absorbed > 0) setCharge(stack, charge(stack) - absorbed * 800);
        return absorbed;
    }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) { return features(stack).contains(ArmorModule.id("golden")); }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) { return !enchantment.is(Enchantments.MENDING); }
    @Override public int electricEnchantability(ItemStack stack) { return 10; }
    @Override public boolean isBarVisible(ItemStack stack) { return capacity(stack) > 0; }
    @Override public int getBarWidth(ItemStack stack) { return capacity(stack) == 0 ? 0 : Math.round(13F * charge(stack) / capacity(stack)); }
    @Override public int getBarColor(ItemStack stack) { return 0x42CDEA; }
}
