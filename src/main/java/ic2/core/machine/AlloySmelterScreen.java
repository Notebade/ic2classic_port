package ic2.core.machine;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** First Remastered screen, backed by the existing alloy menu and JEI recipe views. */
final class AlloySmelterScreen extends AbstractContainerScreen<MachineMenu> {
    private enum Tab { WORK, RECIPES, UPGRADES, ENERGY }
    private static final MachineKind KIND = MachineKind.ALLOY_SMELTER;
    private Tab tab = Tab.WORK;
    private int recipeIndex;
    private List<MachineRecipeView> allRecipes = List.of();
    private final List<MachineRecipeView> recipes = new ArrayList<>();
    private String query = "";
    private Button previous;
    private Button next;
    private Button craft;
    private EditBox search;
    private MachineRecipePanel recipePanel;
    private ItemStack hoveredRecipe = ItemStack.EMPTY;

    AlloySmelterScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AlloyGuiLayout.WIDTH;
        imageHeight = AlloyGuiLayout.HEIGHT;
        inventoryLabelX = AlloyGuiLayout.SLOT_X;
        inventoryLabelY = 150;
    }

    @Override protected void init() {
        super.init();
        allRecipes = MachineRecipeViews.forProcess(MachineRecipeView.Process.ALLOY_SMELTER, minecraft.level);
        recipes.clear();
        recipes.addAll(allRecipes);
        recipeIndex = Math.clamp(recipeIndex, 0, Math.max(0, recipes.size() - 1));
        for (Tab choice : Tab.values()) {
            addRenderableWidget(new RemasteredButton(leftPos + 8 + choice.ordinal() * 60, topPos + 24, 59, 20,
                text("tab." + choice.name().toLowerCase(Locale.ROOT)), ignored -> select(choice), () -> tab == choice));
        }
        search = addRenderableWidget(new EditBox(font, leftPos + 14, topPos + 52, 136, 14, text("search")));
        search.setMaxLength(64);
        search.setHint(text("search"));
        recipePanel = addRenderableWidget(new MachineRecipePanel(leftPos + 14, topPos + 70, recipes,
            () -> recipeIndex, index -> { recipeIndex = index; updateRecipeButtons(); }, () -> tab == Tab.RECIPES,
            stack -> hoveredRecipe = stack, this::canCraft));
        recipePanel.reveal(recipeIndex);
        previous = addRenderableWidget(new RemasteredButton(leftPos + 160, topPos + 130, 16, 12,
            Component.literal("<"), ignored -> changeRecipe(-1), () -> false));
        next = addRenderableWidget(new RemasteredButton(leftPos + 226, topPos + 130, 16, 12,
            Component.literal(">"), ignored -> changeRecipe(1), () -> false));
        craft = addRenderableWidget(new RemasteredButton(leftPos + 178, topPos + 130, 46, 12,
            text("create"), ignored -> {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    MachineMenu.recipeButton(allRecipes.indexOf(recipes.get(recipeIndex)), hasShiftDown()));
                select(Tab.WORK);
            }, () -> false));
        craft.setTooltip(Tooltip.create(text("create_hint")));
        search.setResponder(this::filterRecipes);
        search.setValue(query);
        select(tab);
    }

    private void select(Tab choice) {
        tab = choice;
        menu.showMachineSlots(tab == Tab.WORK);
        previous.visible = next.visible = tab == Tab.RECIPES;
        search.visible = craft.visible = tab == Tab.RECIPES;
        if (!search.visible && search.isFocused()) setFocused(null);
        updateRecipeButtons();
    }

    private void filterRecipes(String value) {
        query = value;
        var previousRecipe = recipes.isEmpty() ? null : recipes.get(recipeIndex);
        String needle = value.strip().toLowerCase(Locale.ROOT);
        recipes.clear();
        allRecipes.stream().filter(recipe -> recipe.outputs().stream().flatMap(slot -> slot.items().stream())
            .anyMatch(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)))
            .forEach(recipes::add);
        recipeIndex = Math.max(0, recipes.indexOf(previousRecipe));
        recipePanel.resetScroll();
        recipePanel.reveal(recipeIndex);
        updateRecipeButtons();
    }

    private boolean canCraft(int index) {
        return index >= 0 && index < recipes.size() && menu.canLoadRecipe(allRecipes.indexOf(recipes.get(index)));
    }

    private void updateRecipeButtons() {
        previous.active = next.active = recipes.size() > 1;
        craft.active = canCraft(recipeIndex);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.visible && search.isFocused() && key != 256) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void changeRecipe(int delta) {
        if (!recipes.isEmpty()) {
            recipeIndex = Math.floorMod(recipeIndex + delta, recipes.size());
            recipePanel.reveal(recipeIndex);
        }
    }

    @Override public void removed() {
        menu.showMachineSlots(true);
        super.removed();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredRecipe = ItemStack.EMPTY;
        updateRecipeButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!hoveredRecipe.isEmpty()) graphics.renderTooltip(font, hoveredRecipe, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        RemasteredGui.background(graphics, leftPos, topPos);
        for (var slot : menu.slots) {
            if (slot.isActive()) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        if (tab == Tab.WORK) {
            RemasteredGui.bar(graphics, leftPos + 18, topPos + 90, 62, 7, menu.energy(), KIND.capacity, RemasteredGui.GREEN);
            RemasteredGui.bar(graphics, leftPos + 100, topPos + 96, 120, 8, menu.work(), KIND.recipeTicks, RemasteredGui.ORANGE);
        } else if (tab == Tab.ENERGY) {
            RemasteredGui.bar(graphics, leftPos + 18, topPos + 78, 220, 10, menu.energy(), KIND.capacity, RemasteredGui.GREEN);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), 232), 12, 10, 0xff252b33, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, RemasteredGui.TEXT, false);
        switch (tab) {
            case WORK -> work(graphics);
            case RECIPES -> recipe(graphics);
            case UPGRADES -> upgrades(graphics);
            case ENERGY -> energy(graphics);
        }
    }

    private void work(GuiGraphics graphics) {
        label(graphics, "EU", 18, 57, RemasteredGui.GREEN);
        label(graphics, menu.energy() + " /", 18, 70, RemasteredGui.TEXT);
        label(graphics, KIND.capacity + " EU", 18, 80, RemasteredGui.MUTED);
        label(graphics, "+", 122, 69, RemasteredGui.TEXT);
        label(graphics, ">", 178, 69, RemasteredGui.ORANGE);
        label(graphics, text("input"), 100, 53, RemasteredGui.MUTED);
        label(graphics, text("output"), 198, 53, RemasteredGui.MUTED);
        label(graphics, (menu.work() * 100 / KIND.recipeTicks) + "%", 100, 108, RemasteredGui.ORANGE);
        label(graphics, text("remaining", seconds(Math.max(0, KIND.recipeTicks - menu.work()))), 145, 108, RemasteredGui.MUTED);
        label(graphics, text("draw", menu.fluid() == 1 ? KIND.energyPerTick : 0), 18, 122, RemasteredGui.TEXT);
        label(graphics, text(menu.fluid() == 1 ? "running" : "idle"), 18, 135,
            menu.fluid() == 1 ? RemasteredGui.GREEN : RemasteredGui.MUTED);
        // The server's LIT bit gates the animation, including pauses and full outputs.
        if (menu.fluid() == 1) {
            int phase = (int) (minecraft.level.getGameTime() / 4 % 5);
            for (int i = 0; i < 5; i++) graphics.fill(185 + i * 7, 126, 189 + i * 7, 139,
                i == phase ? RemasteredGui.ORANGE : 0xff65462d);
        }
    }

    private void recipe(GuiGraphics graphics) {
        if (recipes.isEmpty()) {
            graphics.drawWordWrap(font, text("no_recipes"), 160, 54, 82, RemasteredGui.MUTED);
            return;
        }
        MachineRecipeView recipe = recipes.get(recipeIndex);
        label(graphics, text("recipe_page", recipeIndex + 1, recipes.size()), 160, 54, RemasteredGui.TEXT);
        var output = recipe.outputs().getFirst().items();
        if (!output.isEmpty()) {
            var lines = font.split(output.getFirst().getHoverName(), 82);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                graphics.drawString(font, lines.get(i), 160, 68 + i * 9, RemasteredGui.MUTED, false);
        }
        label(graphics, KIND.recipeTicks * KIND.energyPerTick + " EU", 160, 96, RemasteredGui.GREEN);
        label(graphics, text("recipe_seconds", seconds(KIND.recipeTicks)), 160, 107, RemasteredGui.MUTED);
        label(graphics, KIND.energyPerTick + " EU/t", 160, 118, RemasteredGui.MUTED);
    }

    private void upgrades(GuiGraphics graphics) {
        graphics.drawWordWrap(font, text("upgrades_unavailable"), 18, 55, 220, RemasteredGui.MUTED);
        label(graphics, text("base_stats"), 18, 91, RemasteredGui.TEXT);
        label(graphics, text("draw", KIND.energyPerTick), 18, 106, RemasteredGui.GREEN);
        label(graphics, text("duration", seconds(KIND.recipeTicks)), 18, 120, RemasteredGui.TEXT);
        label(graphics, text("capacity", KIND.capacity), 18, 134, RemasteredGui.TEXT);
    }

    private void energy(GuiGraphics graphics) {
        label(graphics, text("capacity", KIND.capacity), 18, 55, RemasteredGui.TEXT);
        label(graphics, menu.energy() + " / " + KIND.capacity + " EU", 18, 68, RemasteredGui.GREEN);
        label(graphics, text("draw", KIND.energyPerTick), 18, 100, RemasteredGui.TEXT);
        graphics.drawWordWrap(font, text("packet_unlimited"), 18, 116, 220, RemasteredGui.MUTED);
    }

    private void label(GuiGraphics graphics, String value, int x, int y, int color) {
        label(graphics, Component.literal(value), x, y, color);
    }

    private void label(GuiGraphics graphics, Component value, int x, int y, int color) {
        graphics.drawString(font, value, x, y, color, false);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gui.ic2.remastered." + key, arguments);
    }
}
