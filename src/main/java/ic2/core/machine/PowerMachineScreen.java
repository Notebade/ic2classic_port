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
    PowerMachineScreen(PowerMachineMenu menu, Inventory player, Component title) { super(menu, player, title); imageHeight = 214; }
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        for (var type : PowerMachines.SCREENS.values()) event.register(type.get(), PowerMachineScreen::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(BoilerFuel.CAN.get(), ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "fueled"),
            (stack, level, entity, seed) -> BoilerFuel.amount(stack) > 0 ? 1 : 0));
    }
    @Override protected void init() {
        super.init();
        if (menu.kind != PowerMachines.Kind.TRANSFORMER) return;
        String[] names = {"÷2", "×2", "-1", "+1", "-", "+"};
        for (int i = 0; i < names.length; i++) {
            final int button = i;
            addRenderableWidget(Button.builder(Component.literal(names[i]), ignored -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button))
                .bounds(leftPos + 8 + i * 27, topPos + 74, 25, 20).build());
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.render(graphics, mouseX, mouseY, tick); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff555555);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
    }
    private void line(GuiGraphics graphics, int row, String key, Object... args) {
        graphics.drawString(font, Component.translatable("gui.ic2.power_" + key, args), 8, 20 + row * 12, 0x404040, false);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        if (menu.kind.multiblock()) line(graphics, 0, menu.value(10) == 1 ? "formed" : "incomplete");
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
        }
        graphics.drawString(font, playerInventoryTitle, 8, 122, 0x404040, false);
    }
}
