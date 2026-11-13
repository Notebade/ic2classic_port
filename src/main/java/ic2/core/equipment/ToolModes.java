package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class ToolModes {
    private ToolModes() {}
    static String read(ConfigurableTool tool, ItemStack stack) {
        String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("ic2_tool_mode");
        return tool.modes().contains(value) ? value : tool.modes().getFirst();
    }
    static boolean write(Item item, ConfigurableTool tool, ItemStack stack, String value) {
        if (!stack.is(item) || !tool.modes().contains(value)) return false;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putString("ic2_tool_mode", value); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return true;
    }
    static void next(Item item, ConfigurableTool tool, ItemStack stack) {
        write(item, tool, stack, tool.modes().get((tool.modes().indexOf(read(tool, stack)) + 1) % tool.modes().size()));
    }
}
