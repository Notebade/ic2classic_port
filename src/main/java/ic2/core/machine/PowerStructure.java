package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;

/** Local coordinates: x is right, y is down, z is inward. */
final class PowerStructure {
    enum Part { AIR, STONE, GLASS, BOILER, TUNNEL, TUNNEL_VALVE }
    record Cell(int x, int y, int z, Part part) {}
    private static final List<Cell> FUSION = createFusion();
    private static final List<Cell> BOILER = createBoiler();
    private static final List<Cell> TUNNEL = createTunnel();
    static List<Cell> fusion() { return FUSION; }
    static List<Cell> boiler() { return BOILER; }
    static List<Cell> tunnel() { return TUNNEL; }
    private static List<Cell> createFusion() {
        var cells = new ArrayList<Cell>();
        for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            int edges = edge(x, 4) + edge(y, 4) + edge(z, 4);
            cells.add(new Cell(x, y, z, edges == 0 ? Part.AIR : edges >= 2 || y == 4 ? Part.STONE : Part.GLASS));
        }
        return List.copyOf(cells);
    }
    private static List<Cell> createBoiler() {
        var cells = new ArrayList<Cell>();
        for (int i = 1; i < 8; i++) cells.add(new Cell(i % 2, i / 4, i / 2 % 2, Part.BOILER));
        return List.copyOf(cells);
    }
    private static List<Cell> createTunnel() {
        var cells = new ArrayList<Cell>();
        for (int z = 0; z < 5; z++) for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0 && z == 0) continue;
            int edges = edge(x + 1, 2) + edge(y + 1, 2) + edge(z, 4);
            cells.add(new Cell(x, y, z, edges == 0 ? Part.AIR : edges == 1 ? Part.TUNNEL_VALVE : Part.TUNNEL));
        }
        return List.copyOf(cells);
    }
    private static int edge(int value, int max) { return value == 0 || value == max ? 1 : 0; }
}
