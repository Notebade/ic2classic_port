package example;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Runs equipment behavior fixtures in an actual integrated client world. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class EquipmentClientChecks {
    @FunctionalInterface private interface Check { void run(ServerLevel level) throws Exception; }
    private record Case(String name, Check check) {}
    private static final List<Case> CASES = List.of(
        new Case("mining tools", MiningToolChecks::run),
        new Case("portable energy", PortableEnergyChecks::run),
        new Case("equipment", EquipmentChecks::run),
        new Case("utility tools", UtilityToolChecks::run),
        new Case("toolboxes", ToolboxChecks::run),
        new Case("equipment power", EquipmentPowerChecks::run),
        new Case("solar armor", SolarArmorChecks::run),
        new Case("modular armor", ModularArmorChecks::run),
        new Case("movement modules", MovementModuleChecks::run),
        new Case("fall armor", FallArmorChecks::run),
        new Case("electrical armor", ElectricArmorChecks::run));
    private static int index;
    private static int ticks;
    private static boolean preparing;
    private static volatile boolean running;
    private static volatile Throwable failure;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.equipment.verify")) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null || client.isPaused() || ++ticks < 60) return;
        if (failure != null) throw new AssertionError("Equipment client fixture failed", failure);
        if (running) return;
        if (index == CASES.size()) {
            com.mojang.logging.LogUtils.getLogger().info("IC2 equipment integrated-client fixtures passed: {}", index);
            client.stop();
            return;
        }
        if (!preparing) {
            preparing = true;
            ticks = 0;
            var id = client.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                double coordinate = index < 6 ? 480 : index < 10 ? 600 : 790;
                player.teleportTo(coordinate, 200, coordinate);
                player.setNoGravity(true);
            });
            return;
        }
        preparing = false;
        var test = CASES.get(index++);
        running = true;
        server.execute(() -> {
            try {
                test.check.run(server.overworld());
                com.mojang.logging.LogUtils.getLogger().info("IC2 equipment client fixture passed: {}", test.name);
            } catch (Throwable error) {
                failure = error;
            } finally {
                running = false;
            }
        });
    }
}
