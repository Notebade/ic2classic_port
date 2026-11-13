package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.UraniumType;
import java.util.Arrays;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;

final class UraniumEnrichment {
    static final int PACKET_LIMIT = 512;
    static final int INGOT_TICKS = 1_000;
    static final int MAX_POINTS = 1_000;
    private static final int CATALYST_TICKS = 100;
    private static final int CATALYST_EU = 100;
    private static final TagKey<Item> CHARCOAL_BLOCKS =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/charcoal"));

    private Blend blend;
    private int points;
    private int ingotProgress;
    private int catalystProgress;

    boolean acceptsCatalyst(ItemStack stack) {
        Catalyst catalyst = catalystFor(stack);
        return catalyst != null && (points == 0 || catalyst.blend() == blend);
    }

    int tick(ItemStackHandler inventory, int energy) {
        int spent = enrichIngot(inventory, energy);
        return spent + processCatalyst(inventory, energy - spent);
    }

    private int enrichIngot(ItemStackHandler inventory, int energy) {
        if (blend == null || points < blend.requiredPoints || energy < blend.euPerTick
                || !inventory.getStackInSlot(MachineEntity.INPUT_SLOT).is(IC2.URANIUM_INGOT.get())) {
            ingotProgress = 0;
            return 0;
        }
        var result = IC2.ENRICHED_URANIUM.ingot(blend.type);
        var output = inventory.getStackInSlot(MachineEntity.OUTPUT_SLOT);
        if (!output.isEmpty() && (!output.is(result) || output.getCount() >= output.getMaxStackSize())) {
            ingotProgress = 0;
            return 0;
        }
        if (++ingotProgress == INGOT_TICKS) {
            ingotProgress = 0;
            points -= blend.requiredPoints;
            inventory.extractItem(MachineEntity.INPUT_SLOT, 1, false);
            inventory.setStackInSlot(MachineEntity.OUTPUT_SLOT, new ItemStack(result, output.getCount() + 1));
        }
        return blend.euPerTick;
    }

    private int processCatalyst(ItemStackHandler inventory, int energy) {
        var catalyst = catalystFor(inventory.getStackInSlot(MachineEntity.SECOND_INPUT_SLOT));
        if (energy < CATALYST_EU || catalyst == null || points > 0 && catalyst.blend() != blend
                || points + catalyst.points() > MAX_POINTS) {
            catalystProgress = 0;
            return 0;
        }
        if (++catalystProgress == CATALYST_TICKS) {
            catalystProgress = 0;
            blend = catalyst.blend();
            points += catalyst.points();
            inventory.extractItem(MachineEntity.SECOND_INPUT_SLOT, 1, false);
        }
        return CATALYST_EU;
    }

    int ingotProgress() {
        return ingotProgress;
    }

    int points() {
        return points;
    }

    CompoundTag save() {
        var tag = new CompoundTag();
        if (blend != null && points > 0) {
            tag.putString("blend", blend.type.id());
            tag.putInt("points", points);
            tag.putInt("ingot_progress", ingotProgress);
        }
        tag.putInt("catalyst_progress", catalystProgress);
        return tag;
    }

    void load(CompoundTag tag) {
        String id = tag.getString("blend");
        blend = Arrays.stream(Blend.values()).filter(value -> value.type.id().equals(id)).findFirst().orElse(null);
        points = blend == null ? 0 : Math.clamp(tag.getInt("points"), 0, MAX_POINTS);
        ingotProgress = points == 0 ? 0 : Math.clamp(tag.getInt("ingot_progress"), 0, INGOT_TICKS - 1);
        catalystProgress = Math.clamp(tag.getInt("catalyst_progress"), 0, CATALYST_TICKS - 1);
    }

    private static Catalyst catalystFor(ItemStack stack) {
        if (stack.is(Items.REDSTONE)) {
            return new Catalyst(Blend.REDSTONE, 25);
        }
        if (stack.is(Items.REDSTONE_BLOCK)) {
            return new Catalyst(Blend.REDSTONE, 200);
        }
        if (stack.is(Items.CHARCOAL)) {
            return new Catalyst(Blend.CHARCOAL, 25);
        }
        if (stack.is(CHARCOAL_BLOCKS)) {
            return new Catalyst(Blend.CHARCOAL, 200);
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return new Catalyst(Blend.BLAZE, 200);
        }
        if (stack.is(Items.ENDER_PEARL)) {
            return new Catalyst(Blend.ENDER_PEARL, 100);
        }
        return stack.is(Items.NETHER_STAR) ? new Catalyst(Blend.NETHER_STAR, 200) : null;
    }

    static java.util.List<MachineRecipeView> recipeViews() {
        var recipes = new java.util.ArrayList<MachineRecipeView>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            var catalyst = catalystFor(stack);
            if (catalyst == null) continue;
            var blend = catalyst.blend();
            int count = (blend.requiredPoints + catalyst.points() - 1) / catalyst.points();
            recipes.add(new MachineRecipeView(
                java.util.List.of(MachineRecipeView.Slot.item(IC2.URANIUM_INGOT.get()),
                    MachineRecipeView.Slot.item(stack.copyWithCount(count))),
                java.util.List.of(MachineRecipeView.Slot.item(IC2.ENRICHED_URANIUM.ingot(blend.type))),
                MachineRecipeViews.note("cost", INGOT_TICKS, (long) INGOT_TICKS * blend.euPerTick),
                MachineRecipeViews.note("enrichment_catalyst", count * catalyst.points(), blend.requiredPoints,
                    count * CATALYST_TICKS, (long) count * CATALYST_TICKS * CATALYST_EU)));
        }
        return java.util.List.copyOf(recipes);
    }

    private enum Blend {
        BLAZE(UraniumType.BLAZE, 100, 20),
        CHARCOAL(UraniumType.CHARCOAL, 100, 5),
        ENDER_PEARL(UraniumType.ENDER_PEARL, 100, 75),
        NETHER_STAR(UraniumType.NETHER_STAR, 150, 150),
        REDSTONE(UraniumType.REDSTONE, 100, 25);

        private final UraniumType type;
        private final int requiredPoints;
        private final int euPerTick;

        Blend(UraniumType type, int requiredPoints, int euPerTick) {
            this.type = type;
            this.requiredPoints = requiredPoints;
            this.euPerTick = euPerTick;
        }
    }

    private record Catalyst(Blend blend, int points) {}
}
