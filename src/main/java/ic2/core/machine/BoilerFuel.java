package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BoilerFuel {
    public static DeferredItem<Item> CAN, COAL_CELL, BIO_CELL, HYDRATED_CELL, RAW_BIO_CELL, HYDRATED_DUST, COMPRESSED_COAL, PLANT_BALL, COMPRESSED_PLANT, FLUID_EXPORT;
    private BoilerFuel() {}
    static void register(DeferredRegister.Items items) {
        CAN = items.registerItem("fuel_can", Can::new, new Item.Properties().stacksTo(1));
        COAL_CELL = items.registerItem("cell_coal_fuel", p -> new Item(p.craftRemainder(IC2.CELL_EMPTY.get())));
        BIO_CELL = items.registerItem("cell_bio_fuel", p -> new Item(p.craftRemainder(IC2.CELL_EMPTY.get())));
        HYDRATED_CELL = items.registerSimpleItem("cell_hydrated_coal");
        RAW_BIO_CELL = items.registerSimpleItem("cell_bio");
        HYDRATED_DUST = items.registerSimpleItem("dust_hydrated_coal");
        COMPRESSED_COAL = items.registerSimpleItem("hydrated_coal_compressed");
        PLANT_BALL = items.registerSimpleItem("plant_ball");
        COMPRESSED_PLANT = items.registerSimpleItem("plant_ball_compressed");
        FLUID_EXPORT = items.registerItem("fluid_export_upgrade_simple", p -> new TransportUpgradeItem(p, TransportUpgradeItem.Kind.FLUID_EXPORT));
    }
    public static int amount(ItemStack stack) {
        return stack.is(CAN.get()) ? Math.max(0, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("fuel")) : 0;
    }
    public static void set(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (amount > 0) tag.putInt("fuel", amount); else tag.remove("fuel");
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA); else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static int value(ItemStack cell) { return cell.is(COAL_CELL.get()) ? 2548 : cell.is(BIO_CELL.get()) ? 868 : 0; }
    private static final class Can extends Item {
        Can(Properties properties) { super(properties); }
        @Override public boolean isBarVisible(ItemStack stack) { return amount(stack) > 0; }
        @Override public int getBarWidth(ItemStack stack) { return Math.min(13, (int) (13L * amount(stack) / 10000)); }
        @Override public int getBarColor(ItemStack stack) { return 0xffaa00; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("gui.ic2.power_fuel", amount(stack)));
        }
    }
}
