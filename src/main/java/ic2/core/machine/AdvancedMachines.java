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

public final class AdvancedMachines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<AdvancedMachineBlock> FURNACE = block("induction_furnace", Variant.FURNACE);
    static final DeferredBlock<AdvancedMachineBlock> BLAST = block("induction_blast_furnace", Variant.BLAST);
    static final DeferredBlock<AdvancedMachineBlock> SMOKER = block("induction_smoker", Variant.SMOKER);
    static final DeferredBlock<AdvancedMachineBlock> ROTARY = block("rotary_macerator", Variant.ROTARY);
    static final DeferredBlock<AdvancedMachineBlock> CENTRIFUGAL = block("centrifugal_extractor", Variant.CENTRIFUGAL);
    static final DeferredBlock<AdvancedMachineBlock> SINGULARITY = block("singularity_compressor", Variant.SINGULARITY);
    static final Supplier<BlockEntityType<AdvancedMachineEntity>> ENTITY = ENTITIES.register("induction_furnace",
        () -> BlockEntityType.Builder.of(AdvancedMachineEntity::new, FURNACE.get(), BLAST.get(), SMOKER.get(),
            ROTARY.get(), CENTRIFUGAL.get(), SINGULARITY.get()).build(null));
    static final Supplier<MenuType<AdvancedMachineMenu>> MENU = MENUS.register("induction_furnace",
        () -> new MenuType<>(AdvancedMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<AdvancedMachineMenu>> ROTARY_MENU = menu("rotary_macerator", Variant.ROTARY);
    static final Supplier<MenuType<AdvancedMachineMenu>> CENTRIFUGAL_MENU = menu("centrifugal_extractor", Variant.CENTRIFUGAL);
    static final Supplier<MenuType<AdvancedMachineMenu>> SINGULARITY_MENU = menu("singularity_compressor", Variant.SINGULARITY);

    private AdvancedMachines() {}

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        items.registerSimpleBlockItem(FURNACE);
        items.registerSimpleBlockItem(BLAST);
        items.registerSimpleBlockItem(SMOKER);
        items.registerSimpleBlockItem(ROTARY);
        items.registerSimpleBlockItem(CENTRIFUGAL);
        items.registerSimpleBlockItem(SINGULARITY);
        bus.addListener(AdvancedMachines::capabilities);
    }

    private static DeferredBlock<AdvancedMachineBlock> block(String id, Variant variant) {
        return BLOCKS.registerBlock(id, properties -> new AdvancedMachineBlock(properties, variant),
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), AdvancedMachineEntity::automation);
    }

    private static Supplier<MenuType<AdvancedMachineMenu>> menu(String id, Variant variant) {
        return MENUS.register(id, () -> new MenuType<>((window, player) ->
            new AdvancedMachineMenu(window, player, variant), FeatureFlags.DEFAULT_FLAGS));
    }

    enum Variant {
        FURNACE(RecipeType.SMELTING, 15), BLAST(RecipeType.BLASTING, 29), SMOKER(RecipeType.SMOKING, 29),
        ROTARY(2), CENTRIFUGAL(3), SINGULARITY(1);

        final RecipeType<? extends AbstractCookingRecipe> recipes;
        final int cost;
        final ProcessingLayout layout;

        Variant(RecipeType<? extends AbstractCookingRecipe> recipes, int cost) {
            this.recipes = recipes;
            this.cost = cost;
            this.layout = new ProcessingLayout(2, 2);
        }

        Variant(int outputs) {
            recipes = null;
            cost = 15;
            layout = new ProcessingLayout(1, outputs);
        }

        MenuType<AdvancedMachineMenu> menu() {
            return switch (this) {
                case ROTARY -> ROTARY_MENU.get();
                case CENTRIFUGAL -> CENTRIFUGAL_MENU.get();
                case SINGULARITY -> SINGULARITY_MENU.get();
                default -> MENU.get();
            };
        }
    }
}
