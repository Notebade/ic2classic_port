package ic2.core.machine;

final class ColossalCycle {
    private ColossalCycle() {}
    static int lanes(int edge) { return edge < 2 || edge > 4 ? 0 : 1 << (edge - 1); }
    static boolean fluidsReady(int water, int steam, int activeLanes) { return water >= activeLanes * 100 && 16000 - steam >= activeLanes * 25; }
    static int progress(int current, boolean running, int step) { return running ? Math.min(6000, current + step) : Math.max(0, current - step); }
}
