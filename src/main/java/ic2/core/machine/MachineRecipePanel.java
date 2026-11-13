package ic2.core.machine;

import com.mojang.blaze3d.vertex.Tesselator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;

/** Recipe rows on NeoForge's existing wheel/drag/clipping implementation. */
final class MachineRecipePanel extends ScrollPanel {
    private static final int ROW_HEIGHT = 18;
    private final List<MachineRecipeView> recipes;
    private final IntSupplier selected;
    private final IntConsumer select;
    private final BooleanSupplier visible;
    private final Consumer<ItemStack> hover;
    private final IntPredicate craftable;
    private final Consumer<List<Component>> hoverText;
    private boolean hovered;

    MachineRecipePanel(int x, int y, List<MachineRecipeView> recipes, IntSupplier selected,
                       IntConsumer select, BooleanSupplier visible, Consumer<ItemStack> hover, IntPredicate craftable, Consumer<List<Component>> hoverText) {
        super(Minecraft.getInstance(), 136, ROW_HEIGHT * 5, y, x, 0, 5,
            0xff141b23, 0xff596674, 0xff94a0ad);
        this.recipes = recipes;
        this.selected = selected;
        this.select = select;
        this.visible = visible;
        this.hover = hover;
        this.craftable = craftable;
        this.hoverText = hoverText;
    }

    void resetScroll() { scrollDistance = 0; }

    void reveal(int index) {
        int rowTop = index * ROW_HEIGHT;
        if (rowTop < scrollDistance) scrollDistance = rowTop;
        if (rowTop + ROW_HEIGHT > scrollDistance + height) scrollDistance = rowTop + ROW_HEIGHT - height;
    }

    @Override protected int getContentHeight() { return Math.max(height, recipes.size() * ROW_HEIGHT); }
    @Override protected int getScrollAmount() { return ROW_HEIGHT; }
    @Override public boolean isActive() { return visible.getAsBoolean(); }
    @Override public boolean isMouseOver(double x, double y) { return isActive() && super.isMouseOver(x, y); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        hovered = isMouseOver(mouseX, mouseY);
        if (isActive()) {
            graphics.flush();
            super.render(graphics, mouseX, mouseY, tick);
        }
    }

    @Override public NarrationPriority narrationPriority() { return hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE; }

    @Override public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE,
            Component.translatable("gui.ic2.remastered.recipe_page", selected.getAsInt() + 1, recipes.size()));
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!isActive() || recipes.isEmpty() || key != 264 && key != 265) return false;
        int index = Math.clamp(selected.getAsInt() + (key == 264 ? 1 : -1), 0, recipes.size() - 1);
        select.accept(index);
        reveal(index);
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return isMouseOver(x, y) && super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        return isMouseOver(x, y) && super.mouseClicked(x, y, button);
    }

    @Override protected boolean clickPanel(double x, double y, int button) {
        int index = (int) y / ROW_HEIGHT;
        if (button != 0 || index < 0 || index >= recipes.size()) return false;
        select.accept(index);
        return true;
    }

    @Override protected void drawBackground(GuiGraphics graphics, Tesselator tess, float tick) {
        graphics.fill(left, top, right, bottom, 0xffa7aeb9);
    }

    @Override protected void drawPanel(GuiGraphics graphics, int entryRight, int baseY,
                                       Tesselator tess, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < recipes.size(); i++) {
            int y = baseY + i * ROW_HEIGHT;
            if (y + ROW_HEIGHT <= top || y >= bottom) continue;
            boolean chosen = i == selected.getAsInt();
            boolean available = craftable.test(i);
            graphics.fill(left, y, right - 6, y + ROW_HEIGHT,
                chosen ? RemasteredGui.ORANGE : available ? RemasteredGui.GREEN : 0xff48535f);
            graphics.fill(left + 1, y + 1, right - 7, y + ROW_HEIGHT - 1,
                available ? 0xff516851 : 0xff8d949f);
            graphics.fill(left + 2, y + 2, right - 8, y + 3, 0xffc4c9d0);
            var recipe = recipes.get(i);
            if (!recipe.inputs().isEmpty()) item(graphics, recipe.inputs().get(0), left + 4, y + 1, mouseX, mouseY);
            if (recipe.inputs().size() > 1) {
                graphics.drawString(font, "+", left + 28, y + 5, 0xff252b33, false);
                item(graphics, recipe.inputs().get(1), left + 42, y + 1, mouseX, mouseY);
            }
            if (recipe.inputs().size() > 2) item(graphics, recipe.inputs().get(2), left + 58, y + 1, mouseX, mouseY);
            graphics.drawString(font, ">", left + 69, y + 5, 0xff252b33, false);
            if (!recipe.outputs().isEmpty()) item(graphics, recipe.outputs().getFirst(), left + 85, y + 1, mouseX, mouseY);
            graphics.drawString(font, Integer.toString(i + 1), left + 113, y + 5,
                available ? 0xff164d19 : 0xff252b33, false);
        }
        // NeoForge's panel uses a RenderSystem scissor; flush before it disables clipping.
        graphics.flush();
    }

    private void item(GuiGraphics graphics, MachineRecipeView.Slot slot, int x, int y, int mouseX, int mouseY) {
        if (!slot.fluid().isEmpty()) {
            drawFluid(graphics, slot.fluid(), x, y);
            if (isMouseOver(mouseX, mouseY) && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                hoverText.accept(List.of(slot.fluid().getHoverName(), Component.literal(slot.fluid().getAmount() + " mB")));
            return;
        }
        if (slot.items().isEmpty()) return;
        var client = Minecraft.getInstance();
        int cycle = (int) (client.level.getGameTime() / 20 % slot.items().size());
        ItemStack stack = slot.items().get(cycle);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(1f, 1f, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(client.font, stack, 0, 0);
        graphics.pose().popPose();
        if (isMouseOver(mouseX, mouseY) && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
            hover.accept(stack);
    }

    static void drawFluid(GuiGraphics graphics, net.neoforged.neoforge.fluids.FluidStack fluid, int x, int y) {
        var extension = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = extension.getStillTexture(fluid);
        if (texture == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
        int tint = extension.getTintColor(fluid);
        graphics.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, (tint >>> 24) / 255f);
        graphics.blit(x, y, 0, 16, 16, sprite);
        graphics.setColor(1, 1, 1, 1);
    }
}
