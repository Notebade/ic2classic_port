package ic2.core.machine;

import ic2.core.item.ReactorHeatItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class SteamVentCycle {
    private static final int STEAM_PER_WATER = 160;
    private static final int HEAT_PER_WATER = 20;

    private SteamVentCycle() {}

    static int boil(SteamReactorTanks tanks, ItemStack stack, ReactorHeatItem.SteamVent vent, boolean passiveCooling) {
        int heat = stack.getDamageValue();
        CompoundTag data = data(stack);
        double water = data.getDouble("vent_water");
        if (heat < 100 || water <= 0) {
            if (passiveCooling) {
                stack.setDamageValue(Math.max(0, heat - 1));
            }
            return 0;
        }
        double rate = vent.selfCooling() * heat / ((stack.getMaxDamage() - 100.0) * HEAT_PER_WATER);
        int steam = (int) (Math.min(water, rate) * STEAM_PER_WATER);
        if (!tanks.produce(steam)) {
            return 0;
        }
        double consumed = (double) steam / STEAM_PER_WATER;
        double cooling = data.getDouble("vent_cooling") + consumed * HEAT_PER_WATER;
        int cooled = Math.min(heat, (int) cooling);
        stack.setDamageValue(heat - cooled);
        data.putDouble("vent_water", water - consumed);
        data.putDouble("vent_cooling", cooling - cooled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return steam;
    }

    static void refill(SteamReactorTanks tanks, ItemStack stack, ReactorHeatItem.SteamVent vent) {
        CompoundTag data = data(stack);
        double water = data.getDouble("vent_water");
        if (water <= vent.selfCooling() - 1) {
            int received = tanks.refillVent(vent.selfCooling() - (int) water);
            if (received > 0) {
                data.putDouble("vent_water", water + received);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
