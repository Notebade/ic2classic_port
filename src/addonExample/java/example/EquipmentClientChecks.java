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
    private record Case(String name, double coordinate, Check check) {}
    private static final List<Case> CASES = cases();
    private static List<Case> cases() {
        String selection = System.getProperty("ic2.equipment.case", "all");
        if (selection.equals("sonar")) return List.of(new Case("sonar", 480, ModularArmorChecks::checkSonar));
        var all = List.of(
            new Case("mining tools", 480, MiningToolChecks::run),
            new Case("portable energy", 480, PortableEnergyChecks::run),
            new Case("equipment", 480, EquipmentChecks::run),
            new Case("utility tools", 480, UtilityToolChecks::run),
            new Case("toolboxes", 480, ToolboxChecks::run),
            new Case("equipment power", 480, EquipmentPowerChecks::run),
            new Case("solar armor", 600, SolarArmorChecks::run),
            new Case("modular armor", 600, ModularArmorChecks::run),
            new Case("movement modules", 600, MovementModuleChecks::run),
            new Case("fall armor", 600, FallArmorChecks::run),
            new Case("electrical armor", 790, ElectricArmorChecks::run));
        if (selection.equals("all")) return all;
        var selected = all.stream().filter(test -> test.name.equals(selection)).toList();
        if (selected.isEmpty()) throw new IllegalArgumentException("Unknown equipment case: " + selection);
        return selected;
    }
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
                double coordinate = CASES.get(index).coordinate;
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
