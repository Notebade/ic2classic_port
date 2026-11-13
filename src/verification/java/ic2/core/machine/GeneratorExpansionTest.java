package ic2.core.machine;

public final class GeneratorExpansionTest {
    public static void main(String[] args) {
        var wind = new WindmillCycle.State(1, 0);
        var first = wind.step(1, true, true, 0);
        require(first.energy() == 0, "original wind accumulator waits past the exact threshold");
        var second = first.next().step(1, true, true, 0);
        require(second.energy() == 512 && second.next().remainder() == 0, "two nominal wind ticks produce 512 EU");
        require(wind.step(1, false, true, 0).next().speed() == 0, "rotor removal stops generation");
        require(wind.step(0, true, false, 0).next().speed() == .999, "blocked rotor coasts down slowly");
        require(wind.step(3, true, true, 4000).next().remainder() == 0, "full wind buffer cannot bank production");
        require(new WindmillCycle.State(0, 0).step(3, true, true, 0).next().speed() == .01, "acceleration limited per tick");
        require(WindmillCycle.freeRatio(2106, 2106, 2) == .5, "nearby mills share connected air");
        var north = WindmillCycle.area(0, -1, 8);
        require(north.minX() == -4 && north.maxX() == 4 && north.minZ() == -20 && north.maxZ() == 5, "wind bounds extend 20 ahead, 5 behind");
        require(WindmillCycle.area(1, 0, 10).maxX() == 20, "area rotates east");
        var fuel = new LiquidFuelCycle.State(2, 128);
        require(fuel.step(10000).next().remaining() == 2, "full fuel buffer pauses burning");
        var burn = fuel.step(9872);
        require(burn.energy() == 10000 && burn.next().remaining() == 1, "exactly fitting fuel pulse is conserved");
        require(fuel.step(9873).next().remaining() == 2, "partial space does not waste fuel");
        require(LiquidFuelCycle.fuelTicks(250, 1) == 0 && LiquidFuelCycle.fuelTicks(250, 4) == 1, "sub-tick liquid fuel is not consumed");
        require(LiquidFuelCycle.fuelTicks(2000, 1000) * 128 == 256000, "biofuel bucket energy");
        require(LiquidFuelCycle.fuelTicks(24000, 1000) * 50 == 1200000, "alcohol bucket energy");
        require(LiquidFuelCycle.packet(10) == 32 && LiquidFuelCycle.packet(50) == 128, "fuel output packet tiers");
        System.out.println("Windmill and liquid-fuel cycles: all checks passed");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
