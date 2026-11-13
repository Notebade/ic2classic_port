package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import java.util.function.IntSupplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class MachineMenu extends AbstractContainerMenu {
    private static final int ENERGY_LOW = 0;
    private static final int ENERGY_HIGH = 1;
    private static final int WORK_LOW = 2;
    private static final int WORK_HIGH = 3;
    private static final int FLUID = 4;
    private static final int DATA_COUNT = 5;
    private final ContainerLevelAccess access;
    private final Block block;
    private final ContainerData data;
    private final MachineKind storageKind;
    private final int machineSlots;

    MachineMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, Machines.MENU.get());
    }

    static MachineMenu batbox(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.BATBOX_MENU.get());
    }

    static MachineMenu mfe(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.MFE_MENU.get());
    }

    static MachineMenu mfsu(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.MFSU_MENU.get());
    }
    static MachineMenu esu(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.ESU_MENU.get());
    }
    static MachineMenu isu(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.ISU_MENU.get());
    }
    static MachineMenu pesu(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.PESU_MENU.get());
    }

    static MachineMenu fabricator(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.FABRICATOR_MENU.get());
    }

    static MachineMenu geothermal(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.GEOTHERMAL_MENU.get());
    }

    static MachineMenu thermalGenerator(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.THERMAL_GENERATOR_MENU.get());
    }

    static MachineMenu windTurbine(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.WIND_TURBINE_MENU.get());
    }

    static MachineMenu steamTurbine(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.STEAM_TURBINE_MENU.get());
    }

    static MachineMenu waterMill(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.WATER_MILL_MENU.get());
    }

    static MachineMenu mvWaterMill(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.MV_WATER_MILL_MENU.get());
    }

    static MachineMenu hvWaterMill(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.HV_WATER_MILL_MENU.get());
    }

    static MachineMenu alloySmelter(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.ALLOY_SMELTER_MENU.get());
    }

    static MachineMenu rareEarth(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.RARE_EARTH_MENU.get());
    }

    static MachineMenu uraniumEnricher(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.URANIUM_ENRICHER_MENU.get());
    }

    static MachineMenu canner(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.CANNER_MENU.get());
    }

    static MachineMenu vacuumCanner(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.VACUUM_CANNER_MENU.get());
    }

    static MachineMenu electrolyzer(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.ELECTROLYZER_MENU.get());
    }

    static MachineMenu chargedElectrolyzer(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.CHARGED_ELECTROLYZER_MENU.get());
    }

    private MachineMenu(int id, Inventory playerInventory, MenuType<MachineMenu> type) {
        this(id, playerInventory, new ItemStackHandler(slotCount(type)),
            ContainerLevelAccess.NULL, Blocks.AIR, new SimpleContainerData(DATA_COUNT), () -> {}, type);
        data.set(FLUID, -1);
    }

    private static int slotCount(MenuType<MachineMenu> type) {
        if (type == Machines.STEAM_TURBINE_MENU.get()) {
            return 0;
        }
        if (type == Machines.WIND_TURBINE_MENU.get()) {
            return 1;
        }
        return isFluidGenerator(type) || hasSecondInput(type)
            ? MachineEntity.SLOT_COUNT + 1 : MachineEntity.SLOT_COUNT;
    }

    private static boolean hasSecondInput(MenuType<?> type) {
        return type == Machines.ALLOY_SMELTER_MENU.get() || type == Machines.URANIUM_ENRICHER_MENU.get() || isCanner(type);
    }

    static boolean isCanner(MenuType<?> type) {
        return type == Machines.CANNER_MENU.get() || type == Machines.VACUUM_CANNER_MENU.get();
    }

    MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                Block block, IntSupplier energy, IntSupplier work, IntSupplier fluid, Runnable changed,
                MenuType<MachineMenu> type) {
        this(id, playerInventory, inventory, access, block, readOnly(energy, work, fluid), changed, type);
    }

    private MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                        Block block, ContainerData data, Runnable changed, MenuType<MachineMenu> type) {
        super(type, id);
        if (type == Machines.BATBOX_MENU.get()) {
            storageKind = MachineKind.BATBOX;
        } else if (type == Machines.MFE_MENU.get()) {
            storageKind = MachineKind.MFE;
        } else if (type == Machines.MFSU_MENU.get()) {
            storageKind = MachineKind.MFSU;
        } else if (type == Machines.ESU_MENU.get()) {
            storageKind = MachineKind.ESU;
        } else if (type == Machines.ISU_MENU.get()) {
            storageKind = MachineKind.ISU;
        } else if (type == Machines.PESU_MENU.get()) {
            storageKind = MachineKind.PESU;
        } else {
            storageKind = null;
        }
        this.access = access;
        this.block = block;
        this.data = data;
        machineSlots = inventory.getSlots();
        addDataSlots(data);
        if (isWindTurbine()) {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 80, changed);
        } else if (isFluidGenerator(getType())) {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 24, changed);
            addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 80, changed);
            addMachineSlot(inventory, MachineEntity.FUEL_GENERATOR_BATTERY_SLOT, 136, changed);
        } else if (hasSecondInput(getType())) {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 24, changed);
            addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 136, changed);
            addMachineSlot(inventory, MachineEntity.SECOND_INPUT_SLOT, 80, changed);
        } else if (!isSteamTurbine()) {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 53, changed);
            addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 107, changed);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 83 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 141));
        }
    }

    private void addMachineSlot(ItemStackHandler inventory, int index, int x, Runnable changed) {
        addSlot(new SlotItemHandler(inventory, index, x, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (isStorage()) {
                    return RechargeableItem.supports(stack, storageKind.storageOutput, index == MachineEntity.INPUT_SLOT);
                }
                if (isElectrolyzer()) {
                    return index == MachineEntity.INPUT_SLOT
                        ? Electrolysis.charge(stack) != null : Electrolysis.discharge(stack) != null;
                }
                if (isWindTurbine() || isFluidGenerator(getType()) && index == MachineEntity.FUEL_GENERATOR_BATTERY_SLOT) {
                    if (isThermalGenerator()) {
                        return RechargeableItem.supports(stack, ThermalProduction.CHARGE_PACKET);
                    }
                    WaterMillTier tier = waterMillTier(getType());
                    int chargePacket = tier == null ? MachineKind.BATBOX.storageOutput : tier.chargePacket();
                    return RechargeableItem.supports(stack, chargePacket);
                }
                return index != MachineEntity.OUTPUT_SLOT && super.mayPlace(stack);
            }

            @Override
            public void setChanged() {
                changed.run();
            }
        });
    }

    private static ContainerData readOnly(IntSupplier energy, IntSupplier work, IntSupplier fluid) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                // Minecraft 1.21.1 sends menu data as shorts.
                return switch (index) {
                    case ENERGY_LOW -> energy.getAsInt() & 0xffff;
                    case ENERGY_HIGH -> energy.getAsInt() >>> 16;
                    case WORK_LOW -> work.getAsInt() & 0xffff;
                    case WORK_HIGH -> work.getAsInt() >>> 16;
                    case FLUID -> fluid.getAsInt();
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    int energy() {
        return (data.get(ENERGY_HIGH) << 16) | (data.get(ENERGY_LOW) & 0xffff);
    }

    int work() {
        return (data.get(WORK_HIGH) << 16) | (data.get(WORK_LOW) & 0xffff);
    }

    int fluid() {
        return data.get(FLUID);
    }

    boolean isStorage() {
        return storageKind != null;
    }

    int storageCapacity() {
        return storageKind.capacity;
    }

    boolean isFabricator() {
        return getType() == Machines.FABRICATOR_MENU.get();
    }

    static boolean isFluidGenerator(MenuType<?> type) {
        return type == Machines.GEOTHERMAL_MENU.get() || type == Machines.THERMAL_GENERATOR_MENU.get()
            || waterMillTier(type) != null;
    }

    boolean isThermalGenerator() {
        return getType() == Machines.THERMAL_GENERATOR_MENU.get();
    }

    boolean isWaterMill() {
        return waterMillTier(getType()) != null;
    }

    private static WaterMillTier waterMillTier(MenuType<?> type) {
        if (type == Machines.HV_WATER_MILL_MENU.get()) {
            return WaterMillTier.HV;
        }
        if (type == Machines.MV_WATER_MILL_MENU.get()) {
            return WaterMillTier.MV;
        }
        return type == Machines.WATER_MILL_MENU.get() ? WaterMillTier.BASE : null;
    }

    boolean isWindTurbine() {
        return getType() == Machines.WIND_TURBINE_MENU.get();
    }

    boolean isSteamTurbine() {
        return getType() == Machines.STEAM_TURBINE_MENU.get();
    }

    boolean isAlloySmelter() {
        return getType() == Machines.ALLOY_SMELTER_MENU.get();
    }

    boolean isUraniumEnricher() {
        return getType() == Machines.URANIUM_ENRICHER_MENU.get();
    }

    boolean isVacuumCanner() {
        return getType() == Machines.VACUUM_CANNER_MENU.get();
    }

    boolean isRareEarth() {
        return getType() == Machines.RARE_EARTH_MENU.get();
    }

    boolean isElectrolyzer() {
        return getType() == Machines.ELECTROLYZER_MENU.get() || getType() == Machines.CHARGED_ELECTROLYZER_MENU.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (machineSlots == 0) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int input = MachineEntity.INPUT_SLOT;
        if (stack.getItem() instanceof ic2.api.item.ElectricItem cell) {
            if (isFluidGenerator(getType())) {
                input = MachineEntity.FUEL_GENERATOR_BATTERY_SLOT;
            } else if (isStorage() && cell.canProvideEnergy() && cell.charge(stack) > 0) {
                input = MachineEntity.OUTPUT_SLOT;
            }
        } else if (isElectrolyzer() && Electrolysis.discharge(stack) != null) {
            input = MachineEntity.OUTPUT_SLOT;
        }
        boolean moved = index < machineSlots
            ? moveItemStackTo(stack, machineSlots, slots.size(), true)
            : moveItemStackTo(stack, input, input + 1, false)
                || hasSecondInput(getType()) && moveItemStackTo(stack, MachineEntity.SECOND_INPUT_SLOT, machineSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return before;
    }
}
