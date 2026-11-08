package ic2.core.machine;

import ic2.core.machine.MachineRecipeView.Process;
import ic2.core.machine.MachineRecipeView.Slot;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class MachineRecipeCategory implements IRecipeCategory<MachineRecipeView> {
    private static final int WIDTH = 148;
    private final Process process;
    private final IDrawable icon;
    private final IDrawable arrow;

    MachineRecipeCategory(Process process, IGuiHelper gui) {
        this.process = process;
        icon = gui.createDrawableItemLike(process.machine.get());
        arrow = gui.getRecipeArrow();
    }

    static RecipeType<MachineRecipeView> type(Process process) {
        return new RecipeType<>(process.machine.getId(), MachineRecipeView.class);
    }

    @Override
    public RecipeType<MachineRecipeView> getRecipeType() {
        return type(process);
    }

    @Override
    public Component getTitle() {
        return process.machine.get().getName();
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 86;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(MachineRecipeView recipe) {
        return recipe.sourceId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipeView recipe, IFocusGroup focuses) {
        addSlots(builder, recipe.inputs(), RecipeIngredientRole.INPUT, 8);
        addSlots(builder, recipe.outputs(), RecipeIngredientRole.OUTPUT, 111);
    }

    private static void addSlots(IRecipeLayoutBuilder builder, List<Slot> contents, RecipeIngredientRole role, int x) {
        for (int index = 0; index < contents.size(); index++) {
            var slot = builder.addSlot(role, x + index * 21, 6).setStandardSlotBackground();
            Slot ingredient = contents.get(index);
            if (ingredient.fluid().isEmpty()) {
                slot.addItemStacks(ingredient.items());
            } else {
                slot.addFluidStack(ingredient.fluid().getFluid(), ingredient.fluid().getAmount());
            }
        }
    }

    @Override
    public void draw(MachineRecipeView recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (!recipe.inputs().isEmpty() && !recipe.outputs().isEmpty()) {
            arrow.draw(graphics, 62, 6);
        }
        var font = Minecraft.getInstance().font;
        int y = 29;
        for (Component note : recipe.notes()) {
            for (var line : font.split(note, WIDTH - 12)) {
                graphics.drawString(font, line, 6, y, 0x404040, false);
                y += 10;
            }
        }
    }
}
