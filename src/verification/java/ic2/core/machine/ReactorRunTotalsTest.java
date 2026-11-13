package ic2.core.machine;

public final class ReactorRunTotalsTest {

    public static void main(String[] args) {
        var run = new ReactorRunTotals();
        run.record(400, 400, 1, 20);
        run.record(800, 400, 1, 20);
        require(run.production() == 1200 && run.efficiencyPermille() == 1500, "efficiency uses full run, not final output");
        require(run.fuelTicks() == 40 && run.fuelCells() == 1, "runtime counts active fuel ticks");
        run.record(0, 0, 0, 20);
        require(run.fuelTicks() == 40 && run.production() == 1200, "cooldown does not extend fuel runtime");
        require(ReactorRunTotals.remainingTicks(100, 25) == 1500, "both reactor modes consume durability every twenty game ticks");
        System.out.println("Reactor run totals: all checks passed");
    }

    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
