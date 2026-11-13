package ic2.api.recipe;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Single-input processing recipe; outputs contain default item components. */
public record ProcessingRecipe(ResourceLocation id, Process process, Item input, int inputCount, Item output, int outputCount) {
    public enum Process { COMPRESSION, EXTRACTION }
    public ProcessingRecipe {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(process, "process");
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(output, "output");
        if (input.getDefaultInstance().isEmpty() || output.getDefaultInstance().isEmpty()) throw new IllegalArgumentException("Air is not a recipe item");
        if (inputCount < 1 || inputCount > input.getDefaultMaxStackSize() || outputCount < 1 || outputCount > output.getDefaultMaxStackSize())
            throw new IllegalArgumentException("Recipe counts must fit item stacks");
        if (process == Process.COMPRESSION && outputCount != 1 || process == Process.EXTRACTION && inputCount != 1)
            throw new IllegalArgumentException("Compression has one output item; extraction consumes one input item");
    }
}
