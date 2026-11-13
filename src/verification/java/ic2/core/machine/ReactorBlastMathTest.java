package ic2.core.machine;

public final class ReactorBlastMathTest {

    public static void main(String[] args) {
        if (Math.abs(ReactorBlastMath.absorption(6) - 1.92) > 1.0E-6) throw new AssertionError("original stone absorption");
        if (ReactorBlastMath.damage(10, 0) != 225) throw new AssertionError("original damage at epicentre");
        if (ReactorBlastMath.damage(10, 8) >= ReactorBlastMath.damage(10, 4)) throw new AssertionError("distance attenuates damage");
        if (Math.abs(ReactorBlastMath.radius(12) - 30) > 1.0E-6) throw new AssertionError("original 0.4 ray attenuation radius");
        System.out.println("Reactor blast math: all checks passed");
    }
}
