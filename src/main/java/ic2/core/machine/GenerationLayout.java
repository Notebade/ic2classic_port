package ic2.core.machine;

import java.util.List;

/**
 * Slot coordinates and backgrounds taken from the original containers.
 */
record GenerationLayout(String texture, int height, int inventoryY, List<Point> slots) {
    record Point(int x, int y) {}

    static GenerationLayout forId(String id) {
        return switch (id) {
        case "solar_turbine" -> layout("generators/gui_solar_turbine", 166, 84, 62, 17, 122, 17, 122, 53);
        case "slag_generator" -> layout("generators/gui_slag_generator", 166, 84, 65, 17, 65, 53, 131, 36);
        case "windmill" -> layout("generators/gui_windmill", 166, 84, 80, 26);
        case "liquid_fuel_generator" -> layout("generators/gui_liquid_generator", 166, 84, 134, 35, 26, 17, 26, 53);
        case "plasmafier" -> layout("machines/ev/gui_plasmafier", 166, 84, 44, 35, 116, 23, 116, 45);
        case "fuel_boiler" -> layout("generators/gui_fuel_boiler", 166, 84, 8, 19, 8, 53, 80, 53, 102, 53, 58, 53, 58, 35);
        case "fusion_reactor" -> layout("machines/luv/gui_fusion_reactor", 173, 91, 58, 18, 102, 62, 58, 40, 102, 40, 80, 18, 80, 62, 80, 40);
        default -> null;
        };
    }

    private static GenerationLayout layout(String path, int height, int inventoryY, int... coordinates) {
        var points = new java.util.ArrayList<Point>();
        for (int i = 0; i < coordinates.length; i += 2) points.add(new Point(coordinates[i], coordinates[i + 1]));
        return new GenerationLayout("textures/gui_sprites/blocks/" + path + ".png", height, inventoryY, List.copyOf(points));
    }
}
