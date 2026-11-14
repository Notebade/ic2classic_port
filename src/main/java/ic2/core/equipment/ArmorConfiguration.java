package ic2.core.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Dense module categories align with the stack's CONTAINER component. Derived parameters avoid recursive addon queries. */
record ArmorConfiguration(List<ResourceLocation> categories, int capacity, int rate, int packet, boolean provider,
                          boolean enhanced, List<ResourceLocation> features) {
    static final ArmorConfiguration EMPTY = new ArmorConfiguration(List.of(), 0, 0, 32, false, false, List.of());
    ArmorConfiguration { categories = List.copyOf(categories); features = List.copyOf(features); }
    private static final Codec<ArmorConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.listOf(0, 64).fieldOf("categories").forGetter(ArmorConfiguration::categories),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("capacity").forGetter(ArmorConfiguration::capacity),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("rate").forGetter(ArmorConfiguration::rate),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("packet").forGetter(ArmorConfiguration::packet),
        Codec.BOOL.fieldOf("provider").forGetter(ArmorConfiguration::provider),
        Codec.BOOL.fieldOf("enhanced").forGetter(ArmorConfiguration::enhanced),
        ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(ArmorConfiguration::features)
    ).apply(i, ArmorConfiguration::new));
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, "ic2");
    static final java.util.function.Supplier<DataComponentType<ArmorConfiguration>> TYPE = COMPONENTS.registerComponentType("armor_configuration", b -> b.persistent(CODEC));
    static final java.util.function.Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> FOOD =
        COMPONENTS.registerComponentType("armor_food", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
            .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));
    static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
