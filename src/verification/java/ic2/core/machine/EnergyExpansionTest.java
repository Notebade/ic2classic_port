package ic2.core.machine;

public final class EnergyExpansionTest {
    public static void main(String[] args) {
        var plasma = new PlasmaCycle.State(20_480, 0, 0);
        plasma = plasma.supplyMatter().step();
        require(plasma.energy() == 10_240 && plasma.reserve() == 99 && plasma.plasma() == 1, "UU and EU become plasma");
        require(new PlasmaCycle.State(10_239, 3, 9999).step().plasma() == 9999, "insufficient EU preserves resources");
        require(new PlasmaCycle.State(20_480, 3, 10000).step().energy() == 20_480, "full plasma buffer stops consumption");
        require(new PlasmaCycle.State(0, 50, 0).supplyMatter().reserve() == 50, "reserve threshold");
        var steam = SteamTunnelCycle.step(0, 4096, 0, 0);
        require(steam.energy() == 2048 && steam.steam() == 0 && steam.water() == 25 && steam.remainder() == 60, "steam batch and fractional water");
        steam = SteamTunnelCycle.step(steam.energy(), 4096, steam.water(), steam.remainder());
        require(steam.water() == 51 && steam.remainder() == 20, "water remainder carries across batches");
        require(SteamTunnelCycle.step(10000, 4096, 0, 0).steam() == 4096, "full EU stops steam consumption");
        require(EnergyConversion.acceptedEu(159999, 160000, 32) == 0, "FE remainder cannot create EU room");
        require(EnergyConversion.acceptedEu(159995, 160000, 32) == 1, "accept only whole EU");
        require(EnergyConversion.buffer(32768) == 1_048_576, "transformer capacity follows packet setting");
        var fusion = new FusionCycle.State(47990, 1000, 0, 0);
        fusion = fusion.step(5, 1, 1, 1).next();
        require(fusion.heat() == 48000 && fusion.lava() == 0, "warmup does not produce lava prematurely");
        var production = fusion.step(5, 1, 1, 1);
        require(production.next().lava() == 20 && production.next().material() == 980 && production.damage() == 1, "bootstrap lava consumes feed and wears rods");
        require(production.next().heat() < 48000, "bootstrap reserve costs heat");
        var full = new FusionCycle.State(48000, 1000, 522000, 0).step(25, 24, 6, 1.5);
        require(full.next().material() == 1000 && full.next().lava() == 522000, "full lava tank preserves material");
        require(new FusionCycle.State(48000, 1000, 10000, 0).step(1, 1, 1, 1).next().lava() == 10006, "low-heat rods still produce after bootstrap");
        require(new FusionCycle.State(48000, 1000, 521999, 6000).step(25, 24, 6, 1.5).next().remainder() == 5999, "pending production is not amplified against a nearly full tank");
        require(PowerStructure.fusion().size() == 124, "fusion volume excludes controller");
        require(PowerStructure.fusion().stream().filter(cell -> cell.part() == PowerStructure.Part.AIR).count() == 27, "fusion hollow 3x3x3");
        require(PowerStructure.fusion().stream().filter(cell -> cell.part() == PowerStructure.Part.STONE).count() == 52, "fusion stone edges and floor");
        require(PowerStructure.tunnel().size() == 44, "tunnel volume excludes controller");
        require(PowerStructure.tunnel().stream().filter(cell -> cell.part() == PowerStructure.Part.AIR).count() == 3, "tunnel air channel");
        require(PowerStructure.boiler().size() == 7, "seven boiler casings");
        var boiler = BoilerCycle.evaporate(24000, 100, 0, 0);
        require(boiler.water() == 94 && boiler.steam() == 960 && boiler.remainder() == 400, "hot boiler water/steam conversion");
        boiler = BoilerCycle.evaporate(24000, boiler.water(), boiler.steam(), boiler.remainder());
        boiler = BoilerCycle.evaporate(24000, boiler.water(), boiler.steam(), boiler.remainder());
        require(boiler.water() == 81 && boiler.steam() == 3040 && boiler.remainder() == 200, "fractional boiler production is conserved");
        require(BoilerCycle.evaporate(3000, 100, 0, 999).water() == 100, "cold boiler cannot consume water");
        require(BoilerCycle.evaporate(24000, 7, 0, 999).water() == 7, "boiler requires eight mB of water");
        require(BoilerCycle.evaporate(24000, 100, 128000, 999).water() == 100, "full steam buffer preserves water");
        System.out.println("Energy expansion checks passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
