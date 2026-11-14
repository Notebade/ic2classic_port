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

/** Shared Remastered processor screen, backed by existing machine menus and recipe sources. */
class ProcessingMachineScreen<T extends net.minecraft.world.inventory.AbstractContainerMenu & ProcessingMenu> extends AbstractContainerScreen<T> {
    private enum Tab { WORK, RECIPES, UPGRADES, ENERGY }
    private MachineRecipeView.Process currentProcess;
    private Tab tab = Tab.WORK;
    private int recipeIndex;
    private List<MachineRecipeView> allRecipes = List.of();
    private final List<MachineRecipeView> recipes = new ArrayList<>();
    private String query = "";
    private Button previous;
    private Button next;
    private Button craft;
    private final List<Button> controls = new ArrayList<>();
    private EditBox search;
    private MachineRecipePanel recipePanel;
    private ItemStack hoveredRecipe = ItemStack.EMPTY;
    private List<Component> hoveredText = List.of();
    private boolean recipePointerCaptured;

    ProcessingMachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AlloyGuiLayout.WIDTH;
        imageHeight = AlloyGuiLayout.HEIGHT;
        inventoryLabelX = AlloyGuiLayout.SLOT_X;
        inventoryLabelY = 150;
    }

    @Override protected void init() {
        super.init();
        currentProcess = menu.process();
        allRecipes = menu.recipeViews(minecraft.level);
        recipes.clear();
        recipes.addAll(allRecipes);
        recipeIndex = Math.clamp(recipeIndex, 0, Math.max(0, recipes.size() - 1));
        for (Tab choice : Tab.values()) {
            ItemStack icon = new ItemStack(switch (choice) {
                case WORK -> currentProcess == null ? net.minecraft.world.item.Items.FURNACE : currentProcess.machine.get().asItem();
                case RECIPES -> net.minecraft.world.item.Items.BOOK;
                case UPGRADES -> net.minecraft.world.item.Items.COMPARATOR;
                case ENERGY -> net.minecraft.world.item.Items.REDSTONE_TORCH;
            });
            addRenderableWidget(new RemasteredIconButton(leftPos + 152 + choice.ordinal() * 23, topPos + 4,
                text("tab." + choice.name().toLowerCase(Locale.ROOT)), icon, ignored -> select(choice), () -> tab == choice));
        }
        search = addRenderableWidget(new EditBox(font, leftPos + 14, topPos + 32, 136, 14, text("search")));
        search.setMaxLength(64);
        search.setHint(text("search"));
        recipePanel = addRenderableWidget(new MachineRecipePanel(leftPos + 14, topPos + 49, recipes,
            () -> recipeIndex, index -> { recipeIndex = index; updateRecipeButtons(); }, () -> tab == Tab.RECIPES,
            stack -> hoveredRecipe = stack, this::canCraft, lines -> hoveredText = lines));
        recipePanel.reveal(recipeIndex);
        previous = addRenderableWidget(new RemasteredButton(leftPos + 160, topPos + 130, 16, 12,
            Component.literal("<"), ignored -> changeRecipe(-1), () -> false));
        next = addRenderableWidget(new RemasteredButton(leftPos + 226, topPos + 130, 16, 12,
            Component.literal(">"), ignored -> changeRecipe(1), () -> false));
        craft = addRenderableWidget(new RemasteredButton(leftPos + 178, topPos + 130, 46, 12,
            text("create"), ignored -> {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    (menu instanceof MachineMenu basic && basic.isAlloySmelter() ? MachineMenu.recipeButton(allRecipes.indexOf(recipes.get(recipeIndex)), hasShiftDown()) : ProcessingTransfer.button(allRecipes.indexOf(recipes.get(recipeIndex)), hasShiftDown())));
                select(Tab.WORK);
            }, () -> false));
        craft.setTooltip(Tooltip.create(text("create_hint")));
        controls.clear();
        var actions = menu.controls();
        for (int i = 0; i < actions.size(); i++) {
            var action = actions.get(i);
            int width = 152 / actions.size();
            controls.add(addRenderableWidget(new RemasteredButton(leftPos + 90 + i * width, topPos + 130, width - 2, 12,
                action.label(), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action.id()), () -> false)));
        }
        search.setResponder(this::filterRecipes);
        search.setValue(query);
        select(tab);
    }

    private void select(Tab choice) {
        if (recipePointerCaptured) {
            recipePanel.mouseReleased(0, 0, 0);
            recipePointerCaptured = false;
            setDragging(false);
        }
        tab = choice;
        menu.showMachineSlots(tab == Tab.WORK);
        previous.visible = next.visible = tab == Tab.RECIPES;
        search.visible = craft.visible = tab == Tab.RECIPES;
        controls.forEach(button -> button.visible = tab == Tab.WORK);
        if (!search.visible && search.isFocused()) setFocused(null);
        updateRecipeButtons();
    }

    private void filterRecipes(String value) {
        query = value;
        var previousRecipe = recipes.isEmpty() ? null : recipes.get(recipeIndex);
        String needle = value.strip().toLowerCase(Locale.ROOT);
        recipes.clear();
        allRecipes.stream().filter(recipe -> recipe.outputs().stream().flatMap(slot -> slot.items().stream())
            .anyMatch(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle))
            || recipe.outputs().stream().anyMatch(slot -> !slot.fluid().isEmpty()
                && slot.fluid().getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)))
            .forEach(recipes::add);
        recipeIndex = Math.max(0, recipes.indexOf(previousRecipe));
        recipePanel.resetScroll();
        recipePanel.reveal(recipeIndex);
        updateRecipeButtons();
    }

    private boolean canCraft(int index) {
        return index >= 0 && index < recipes.size() && (menu instanceof MachineMenu basic && basic.isAlloySmelter() ? basic.canLoadRecipe(allRecipes.indexOf(recipes.get(index))) : ProcessingTransfer.plan(menu, menu, minecraft.level, allRecipes.indexOf(recipes.get(index)), false) != null);
    }

    private void updateRecipeButtons() {
        previous.active = next.active = recipes.size() > 1;
        craft.active = canCraft(recipeIndex);
        boolean fluid = !recipes.isEmpty() && recipes.get(recipeIndex).inputs().stream().anyMatch(slot -> !slot.fluid().isEmpty());
        craft.setTooltip(Tooltip.create(text(fluid ? "fluid_external" : "create_hint")));
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.visible && search.isFocused() && key != 256) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (tab == Tab.RECIPES && button == 0 && recipePanel.isMouseOver(x, y) && recipePanel.mouseClicked(x, y, button)) {
            setFocused(recipePanel);
            setDragging(true);
            recipePointerCaptured = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        if (recipePointerCaptured && button == 0) {
            recipePanel.mouseDragged(x, y, button, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(x, y, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (recipePointerCaptured && button == 0) {
            recipePanel.mouseReleased(x, y, button);
            recipePointerCaptured = false;
            setDragging(false);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    private void changeRecipe(int delta) {
        if (!recipes.isEmpty()) {
            recipeIndex = Math.floorMod(recipeIndex + delta, recipes.size());
            recipePanel.reveal(recipeIndex);
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (currentProcess != menu.process()) rebuildWidgets();
        var actions = menu.controls();
        for (int i = 0; i < controls.size() && i < actions.size(); i++) controls.get(i).setMessage(actions.get(i).label());
        controls.forEach(button -> button.visible = tab == Tab.WORK);
    }

    @Override public void removed() {
        menu.showMachineSlots(true);
        super.removed();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredRecipe = ItemStack.EMPTY;
        hoveredText = List.of();
        updateRecipeButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!hoveredRecipe.isEmpty()) graphics.renderTooltip(font, hoveredRecipe, mouseX, mouseY);
        if (!hoveredText.isEmpty()) graphics.renderComponentTooltip(font, hoveredText, mouseX, mouseY);
        if (tab == Tab.RECIPES && !recipes.isEmpty() && mouseX >= leftPos + 155 && mouseX < leftPos + 245
                && mouseY >= topPos + 99 && mouseY < topPos + 127)
            graphics.renderComponentTooltip(font, recipes.get(recipeIndex).notes(), mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        RemasteredGui.background(graphics, leftPos, topPos);
        if (tab == Tab.WORK) {
            graphics.fill(leftPos + 9, topPos + 32, leftPos + 247, topPos + 143, 0xffafb5c0);
            graphics.fill(leftPos + 12, topPos + 35, leftPos + 82, topPos + 140, 0xff303640);
            graphics.fill(leftPos + 185, topPos + 89, leftPos + 243, topPos + 115, 0xff303640);
            graphics.fill(leftPos + 106, topPos + 81, leftPos + 110, topPos + 99, 0xff343d48);
            graphics.fill(leftPos + 108, topPos + 95, leftPos + 177, topPos + 99, 0xff343d48);
            graphics.fill(leftPos + 107, topPos + 82, leftPos + 108, topPos + 97, 0xffe0e4e9);
        }
        for (var slot : menu.slots) {
            if (slot.isActive()) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        if (tab == Tab.WORK) {
            RemasteredGui.verticalBar(graphics, leftPos + 18, topPos + 63, 7, 70, menu.gaugeValue(), menu.gaugeCapacity());
            RemasteredGui.bar(graphics, leftPos + 190, topPos + 107, 48, 5, menu.workProgress(), menu.workTotal(), RemasteredGui.GREEN);
            if (currentProcess != null && menu.machineSlotCount() < 8) {
                graphics.pose().pushPose();
                graphics.pose().translate(leftPos + 123, topPos + 86, 0);
                graphics.pose().scale(1.7f, 1.7f, 1);
                graphics.renderItem(new ItemStack(currentProcess.machine.get()), 0, 0);
                graphics.pose().popPose();
            }
        } else if (tab == Tab.RECIPES) {
            graphics.fill(leftPos + 155, topPos + 32, leftPos + 245, topPos + 144, 0xff697380);
            graphics.fill(leftPos + 156, topPos + 33, leftPos + 244, topPos + 143, 0xffc0c5ce);
        } else if (tab == Tab.ENERGY) {
            RemasteredGui.bar(graphics, leftPos + 18, topPos + 78, 220, 10, menu.storedEnergy(), menu.energyCapacity(), RemasteredGui.GREEN);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "IC²", 9, 10, 0xff252b33, false);
        float titleScale = Math.min(1f, 116f / Math.max(1, font.width(title)));
        graphics.pose().pushPose();
        graphics.pose().translate(33, 10, 0);
        graphics.pose().scale(titleScale, titleScale, 1);
        graphics.drawString(font, title, 0, 0, 0xff252b33, false);
        graphics.pose().popPose();
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff252b33, false);
        switch (tab) {
            case WORK -> work(graphics);
            case RECIPES -> recipe(graphics);
            case UPGRADES -> upgrades(graphics);
            case ENERGY -> energy(graphics);
        }
    }

    private void work(GuiGraphics graphics) {
        if (menu.energyCapacity() > 0) RemasteredGui.lightning(graphics, 17, 36);
        label(graphics, menu.energyCapacity() == 0 ? text("fuel") : Component.literal("EU"), menu.energyCapacity() == 0 ? 18 : 31, 39, RemasteredGui.TEXT);
        if (menu.energyCapacity() > 0) {
            label(graphics, Integer.toString(menu.storedEnergy()), 31, 65, RemasteredGui.TEXT);
            label(graphics, "/ " + menu.energyCapacity(), 31, 78, RemasteredGui.MUTED);
        } else label(graphics, (menu.gaugeValue() * 100 / Math.max(1, menu.gaugeCapacity())) + "%", 31, 65, RemasteredGui.TEXT);
        label(graphics, (menu.workProgress() * 100L / Math.max(1, menu.workTotal())) + "%", 190, 95, RemasteredGui.GREEN);
        var details = menu.workDetails();
        if (!details.isEmpty()) graphics.drawString(font, font.plainSubstrByWidth(details.getFirst().getString(), 150), 88, 119, 0xff252b33, false);
    }
    private void recipe(GuiGraphics graphics) {
        if (recipes.isEmpty()) {
            graphics.drawWordWrap(font, text("no_recipes"), 160, 54, 82, RemasteredGui.MUTED);
            return;
        }
        MachineRecipeView recipe = recipes.get(recipeIndex);
        label(graphics, text("recipe_page", recipeIndex + 1, recipes.size()), 160, 36, 0xff252b33);
        var output = recipe.outputs().isEmpty() ? java.util.List.<ItemStack>of() : recipe.outputs().getFirst().items();
        if (!output.isEmpty()) {
            var lines = font.split(output.getFirst().getHoverName(), 82);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                graphics.drawString(font, lines.get(i), 160, 49 + i * 9, 0xff252b33, false);
            graphics.pose().pushPose();
            graphics.pose().translate(170, 71, 0);
            graphics.pose().scale(1.25f, 1.25f, 1);
            graphics.renderItem(output.getFirst(), 0, 0);
            graphics.renderItemDecorations(font, output.getFirst(), 0, 0);
            graphics.pose().popPose();
        } else if (!recipe.outputs().isEmpty() && !recipe.outputs().getFirst().fluid().isEmpty()) {
            var fluid = recipe.outputs().getFirst().fluid();
            graphics.drawWordWrap(font, fluid.getHoverName(), 160, 49, 82, 0xff252b33);
            MachineRecipePanel.drawFluid(graphics, fluid, 170, 76);
        }
        for (Component note : recipe.notes()) {
            if (note.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                    && (key.getKey().equals("jei.ic2.cost") || key.getKey().equals("jei.ic2.chemical_recipe"))
                    && key.getArgs().length >= 2 && key.getArgs()[0] instanceof Number ticks && key.getArgs()[1] instanceof Number energy) {
                label(graphics, energy.longValue() + " EU", 173, 94, 0xff252b33);
                label(graphics, text("recipe_seconds", seconds(ticks.intValue())), 173, 106, 0xff252b33);
                label(graphics, (ticks.intValue() == 0 ? 0 : energy.longValue() / ticks.intValue()) + " EU/t", 173, 118, 0xff252b33);
                RemasteredGui.lightning(graphics, 160, 92);
                graphics.pose().pushPose();
                graphics.pose().translate(160, 105, 0);
                graphics.pose().scale(.6f, .6f, 1);
                graphics.renderItem(new ItemStack(net.minecraft.world.item.Items.CLOCK), 0, 0);
                graphics.pose().popPose();
                return;
            }
        }
        int y = 93;
        for (Component note : recipe.notes()) {
            for (var line : font.split(note, 82)) {
                if (y > 120) break;
                graphics.drawString(font, line, 160, y, 0xff252b33, false);
                y += 9;
            }
        }
    }
    private void upgrades(GuiGraphics graphics) {
        graphics.drawWordWrap(font, text("upgrades_unavailable"), 18, 55, 220, RemasteredGui.MUTED);
        label(graphics, text("base_stats"), 18, 91, RemasteredGui.TEXT);

        if (!menu.workDetails().isEmpty()) graphics.drawWordWrap(font, menu.workDetails().getFirst(), 18, 108, 220, RemasteredGui.MUTED);
        label(graphics, text("capacity", menu.energyCapacity()), 18, 134, RemasteredGui.TEXT);
    }

    private void energy(GuiGraphics graphics) {
        label(graphics, text("capacity", menu.energyCapacity()), 18, 55, RemasteredGui.TEXT);
        label(graphics, menu.storedEnergy() + " / " + menu.energyCapacity() + " EU", 18, 68, RemasteredGui.GREEN);

        int y = 100; for (Component detail : menu.workDetails()) { graphics.drawWordWrap(font, detail, 18, y, 220, RemasteredGui.MUTED); y += 18; if (y > 128) break; }
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
