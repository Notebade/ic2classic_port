package ic2.core.machine;

final class ReactorBlastMath {

    private ReactorBlastMath() {
    }

    static double absorption(double resistance) {
        return (resistance * 0.4 + 4) * 0.3;
    }

    static double radius(double power) {
        return power / 0.4;
    }

    static double force(double power, double distance) {
        return power / 2 / (Math.pow(distance, 0.8) + 1);
    }

    static float damage(double power, double distance) {
        return (float)Math.pow(force(power, distance) * 3, 2);
    }
}
