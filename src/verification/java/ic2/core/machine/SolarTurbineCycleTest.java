package ic2.core.machine;

public final class SolarTurbineCycleTest {
    public static void main(String[] args) {
        var cold = new SolarTurbineCycle.State(0, 0, 0, 0);
        check(cold.step(0, true).next().heat() == 2, "sun warms an empty turbine");
        var filled = cold.fill(25000);
        check(filled.accepted() == 20000 && filled.next().water() == 40000, "original fill doubles water units and caps the tank");
        var hot = new SolarTurbineCycle.State(24000, 40000, 0, 0);
        var step = hot.step(0, true);
        check(step.energy() == 32 && step.next().water() == 39996, "full heat and tank generate 32 EU using four water units");
        var full = hot.step(100, true);
        check(full.energy() == 100 && full.next().water() == 39996, "full buffer still consumes water");
        check(hot.step(99, true).energy() == 131, "original final generation may overshoot the nominal 100 EU buffer");
        var night = hot.step(0, false);
        check(night.next().heat() == 23999 && night.energy() > 0, "stored heat keeps generating at night");
        var fractional = new SolarTurbineCycle.State(100, 1000, 0, 0);
        int total = 0;
        for (int i = 0; i < 200; i++) {
            var next = fractional.step(0, true);
            fractional = next.next();
            total += next.energy();
        }
        check(total > 0 && fractional.energyRemainder() > 0, "fractional EU accumulates across ticks");
        check(new SolarTurbineCycle.State(1, 40000, 0, 0).step(0, false).energy() == 0, "zero heat produces nothing");
        System.out.println("Solar turbine cycle: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
