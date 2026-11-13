package ic2.core.machine;

public final class GenerationSettingsTest {

    public static void main(String[] args) {
        var sun = new SolarTurbineCycle.State(24000, 40000, 0, 0);
        if (sun.step(0, true, 2).energy() != 64) throw new AssertionError("solar multiplier");
        if (SlagGeneratorCycle.step(10, 0, 10, false, 120).energy() != 120) throw new AssertionError("slag output");
        if (SteamTunnelCycle.step(0, 4096, 0, 0, 1024).energy() != 4096) throw new AssertionError("tunnel output");
        var blocked = SteamTunnelCycle.step(9500, 4096, 0, 0, 1024);
        if (blocked.steam() != 4096 || blocked.energy() != 9500) throw new AssertionError("configured tunnel must preserve steam when full");
        var wind = new WindmillCycle.State(1, 256000).step(1, true, true, 0, 512);
        if (wind.energy() != 1024) throw new AssertionError("wind output");
        System.out.println("Generation settings: all checks passed");
    }
}
