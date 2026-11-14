package ic2.core.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class QuantumArmorColors {
    private static Item[] items() {
        return java.util.stream.Stream.of("helmet", "chestplate", "leggings", "boots")
            .map(piece -> BuiltInRegistries.ITEM.get(ResourceLocation.parse("ic2:quantum_suit_" + piece))).toArray(Item[]::new);
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> DyedItemColor.getOrDefault(stack, 0xFFFFFFFF), items());
    }
    @SubscribeEvent public static void armor(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public int getDefaultDyeColor(ItemStack stack) { return DyedItemColor.getOrDefault(stack, 0xFFFFFFFF); }
        }, items());
    }
}
