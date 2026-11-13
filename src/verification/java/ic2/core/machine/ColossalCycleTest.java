package ic2.core.machine;

public final class ColossalCycleTest {
    public static void main(String[] args) {
        if (ColossalCycle.lanes(2) != 2 || ColossalCycle.lanes(3) != 4 || ColossalCycle.lanes(4) != 8)
            throw new AssertionError("solid cubes unlock 2, 4 and 8 lanes");
        if (!ColossalCycle.fluidsReady(200, 15950, 2) || ColossalCycle.fluidsReady(199, 0, 2)
                || ColossalCycle.fluidsReady(200, 15951, 2)) throw new AssertionError("per-lane water and steam reserve");
        if (ColossalCycle.progress(5000, true, 1000) != 6000 || ColossalCycle.progress(5000, false, 1000) != 4000)
            throw new AssertionError("running progresses and power failure reverses progress");
        System.out.println("Colossal machine cycle: all checks passed");
    }
}
