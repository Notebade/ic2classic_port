package ic2.core.machine;

import java.util.Random;

/** Original default wind: random signed targets, a random interval and 0.1 speed change per tick. */
final class WindField {
    static final int CHANGE_INTERVAL = 6000;
    private WindField() {}
    static State initial(long seed) {
        var random = new Random(seed);
        double initial = random.nextDouble() * 200 - 100;
        return new State(initial, initial, 0, random.nextLong());
    }
    record State(double current, double target, int remaining, long seed) {
        State {
            if (!Double.isFinite(current) || !Double.isFinite(target) || Math.abs(current) > 100 || Math.abs(target) > 100
                    || remaining < 0 || remaining >= CHANGE_INTERVAL) throw new IllegalArgumentException("Invalid wind state");
        }
        State step() {
            double nextTarget = target; int countdown = remaining; long nextSeed = seed;
            if (countdown == 0) {
                var random = new Random(seed);
                countdown = random.nextInt(CHANGE_INTERVAL);
                nextTarget = random.nextDouble() * 200 - 100;
                nextSeed = random.nextLong();
            } else countdown--;
            return new State(current + Math.clamp(nextTarget - current, -.1, .1), nextTarget, countdown, nextSeed);
        }
    }
}
