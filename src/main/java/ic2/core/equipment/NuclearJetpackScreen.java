package ic2.core.equipment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import ic2.core.machine.RemasteredGui;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NuclearJetpackScreen extends AbstractContainerScreen<NuclearJetpackMenu> {
    NuclearJetpackScreen(NuclearJetpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 222; inventoryLabelY = 128;
    }
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) { event.register(NuclearJetpackMenu.TYPE.get(), NuclearJetpackScreen::new); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { RemasteredGui.container(graphics, leftPos, topPos, imageWidth, imageHeight, menu.slots, inventoryLabelY); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
}
