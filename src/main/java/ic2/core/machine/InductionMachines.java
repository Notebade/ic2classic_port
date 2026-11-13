package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class InductionMachines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<InductionBlock> FURNACE = block("induction_furnace", Variant.FURNACE);
    static final DeferredBlock<InductionBlock> BLAST = block("induction_blast_furnace", Variant.BLAST);
    static final DeferredBlock<InductionBlock> SMOKER = block("induction_smoker", Variant.SMOKER);
    static final Supplier<BlockEntityType<InductionEntity>> ENTITY = ENTITIES.register("induction_furnace",
        () -> BlockEntityType.Builder.of(InductionEntity::new, FURNACE.get(), BLAST.get(), SMOKER.get()).build(null));
    static final Supplier<MenuType<InductionMenu>> MENU = MENUS.register("induction_furnace",
        () -> new MenuType<>(InductionMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private InductionMachines() {}

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        items.registerSimpleBlockItem(FURNACE);
        items.registerSimpleBlockItem(BLAST);
        items.registerSimpleBlockItem(SMOKER);
        bus.addListener(InductionMachines::capabilities);
    }

    private static DeferredBlock<InductionBlock> block(String id, Variant variant) {
        return BLOCKS.registerBlock(id, properties -> new InductionBlock(properties, variant),
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), InductionEntity::automation);
    }

    enum Variant {
        FURNACE(RecipeType.SMELTING, 15), BLAST(RecipeType.BLASTING, 29), SMOKER(RecipeType.SMOKING, 29);

        final RecipeType<? extends AbstractCookingRecipe> recipes;
        final int cost;

        Variant(RecipeType<? extends AbstractCookingRecipe> recipes, int cost) {
            this.recipes = recipes;
            this.cost = cost;
        }
    }
}
