package ic2.core.equipment;

import ic2.api.item.GeneratedArmor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredRegister;

final class StaticBoots extends ArmorItem implements GeneratedArmor, ic2.api.item.ArmorTickItem {
    private static final int THRESHOLD = 20, CAPACITY = 22;
    private StaticBoots(Holder<ArmorMaterial> material, Properties properties) { super(material, Type.BOOTS, properties); }
    static void register(DeferredRegister.Items items) {
        var material = EquipmentMaterials.simple("static_boots", Type.BOOTS, 1);
        items.registerItem("static_boots", p -> new StaticBoots(material, p), new Properties().stacksTo(1));
    }
    private static CompoundTag state(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("ic2_static"); }
    private static void save(ItemStack stack, CompoundTag state) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put("ic2_static", state); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    @Override public int generatedEnergy(ItemStack stack) { return Mth.clamp(state(stack).getInt("energy"), 0, CAPACITY); }
    @Override public int generationCapacity() { return CAPACITY; }
    @Override public void setGeneratedEnergy(ItemStack stack, int energy) {
        if (!stack.is(this)) throw new IllegalArgumentException("Wrong static generator item");
        var data = state(stack); data.putInt("energy", Mth.clamp(energy, 0, CAPACITY)); save(stack, data);
    }
    @Override public void tickArmor(ItemStack stack, Player player, EquipmentSlot slot) {
        if (slot != EquipmentSlot.FEET) return;
        var data = state(stack); int energy = generatedEnergy(stack);
        if (player.level().getGameTime() % 10 == 0 && energy > 0) energy -= ic2.api.item.ElectricEquipment.offerToArmor(player, energy);
        energy = walkEnergy(player, data, energy);
        data.putInt("energy", energy); save(stack, data);
    }
    static int walkEnergy(Player player, CompoundTag data, int energy) {
        int x = (int) player.getX(), z = (int) player.getZ();
        boolean walking = !player.isPassenger() && !player.isInWater() && player.onGround();
        if (!walking || !data.contains("x") || !data.contains("z")) { data.putInt("x", x); data.putInt("z", z); return energy; }
        if (energy >= THRESHOLD) return energy;
        double distance = Math.hypot((double) data.getInt("x") - x, (double) data.getInt("z") - z);
        if (distance < 5) return energy;
        data.putInt("x", x); data.putInt("z", z);
        int added = Math.min(3, Mth.floor(player.getInBlockState().is(BlockTags.WOOL_CARPETS) ? distance : distance / 5));
        return Math.min(CAPACITY, energy + added);
    }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2", "ingot_refined_iron")));
    }
}
