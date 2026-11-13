package ic2.core.machine;

import ic2.core.IC2;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SubscribeEvent
    public static void registerSteamTexture(RegisterClientExtensionsEvent event) {
        var texture = ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "block/fluids/steam_still");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return texture; }
            @Override public ResourceLocation getFlowingTexture() { return texture; }
        }, SteamFluids.type());
        for (var fuel : GeneratorFluids.Fuel.values()) {
            var sprite = ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "block/fluids/" + fuel.texture);
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return sprite; }
                @Override public ResourceLocation getFlowingTexture() { return sprite; }
                @Override public int getTintColor() { return fuel.tint; }
            }, fuel.type.get());
        }
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "block/fluids/base_fluid_still"); }
            @Override public ResourceLocation getFlowingTexture() { return getStillTexture(); }
            @Override public int getTintColor() { return 0xffbf9f18; }
        }, ChemistryMaterials.WHISKY_TYPE.get());
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(Machines.COMPARING_CABLE_MENU.get(), ComparingCableScreen::new);
        event.register(Machines.STONE_MACERATOR_MENU.get(), StoneMaceratorScreen::new);
        event.register(Machines.MENU.get(), MachineScreen::new);
        event.register(Machines.BATBOX_MENU.get(), MachineScreen::new);
        event.register(Machines.MFE_MENU.get(), MachineScreen::new);
        event.register(Machines.MFSU_MENU.get(), MachineScreen::new);
        event.register(Machines.ESU_MENU.get(), MachineScreen::new);
        event.register(Machines.ISU_MENU.get(), MachineScreen::new);
        event.register(Machines.PESU_MENU.get(), MachineScreen::new);
        event.register(Machines.ALLOY_SMELTER_MENU.get(), MachineScreen::new);
        event.register(Machines.RARE_EARTH_MENU.get(), MachineScreen::new);
        event.register(Machines.URANIUM_ENRICHER_MENU.get(), MachineScreen::new);
        event.register(Machines.CANNER_MENU.get(), MachineScreen::new);
        event.register(Machines.VACUUM_CANNER_MENU.get(), MachineScreen::new);
        event.register(Machines.ELECTROLYZER_MENU.get(), MachineScreen::new);
        event.register(Machines.CHARGED_ELECTROLYZER_MENU.get(), MachineScreen::new);
        event.register(Machines.FABRICATOR_MENU.get(), MachineScreen::new);
        event.register(Machines.GEOTHERMAL_MENU.get(), MachineScreen::new);
        event.register(Machines.THERMAL_GENERATOR_MENU.get(), MachineScreen::new);
        event.register(Machines.NUCLEAR_REACTOR_MENU.get(), ReactorScreen::new);
        event.register(Machines.STEAM_REACTOR_MENU.get(), ReactorScreen::new);
        event.register(Machines.HEAT_PUMP_MENU.get(), HeatPumpScreen::new);
        event.register(Machines.REACTOR_PLANNER_MENU.get(), ReactorPlannerScreen::new);
        event.register(Machines.STEAM_TURBINE_MENU.get(), MachineScreen::new);
        event.register(Machines.WIND_TURBINE_MENU.get(), MachineScreen::new);
        event.register(Machines.WATER_MILL_MENU.get(), MachineScreen::new);
        event.register(Machines.MV_WATER_MILL_MENU.get(), MachineScreen::new);
        event.register(Machines.HV_WATER_MILL_MENU.get(), MachineScreen::new);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
            graphics.fill(x, y, x + 17, y + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.isSteamTurbine()) {
            graphics.drawString(font, Component.translatable("gui.ic2.steam_turbine_status", menu.energy(), menu.work()),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.steam_turbine_tank", menu.fluid()),
                8, 33, 0x404040, false);
            return;
        }
        if (menu.isWindTurbine()) {
            String output = String.format(Locale.ROOT, "%.2f", menu.work() / (double) WindProduction.UNITS_PER_EU);
            graphics.drawString(font, Component.translatable("gui.ic2.wind_status", output, menu.energy()),
                8, 21, 0x404040, false);
            Component charge = Component.translatable("gui.ic2.charge");
            graphics.drawString(font, charge, 88 - font.width(charge) / 2, 61, 0x404040, false);
            return;
        }
        if (menu.isStorage()) {
            graphics.drawString(font, Component.translatable("gui.ic2.storage_energy", menu.energy(), menu.storageCapacity()),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.charge"), 43, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.discharge"), 95, 61, 0x404040, false);
            Direction direction = Direction.from3DDataValue(menu.work());
            Component output = Component.translatable("gui.ic2.storage_output",
                Component.translatable("direction.ic2." + direction.getName()));
            graphics.drawString(font, output, imageWidth - 8 - font.width(output), inventoryLabelY, 0x404040, false);
            return;
        }
        if (MachineMenu.isFluidGenerator(menu.getType())) {
            Component status;
            if (menu.isWaterMill()) {
                status = Component.translatable("gui.ic2.water_mill_status",
                    String.format(Locale.ROOT, "%.2f", menu.work() / (double) WaterMillTier.UNITS_PER_EU),
                    menu.fluid(), FluidGeneratorBlock.WATER_TANK);
            } else if (menu.isThermalGenerator()) {
                status = Component.translatable("gui.ic2.thermal_generator_status",
                    String.format(Locale.ROOT, "%.2f", menu.work() / (double) ThermalProduction.DISPLAY_UNITS_PER_EU),
                    menu.fluid(), ThermalProduction.TANK_CAPACITY);
            } else {
                status = Component.translatable("gui.ic2.geothermal_status", menu.energy(),
                    menu.fluid(), FluidGeneratorBlock.GEOTHERMAL_TANK);
            }

            graphics.drawString(font, status, 8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.input"), 9, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.output"), 69, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.charge"), 120, 61, 0x404040, false);
            return;
        }
        if (menu.isAlloySmelter() || menu.isUraniumEnricher() || MachineMenu.isCanner(menu.getType())) {
            Component status = menu.isUraniumEnricher()
                ? Component.translatable("gui.ic2.enricher_status", menu.energy(),
                    menu.work() * 100 / UraniumEnrichment.INGOT_TICKS, menu.fluid())
                : Component.translatable("gui.ic2.machine_status", menu.energy(), menu.work());
            if (menu.isVacuumCanner()) {
                status = Component.translatable("gui.ic2.vacuum_canner_status", menu.energy(),
                    menu.work() * 100 / MachineKind.VACUUM_CANNER.recipeTicks, menu.fluid() * 100 / CanningRecipes.MAX_SPEED);
            }
            graphics.drawString(font, status, 8, 21, 0x404040, false);
            Component input = Component.translatable(MachineMenu.isCanner(menu.getType()) ? "gui.ic2.container" : "gui.ic2.input");
            graphics.drawString(font, input, 32 - font.width(input) / 2, 61, 0x404040, false);
            Component catalyst = Component.translatable(menu.isUraniumEnricher() ? "gui.ic2.catalyst" : "gui.ic2.input");
            graphics.drawString(font, catalyst, 88 - font.width(catalyst) / 2, 61, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.output"), 120, 61, 0x404040, false);
            return;
        }
        if (menu.isFabricator()) {
            graphics.drawString(font, Component.translatable("gui.ic2.storage_energy", menu.energy(), MassFabrication.MATTER_COST),
                8, 21, 0x404040, false);
            graphics.drawString(font, Component.translatable("gui.ic2.amplifier", menu.work()),
                8, 30, 0x404040, false);
        } else {
            Component status;
            if (menu.isRareEarth()) {
                status = Component.translatable("gui.ic2.rare_earth_status", menu.energy(), menu.work(), menu.fluid());
            } else if (menu.isElectrolyzer()) {
                status = Component.translatable("gui.ic2.storage_energy", menu.energy(), MachineKind.ELECTROLYZER.capacity);
            } else if (menu.fluid() < 0) {
                status = Component.translatable("gui.ic2.machine_status", menu.energy(), menu.work());
            } else {
                status = Component.translatable("gui.ic2.pump_status", menu.energy(), menu.work(), menu.fluid());
            }

            graphics.drawString(font, status, 8, 21, 0x404040, false);
        }
        Component input = Component.translatable(menu.isFabricator() ? "gui.ic2.amplifier_slot"
            : menu.isElectrolyzer() ? "gui.ic2.charge" : "gui.ic2.input");
        graphics.drawString(font, input, 61 - font.width(input) / 2, 61, 0x404040, false);
        graphics.drawString(font, Component.translatable(menu.isElectrolyzer() ? "gui.ic2.discharge" : "gui.ic2.output"),
            102, 61, 0x404040, false);
    }
}
