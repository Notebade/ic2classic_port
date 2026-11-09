package ic2.core.item;

import net.minecraft.world.item.Item;

public class ReactorHeatItem extends Item {
    public ReactorHeatItem(Properties properties, int capacity) {
        super(properties.durability(capacity).setNoRepair());
    }

    public static final class Vent extends ReactorHeatItem {
        private final int selfCooling;
        private final int hullCooling;
        private final boolean electric;

        public Vent(Properties properties, int capacity, int selfCooling, int hullCooling, boolean electric) {
            super(properties, capacity);
            this.selfCooling = selfCooling;
            this.hullCooling = hullCooling;
            this.electric = electric;
        }

        public int selfCooling(boolean powered) {
            return electric && !powered ? selfCooling / 2 : selfCooling;
        }

        public int hullCooling(boolean powered) {
            return electric && !powered ? hullCooling / 2 : hullCooling;
        }

        public int powerCostTenths() {
            return electric ? selfCooling : 0;
        }
    }
}
