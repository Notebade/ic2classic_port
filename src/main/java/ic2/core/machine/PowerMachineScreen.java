package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class PowerMachineScreen extends AbstractContainerScreen<PowerMachineMenu> {
    private final GenerationLayout layout;
    private final java.util.List<Component> details = new java.util.ArrayList<>();
    PowerMachineScreen(PowerMachineMenu menu, Inventory player, Component title) {
        super(menu, player, title);
        layout = GenerationLayout.forId(menu.kind.id);
        if (menu.plasmaPanel()) imageWidth = 232;
        imageHeight = menu.plasmaPanel() ? 214 : menu.steamPanel() ? 206 : menu.generatorPanel() ? 189 : layout == null ? 214 : layout.height();
    }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        for (var entry : PowerMachines.SCREENS.entrySet()) {
            var kind = entry.getKey();
            if (kind == PowerMachines.Kind.GASIFIER || kind == PowerMachines.Kind.ELECTRIC_GASIFIER || kind == PowerMachines.Kind.REFINERY)
                event.register(entry.getValue().get(), ProcessingMachineScreen<PowerMachineMenu>::new);
            else event.register(entry.getValue().get(), PowerMachineScreen::new);
        }
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(BoilerFuel.CAN.get(), ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "fueled"),
                (stack, level, entity, seed) -> BoilerFuel.amount(stack) > 0 ? 1 : 0);
            ItemProperties.register(ChemistryMaterials.WHISKY_MUG.get(), ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "whisky_grade"),
                (stack, level, entity, seed) -> ChemistryMaterials.grade(stack));
        });
    }
    @Override protected void init() {
        super.init();
        if (menu.kind == PowerMachines.Kind.REFINERY) {
            chemicalButton(6, "1 → out", 8, 50); chemicalButton(7, "2 → out", 61, 50); chemicalButton(8, "out → in", 114, 54);
        } else if (menu.kind == PowerMachines.Kind.WHISKY_BARREL) {
            chemicalButton(9, Component.translatable("gui.ic2.power_seal").getString(), 8, 76);
            chemicalButton(10, Component.translatable("gui.ic2.power_open").getString(), 88, 80);
        }
        if (menu.kind != PowerMachines.Kind.TRANSFORMER) return;
        String[] names = {"÷2", "×2", "-1", "+1", "-", "+"};
        for (int i = 0; i < names.length; i++) {
            final int button = i;
            addRenderableWidget(Button.builder(Component.literal(names[i]), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button))
                .bounds(leftPos + 8 + i * 27, topPos + 74, 25, 20).build());
        }
    }
    private void chemicalButton(int id, String label, int x, int width) {
        addRenderableWidget(Button.builder(Component.literal(label), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
            .bounds(leftPos + x, topPos + 74, width, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.render(graphics, mouseX, mouseY, tick); renderTooltip(graphics, mouseX, mouseY);
        if (!menu.plasmaPanel() && layout != null && hoveredSlot == null && mouseX >= leftPos + 8 && mouseX < leftPos + 168
            && mouseY >= topPos + 14 && mouseY < topPos + layout.inventoryY() - 10)
            graphics.renderComponentTooltip(font, details, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        if (menu.plasmaPanel()) {
            RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
            graphics.fill(leftPos + 7, topPos + 19, leftPos + 106, topPos + 118, 0xff252d37);
            graphics.fill(leftPos + 110, topPos + 19, leftPos + 225, topPos + 118, 0xffc5c1b2);
            graphics.fill(leftPos + 110, topPos + 19, leftPos + 225, topPos + 21, 0xff9153a7);
            drawPlasmaMetrics(graphics, false);
            for (var slot : menu.slots) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            return;
        }
        if (menu.steamPanel()) {
            boolean boiler = menu.kind == PowerMachines.Kind.BOILER;
            RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
            graphics.fill(leftPos + 7, topPos + 18, leftPos + 169, topPos + 33, 0xff252d37);
            graphics.fill(leftPos + 7, topPos + 37, leftPos + (boiler ? 107 : 169), topPos + 111, 0xff252d37);
            if (boiler) graphics.fill(leftPos + 111, topPos + 37, leftPos + 169, topPos + 111, 0xff737e8b);
            drawSteamMetrics(graphics, boiler, false);
            for (var slot : menu.slots) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            return;
        }
        if (menu.generatorPanel()) {
            RemasteredGui.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
            graphics.fill(leftPos + 7, topPos + 18, leftPos + 169, topPos + 35, 0xff252d37);
            RemasteredGui.lightning(graphics, leftPos + 9, topPos + 20);
            RemasteredGui.bar(graphics, leftPos + 23, topPos + 30, 142, 3, menu.value(0), menu.value(12), RemasteredGui.GREEN);
            graphics.fill(leftPos + 7, topPos + 38, leftPos + 104, topPos + 94, 0xff252d37);
            graphics.fill(leftPos + 108, topPos + 38, leftPos + 169, topPos + 94, 0xff737e8b);
            if (menu.kind == PowerMachines.Kind.LIQUID_FUEL) {
                RemasteredGui.bar(graphics, leftPos + 11, topPos + 86, 89, 4, menu.value(14), 16000, RemasteredGui.ORANGE);
            }
            for (var slot : menu.slots) RemasteredGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            return;
        }
        if (layout != null) {
            var texture = ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, layout.texture());
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            switch (menu.kind) {
                case PLASMAFIER -> gauge(graphics, 82, 24, 12, 41, menu.value(6), 10000, 0xffbd57ff);
                case LIQUID_FUEL -> gauge(graphics, 48, 15, 16, 58, menu.value(14), 16000, 0xff91a847);
                case BOILER -> {
                    gauge(graphics, 30, 15, 16, 58, menu.value(2), 16000, 0xff3c73cf);
                    gauge(graphics, 152, 15, 16, 58, menu.value(3), 128000, 0xffd8d8df);
                    gauge(graphics, 126, 18, 8, 52, menu.value(1), 24000, 0xffe36a30);
                }
                case FUSION -> {
                    gauge(graphics, 8, 18, 16, 58, Math.max(0, menu.value(4) - 10000), 512000, 0xffff7518);
                    gauge(graphics, 152, 19, 16, 58, menu.value(1), 48000, 0xffed3c1b);
                }
                default -> { }
            }
            return;
        }
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
    }
    private void gauge(GuiGraphics graphics, int x, int y, int width, int height, int value, int capacity, int color) {
        int filled = (int) (Math.clamp(value / (double) capacity, 0, 1) * height);
        graphics.fill(leftPos + x, topPos + y + height - filled, leftPos + x + width, topPos + y + height, color);
    }
    private void line(GuiGraphics graphics, int row, String key, Object... args) {
        var text = Component.translatable("gui.ic2.power_" + key, args);
        if (layout != null) details.add(text);
        else graphics.drawString(font, text, 8, 20 + row * 12, 0x404040, false);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        details.clear();
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        if (menu.plasmaPanel()) {
            drawPlasmaMetrics(graphics, true);
            graphics.drawString(font, playerInventoryTitle, 36, 121, 0x404040, false);
            return;
        }
        if (menu.steamPanel()) {
            boolean formed = menu.value(10) == 1;
            graphics.drawString(font, Component.translatable(formed ? "gui.ic2.power_formed" : "gui.ic2.power_incomplete"),
                11, 21, formed ? RemasteredGui.GREEN : RemasteredGui.ORANGE, false);
            drawSteamMetrics(graphics, menu.kind == PowerMachines.Kind.BOILER, true);
            graphics.drawString(font, playerInventoryTitle, 8, 113, 0x404040, false);
            return;
        }
        if (menu.kind == PowerMachines.Kind.GASIFIER) line(graphics, 0, "solid_fuel", menu.value(18));
        else if (menu.kind == PowerMachines.Kind.WHISKY_BARREL) line(graphics, 0, "barrel_state_" + menu.value(20));
        else if (menu.kind.multiblock()) line(graphics, 0, menu.value(10) == 1 ? "formed" : "incomplete");
        else line(graphics, 0, menu.kind.flux() ? "fe" : "eu", menu.value(0), menu.value(12));
        switch (menu.kind) {
            case TRANSFORMER -> { line(graphics, 1, "packet", menu.value(8), menu.value(9)); line(graphics, 2, "transformer_faces"); line(graphics, 3, "buffer_guard"); }
            case FLUX_LV, FLUX_MV, FLUX_HV -> { line(graphics, 1, "flux_rate", menu.kind.input); line(graphics, 2, "flux_faces"); line(graphics, 3, "flux_item"); }
            case PLASMAFIER -> { line(graphics, 1, "plasma", menu.value(6)); line(graphics, 2, "reserve", menu.value(7)); line(graphics, 5, "plasma_slots"); }
            case FUSION -> { line(graphics, 1, "heat", menu.value(1)); line(graphics, 2, "feed", menu.value(5)); line(graphics, 3, "lava", menu.value(4)); line(graphics, 5, "fusion_slots"); }
            case TUNNEL -> { line(graphics, 1, "steam", menu.value(3)); line(graphics, 2, "water", menu.value(2)); line(graphics, 3, "eu", menu.value(0), menu.value(12)); }
            case BOILER -> { line(graphics, 1, "heat", menu.value(1)); line(graphics, 2, "steam", menu.value(3)); line(graphics, 3, "water", menu.value(2)); line(graphics, 4, "fuel", menu.value(11)); line(graphics, 5, "boiler_slots"); }
            case WINDMILL -> {
                line(graphics, 1, "wind_speed", menu.value(13)); line(graphics, 2, "wind_air", menu.value(14) / 10);
                line(graphics, 3, "generator_output", String.format(java.util.Locale.ROOT, "%.2f", menu.value(15) / 100.0));
                if (menu.value(16) >= 0) line(graphics, 4, "rotor_life", menu.value(16));
                line(graphics, 5, "rotor_slot");
            }
            case LIQUID_FUEL -> {
                line(graphics, 1, "liquid_tank", menu.value(14)); line(graphics, 2, "fuel_remaining", menu.value(13));
                line(graphics, 3, "generator_output", String.format(java.util.Locale.ROOT, "%.2f", menu.value(15) / 100.0));
                line(graphics, 5, "liquid_slots");
            }
            case GASIFIER, ELECTRIC_GASIFIER -> {
                line(graphics, 1, "gas_tank", menu.value(14), menu.kind == PowerMachines.Kind.GASIFIER ? 9000 : 18000);
                line(graphics, 2, "chemical_progress", menu.value(17) == 0 ? 0 : menu.value(13) * 100 / menu.value(17));
                line(graphics, 4, "gasifier_slots");
            }
            case REFINERY -> {
                line(graphics, 1, "chemical_progress", menu.value(17) == 0 ? 0 : menu.value(13) * 100 / menu.value(17));
                line(graphics, 2, "refinery_inputs", menu.value(14), menu.value(15));
                line(graphics, 3, "refinery_output", menu.value(16));
            }
            case WHISKY_BARREL -> {
                line(graphics, 1, "barrel_contents", menu.value(14), menu.value(15));
                line(graphics, 2, "barrel_stage", menu.value(19));
                line(graphics, 3, "chemical_progress", menu.value(17) == 0 ? 0 : (int) (menu.value(13) * 100L / menu.value(17)));
            }
        }
        if (menu.generatorPanel()) {
            graphics.drawString(font, Component.translatable("gui.ic2.power_eu", menu.value(0), menu.value(12)),
                23, 20, RemasteredGui.TEXT, false);
            int y = 41;
            int bottom = menu.kind == PowerMachines.Kind.LIQUID_FUEL ? 84 : 93;
            for (Component metric : details.subList(1, details.size() - 1)) {
                for (var text : font.split(metric, 89)) {
                    if (y + font.lineHeight > bottom) break;
                    graphics.drawString(font, text, 11, y, RemasteredGui.TEXT, false);
                    y += font.lineHeight;
                }
            }
            graphics.drawString(font, playerInventoryTitle, 8, 96, 0x404040, false);
            return;
        }
        graphics.drawString(font, playerInventoryTitle, 8, layout == null ? 122 : layout.inventoryY() - 10, 0x404040, false);
    }

    private void drawPlasmaMetrics(GuiGraphics graphics, boolean labels) {
        if (menu.kind == PowerMachines.Kind.FUSION) {
            boolean formed = menu.value(10) == 1;
            if (labels) {
                Component state = Component.translatable(formed ? "gui.ic2.power_formed" : "gui.ic2.power_incomplete");
                int y = 24;
                for (var line : font.split(state, 90)) {
                    if (y >= 42) break;
                    graphics.drawString(font, line, 11, y, formed ? RemasteredGui.GREEN : RemasteredGui.ORANGE, false);
                    y += 9;
                }
            }
            steamMetric(graphics, labels, 48, 90, "heat", 1, 48000, RemasteredGui.ORANGE);
            steamMetric(graphics, labels, 68, 90, "lava", 4, 522000, 0xffed6532);
            if (labels) steamMetric(graphics, true, 90, 90, "feed", 5, 0, RemasteredGui.TEXT);
        } else {
            steamMetric(graphics, labels, 31, 90, "plasma", 6, 10000, 0xffbd57ff);
            steamMetric(graphics, labels, 57, 90, "reserve", 7, 149, 0xffbd57ff);
            steamMetric(graphics, labels, 83, 90, "eu", 0, menu.value(12), RemasteredGui.GREEN);
        }
    }

    private void drawSteamMetrics(GuiGraphics graphics, boolean boiler, boolean labels) {
        if (boiler) steamMetric(graphics, labels, 41, 92, "heat", 1, 24000, RemasteredGui.ORANGE);
        int width = boiler ? 92 : 154;
        steamMetric(graphics, labels, boiler ? 57 : 43, width, "water", 2, boiler ? 16000 : 2000, 0xff439fdf);
        steamMetric(graphics, labels, boiler ? 73 : 64, width, "steam", 3, boiler ? 128000 : 16000, 0xff53d9e8);
        if (boiler) {
            if (labels) steamMetric(graphics, true, 91, width, "fuel", 11, 0, RemasteredGui.ORANGE);
        } else {
            steamMetric(graphics, labels, 85, width, "eu", 0, menu.value(12), RemasteredGui.GREEN);
        }
    }

    private void steamMetric(GuiGraphics graphics, boolean labels, int y, int width, String key, int index, int max, int color) {
        int value = menu.value(index);
        if (labels) {
            Component text = Component.translatable("gui.ic2.power_" + key, value, max);
            graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), 11, y, RemasteredGui.TEXT, false);
        } else if (max > 0) {
            RemasteredGui.bar(graphics, leftPos + 11, topPos + y + 10, width, 3, value, max, color);
        }
    }
}
