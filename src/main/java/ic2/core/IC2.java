package ic2.core;

import com.mojang.serialization.Codec;
import ic2.core.item.ReBatteryItem;
import ic2.core.item.ScrapBoxItem;
import ic2.core.machine.Machines;
import ic2.core.recipe.UuMatterRecipe;
import ic2.core.tree.Rubberwood;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(IC2.MOD_ID)
public final class IC2 {
    public static final String MOD_ID = "ic2";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UuMatterRecipe>> UU_MATTER_RECIPE =
        RECIPE_SERIALIZERS.register("uu_matter", () -> UuMatterRecipe.SERIALIZER);
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CHARGE =
        COMPONENTS.registerComponentType("battery_charge", builder -> builder
            .persistent(Codec.intRange(0, ReBatteryItem.CAPACITY)));
    public static final DeferredItem<ReBatteryItem> RE_BATTERY =
        ITEMS.registerItem("re_battery", ReBatteryItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("dust_copper");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");
    public static final DeferredItem<Item> STICKY_RESIN = ITEMS.registerSimpleItem("sticky_resin");
    public static final DeferredItem<Item> DENSE_COPPER_PLATE = ITEMS.registerSimpleItem("plate_dense_copper");
    public static final DeferredItem<Item> SCRAP = ITEMS.registerSimpleItem("scrap");
    public static final DeferredItem<Item> SCRAP_METAL = ITEMS.registerSimpleItem("scrap_metal");
    public static final DeferredItem<ScrapBoxItem> SCRAP_BOX = ITEMS.registerItem("scrapbox", ScrapBoxItem::new);
    public static final DeferredItem<Item> UU_MATTER = ITEMS.registerSimpleItem("uumatter");

    public IC2(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        Machines.register(modBus, ITEMS);
        Rubberwood.register(modBus, ITEMS);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ScrapBoxItem::registerDispenser));
    }
}
