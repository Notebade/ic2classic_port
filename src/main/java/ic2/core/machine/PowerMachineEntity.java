package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;

import ic2.core.IC2;
import ic2.core.item.UraniumFuelRod;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

final class PowerMachineEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    @Override public MachineSnapshot machineInfo() {
        var values = new java.util.HashMap<String, Long>();
        if (kind.multiblock()) values.put("formed", structure ? 1L : 0L);
        switch (kind) {
            case TRANSFORMER -> { values.put("packet_eu", (long) packet); values.put("packets_per_tick", (long) packets); }
            case FLUX_LV, FLUX_MV, FLUX_HV -> values.put("input_packet_eu", (long) kind.input);
            case PLASMAFIER -> { values.put("plasma_mb", (long) plasma); values.put("uu_reserve", (long) reserve); }
            case FUSION -> { values.put("heat", (long) heat); values.put("lava_mb", (long) lava); values.put("material_units", (long) material); }
            case BOILER -> { values.put("heat", (long) heat); values.put("water_mb", (long) water); values.put("steam_mb", (long) steam); values.put("fuel_units", (long) fuel); }
            case TUNNEL -> { values.put("water_mb", (long) water); values.put("steam_mb", (long) steam); }
            case WINDMILL -> windmill.telemetry(values);
            case LIQUID_FUEL -> liquidFuel.telemetry(values);
            case GASIFIER, ELECTRIC_GASIFIER, REFINERY, WHISKY_BARREL -> chemical.telemetry(values);
        }
        var unit = kind.flux() ? MachineSnapshot.EnergyUnit.FE : kind.capacity == 0
            ? MachineSnapshot.EnergyUnit.NONE : MachineSnapshot.EnergyUnit.EU;
        return MachineTelemetry.snapshot(this, energy, capacity(), unit, values);
    }
    final PowerMachines.Kind kind = ((PowerMachineBlock) getBlockState().getBlock()).kind;
    private int energy, heat, reserve, plasma, water, steam, lava, material, fuel, remainder;
    private int packet = 32, packets = 1, clock;
    private double fusionRemainder, productionRate = 1;
    private boolean structure;
    final WindmillEngine windmill = kind == PowerMachines.Kind.WINDMILL ? new WindmillEngine() : null;
    final ItemStackHandler inventory = new ItemStackHandler(kind.slots) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (kind == PowerMachines.Kind.WINDMILL && level != null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    };
    private final LiquidFuelEngine liquidFuel = kind == PowerMachines.Kind.LIQUID_FUEL ? new LiquidFuelEngine(this::setChanged, inventory) : null;
    private final ChemicalEngine chemical = switch (kind) {
        case GASIFIER, ELECTRIC_GASIFIER -> new GasifierEngine(kind == PowerMachines.Kind.ELECTRIC_GASIFIER, this::setChanged, inventory);
        case REFINERY -> new RefineryEngine(this::setChanged, inventory);
        case WHISKY_BARREL -> new WhiskyBarrelEngine(this::setChanged, inventory);
        default -> null;
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return accessible() ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return accessible() && extractable(slot) ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
    };
    private final IEnergyStorage fe = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = Math.max(0, Math.min(amount, energy));
            if (!simulate && extracted > 0) { energy -= extracted; setChanged(); }
            return extracted;
        }
        @Override public int getEnergyStored() { return energy; }
        @Override public int getMaxEnergyStored() { return kind.capacity; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 42; }
        @Override public int get(int index) {
            int value = metric(index / 2);
            return index % 2 == 0 ? value & 65535 : value >>> 16;
        }
        @Override public void set(int index, int value) {}
    };

    PowerMachineEntity(BlockPos pos, BlockState state) { super(PowerMachines.ENTITY.get(), pos, state); }
    Direction front() { return getBlockState().getValue(BlockStateProperties.FACING); }
    int capacity() { return kind == PowerMachines.Kind.TRANSFORMER ? EnergyConversion.buffer(packet) : kind.capacity; }
    private boolean accessible() { return !kind.multiblock() || formed(); }
    private int metric(int index) {
        if (chemical != null && index >= 13) return chemical.metric(index);
        return switch (index) {
            case 0 -> energy; case 1 -> heat; case 2 -> water; case 3 -> steam; case 4 -> lava;
            case 5 -> material; case 6 -> plasma; case 7 -> reserve; case 8 -> packet; case 9 -> packets;
            case 10 -> structure ? 1 : 0; case 11 -> fuel; case 12 -> capacity();
            case 13 -> windmill != null ? windmill.speedThousandths() : liquidFuel != null ? liquidFuel.remaining() : 0;
            case 14 -> windmill != null ? windmill.freePermille() : liquidFuel != null ? liquidFuel.tank.getFluidAmount() : 0;
            case 15 -> windmill != null ? windmill.outputHundredths() : liquidFuel != null && liquidFuel.running() ? liquidFuel.output() * 100 : 0;
            case 16 -> kind == PowerMachines.Kind.WINDMILL && inventory.getStackInSlot(0).isDamageableItem()
                ? inventory.getStackInSlot(0).getMaxDamage() - inventory.getStackInSlot(0).getDamageValue() : -1;
            default -> 0;
        };
    }
    IItemHandler automation(Direction side) {
        if (chemical != null) return chemical.automation(side);
        return liquidFuel != null ? liquidFuel.automation(side) : kind.flux() && side != Direction.UP ? null : automation;
    }
    IEnergyStorage feHandler(Direction side) { return kind.flux() && (side == null || side == front()) ? fe : null; }
    private boolean accepts(int slot, ItemStack stack) {
        if (chemical != null) return chemical.accepts(slot, stack);
        if (kind.flux()) return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        return switch (kind) {
            case WINDMILL -> stack.getItem() instanceof WindRotor;
            case LIQUID_FUEL -> LiquidFuelEngine.accepts(slot, stack);
            case PLASMAFIER -> slot == 0 ? stack.is(IC2.UU_MATTER.get()) : slot == 1 && stack.is(IC2.CELL_EMPTY.get());
            case FUSION -> slot < 6 ? stack.getItem() instanceof UraniumFuelRod : stack.is(Items.COBBLESTONE) || stack.is(Items.NETHERRACK);
            case BOILER -> slot == 0 ? FluidUtil.getFluidContained(stack).filter(f -> f.is(Fluids.WATER)).isPresent()
                : (slot == 2 || slot == 4 || slot == 5) && stack.is(BoilerFuel.CAN.get()) && BoilerFuel.amount(stack) > 0;
            default -> false;
        };
    }
    private boolean extractable(int slot) {
        if (kind.flux()) return true;
        return switch (kind) {
            case WINDMILL -> true;
            case PLASMAFIER -> slot == 2;
            case FUSION -> slot < 6 && !(inventory.getStackInSlot(slot).getItem() instanceof UraniumFuelRod);
            case BOILER -> slot == 1 || slot == 3;
            default -> false;
        };
    }
    void tick() {
        clock = (clock + 1) % 102400;
        if (kind.multiblock()) {
            structure = formed();
            if (!structure) { active(false); return; }
        }
        boolean running = switch (kind) {
            case TRANSFORMER -> tickTransformer();
            case FLUX_LV, FLUX_MV, FLUX_HV -> tickFlux();
            case PLASMAFIER -> tickPlasma();
            case FUSION -> tickFusion();
            case TUNNEL -> tickTunnel();
            case BOILER -> tickBoiler();
            case WINDMILL -> tickWindmill();
            case LIQUID_FUEL -> tickLiquidFuel();
            case GASIFIER, ELECTRIC_GASIFIER, REFINERY, WHISKY_BARREL -> tickChemical();
        };
        active(running);
        setChanged();
    }
    private void active(boolean active) {
        if (getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
    }
    private boolean tickWindmill() {
        energy = windmill.tick((net.minecraft.server.level.ServerLevel) level, worldPosition, front(), inventory, clock, energy);
        energy -= EnergyNetwork.send(level, worldPosition, Math.min(energy, 512), side -> side != front(), 0);
        if (clock % 20 == 0) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return windmill.spinning();
    }
    private boolean tickLiquidFuel() {
        energy = liquidFuel.tick(energy, inventory);
        energy -= EnergyNetwork.send(level, worldPosition, Math.min(energy, liquidFuel.packet()), side -> true, 0);
        return liquidFuel.running();
    }
    private boolean tickChemical() {
        var result = chemical.tick((net.minecraft.server.level.ServerLevel) level, worldPosition, energy, clock, inventory);
        energy = result.energy(); return result.active();
    }
    private boolean tickTransformer() {
        int before = energy;
        for (int i = 0; i < packets && energy >= packet; i++) {
            int sent = EnergyNetwork.send(level, worldPosition, packet, side -> side != front(), 0);
            energy -= sent;
            if (sent == 0) break;
        }
        return energy < before;
    }
    private boolean tickFlux() {
        int before = energy;
        var item = inventory.getStackInSlot(0).getCapability(Capabilities.EnergyStorage.ITEM);
        if (item != null && item.canReceive()) energy -= item.receiveEnergy(Math.min(energy, kind.input * 4 * 25), false);
        BlockPos next = worldPosition.relative(front());
        if (level.isLoaded(next)) {
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, next, front().getOpposite());
            if (target != null && target.canReceive()) energy -= target.receiveEnergy(energy, false);
        }
        return energy < before;
    }
    private boolean tickPlasma() {
        var state = new PlasmaCycle.State(energy, reserve, plasma);
        if (clock % 40 == 0 && reserve < 50 && !inventory.getStackInSlot(0).isEmpty()) {
            state = state.supplyMatter(); inventory.extractItem(0, 1, false);
        }
        var next = state.step();
        energy = next.energy(); reserve = next.reserve(); plasma = next.plasma();
        if (clock % 50 == 0 && plasma >= 1000 && inventory.getStackInSlot(1).is(IC2.CELL_EMPTY.get())
                && fits(2, new ItemStack(PowerMachines.PLASMA_CELL.get()))) {
            inventory.extractItem(1, 1, false); put(2, new ItemStack(PowerMachines.PLASMA_CELL.get())); plasma -= 1000;
        }
        return state.energy() > energy;
    }
    private boolean tickTunnel() {
        var step = SteamTunnelCycle.step(energy, steam, water, remainder);
        boolean running = step.energy() > energy;
        energy = step.energy(); steam = step.steam(); water = step.water(); remainder = step.remainder();
        for (int i = 0; i < 4 && energy > 0; i++) {
            int sent = EnergyNetwork.send(level, worldPosition, Math.min(512, energy), side -> true, 0);
            energy -= sent; if (sent == 0) break;
        }
        pushFluid();
        return running;
    }
    private boolean tickFusion() {
        if (clock % 10 != 0) return heat == 48000;
        ItemStack feed = inventory.getStackInSlot(6);
        while (material <= 9000 && !feed.isEmpty() && accepts(6, feed)) {
            productionRate = feed.is(Items.NETHERRACK) ? 1.5 : 1;
            material += 1000; feed.shrink(1);
        }
        int rods = 0, slots = 0, totalHeat = 0;
        for (int slot = 0; slot < 6; slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof UraniumFuelRod rod) {
                rods += rod.cells(); slots++; totalHeat += rod.type().fusionHeat();
            }
        }
        var step = new FusionCycle.State(heat, material, lava, fusionRemainder)
            .step(totalHeat / (double) Math.max(1, slots), rods, slots, productionRate);
        heat = step.next().heat(); material = step.next().material(); lava = step.next().lava(); fusionRemainder = step.next().remainder();
        int damage = step.damage() + (clock % 200 == 0 && heat == 48000 && material > 0 && lava < 522000 ? 1 : 0);
        if (damage > 0) damageRods(damage);
        if (clock % (rods == 0 ? 10 : 10240) == 0) {
            if (rods == 0 && heat > 24000 || lava == 0 && heat > 0) heat = Math.max(rods == 0 && heat > 24000 ? 24000 : 0, heat - 10);
            else if (lava > 0) lava--;
        }
        return heat == 48000;
    }
    private void damageRods(int damage) {
        for (int slot = 0; slot < 6; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof UraniumFuelRod rod) {
                if (stack.getDamageValue() + damage > stack.getMaxDamage())
                    inventory.setStackInSlot(slot, level.random.nextInt(3) == 0 ? rod.depletedRods() : ItemStack.EMPTY);
                else stack.setDamageValue(stack.getDamageValue() + damage);
            }
        }
    }
    private boolean tickBoiler() {
        refillWater();
        boolean burning = fuel > 0;
        if (!burning) {
            ItemStack can = inventory.getStackInSlot(2);
            int stored = BoilerFuel.amount(can);
            fuel = Math.min(10000, stored);
            if (fuel > 0) BoilerFuel.set(can, stored - fuel);
            else heat = Math.max(0, heat - 1);
            if (BoilerFuel.amount(can) == 0 && !can.isEmpty() && fits(3, can)) { put(3, can.copy()); inventory.setStackInSlot(2, ItemStack.EMPTY); }
            transfer(4, 5); transfer(5, 2);
        } else {
            fuel = Math.max(0, fuel - (heat < 24000 ? 30 : 8));
            heat = Math.min(24000, heat + 1);
        }
        var output = BoilerCycle.evaporate(heat, water, steam, remainder);
        water = output.water(); steam = output.steam(); remainder = output.remainder();
        pushFluid();
        return burning;
    }
    private void refillWater() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return;
        var preview = FluidUtil.tryEmptyContainer(input, fluids, Integer.MAX_VALUE, null, false);
        if (preview.isSuccess() && fits(1, preview.getResult())) {
            var result = FluidUtil.tryEmptyContainer(input, fluids, Integer.MAX_VALUE, null, true);
            if (result.isSuccess()) { inventory.extractItem(0, 1, false); put(1, result.getResult()); }
        }
    }
    private void transfer(int from, int to) {
        if (inventory.getStackInSlot(to).isEmpty() && !inventory.getStackInSlot(from).isEmpty()) {
            inventory.setStackInSlot(to, inventory.getStackInSlot(from)); inventory.setStackInSlot(from, ItemStack.EMPTY);
        }
    }
    private boolean fits(int slot, ItemStack stack) {
        ItemStack current = inventory.getStackInSlot(slot);
        return stack.isEmpty() || current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
            && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }
    private void put(int slot, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack current = inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot, current.isEmpty() ? stack.copy() : current.copyWithCount(current.getCount() + stack.getCount()));
    }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int offered) {
        return kind.input > 0 && offered <= kind.input && energy < capacity()
            && (kind == PowerMachines.Kind.TRANSFORMER ? side == front() : !kind.flux() || side != front());
    }
    @Override public int receiveEnergy(int available) {
        int accepted = kind.flux() ? EnergyConversion.acceptedEu(energy, capacity(), available) : Math.max(0, Math.min(available, capacity() - energy));
        energy += accepted * (kind.flux() ? 4 : 1);
        if (accepted > 0) setChanged();
        return accepted;
    }
    void button(int button) {
        if (chemical != null) { if (chemical.button(button)) setChanged(); return; }
        if (kind != PowerMachines.Kind.TRANSFORMER) return;
        int next = switch (button) { case 0 -> Math.max(1, packet / 2); case 1 -> Math.min(32768, packet * 2);
            case 2 -> Math.max(1, packet - 1); case 3 -> Math.min(32768, packet + 1); default -> packet; };
        // Reducing packet size must not erase stored EU. Discharge before lowering below current buffer.
        if (EnergyConversion.buffer(next) >= energy) packet = next;
        if (button == 4) packets = Math.max(1, packets - 1);
        if (button == 5) packets = Math.min(32, packets + 1);
        setChanged();
    }
    private List<PowerStructure.Cell> cells() {
        return switch (kind) { case FUSION -> PowerStructure.fusion(); case TUNNEL -> PowerStructure.tunnel();
            case BOILER -> PowerStructure.boiler(); default -> List.of(); };
    }
    private BlockPos position(PowerStructure.Cell cell) {
        Direction front = front();
        return worldPosition.offset(front.getStepZ() * cell.x() - front.getStepX() * cell.z(), -cell.y(),
            -front.getStepX() * cell.x() - front.getStepZ() * cell.z());
    }
    boolean contains(BlockPos pos) { return cells().stream().anyMatch(cell -> position(cell).equals(pos)); }
    boolean shapeValid() {
        if (!kind.multiblock() || front().getAxis().isVertical()) return false;
        for (var cell : cells()) {
            BlockPos pos = position(cell);
            if (!level.isLoaded(pos) || !matches(level.getBlockState(pos), cell.part())) return false;
        }
        return true;
    }
    private boolean matches(BlockState state, PowerStructure.Part part) {
        return switch (part) {
            case AIR -> state.isAir();
            case STONE -> state.is(PowerMachines.REINFORCED_STONE.get()) || state.is(PowerMachines.FUSION_VALVE.get());
            case GLASS -> state.is(Machines.REINFORCED_GLASS.get()) || state.is(PowerMachines.FUSION_VALVE.get());
            case BOILER -> state.is(PowerMachines.BOILER_CASING.get());
            case TUNNEL -> state.is(PowerMachines.TUNNEL_CASING.get());
            case TUNNEL_VALVE -> state.is(PowerMachines.TUNNEL_CASING.get()) || state.is(PowerMachines.TUNNEL_VALVE.get());
        };
    }
    boolean formed() {
        if (!shapeValid()) return false;
        for (var cell : cells()) if (level.getBlockEntity(position(cell)) instanceof PowerPartEntity part && !part.canClaim(worldPosition)) return false;
        for (var cell : cells()) if (level.getBlockEntity(position(cell)) instanceof PowerPartEntity part) part.claim(worldPosition);
        return true;
    }
    IFluidHandler fluidHandler() { return chemical != null ? chemical.fluids() : liquidFuel != null ? liquidFuel.tank : kind.multiblock() ? fluids : null; }
    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public int getTanks() { return kind == PowerMachines.Kind.FUSION ? 1 : 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (kind == PowerMachines.Kind.FUSION) return new FluidStack(Fluids.LAVA, Math.max(0, lava - 10000));
            return tank == 0 ? new FluidStack(Fluids.WATER, water) : new FluidStack(SteamFluids.steam(), steam);
        }
        @Override public int getTankCapacity(int tank) {
            return kind == PowerMachines.Kind.FUSION ? 512000 : tank == 0 ? (kind == PowerMachines.Kind.BOILER ? 16000 : 2000)
                : kind == PowerMachines.Kind.BOILER ? 128000 : 16000;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return kind == PowerMachines.Kind.BOILER ? tank == 0 && stack.is(Fluids.WATER)
                : kind == PowerMachines.Kind.TUNNEL && tank == 1 && SteamFluids.isSteam(stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!formed()) return 0;
            int tank = kind == PowerMachines.Kind.BOILER ? 0 : 1;
            if (!isFluidValid(tank, stack)) return 0;
            int accepted = Math.min(stack.getAmount(), getTankCapacity(tank) - (tank == 0 ? water : steam));
            if (action.execute() && accepted > 0) { if (tank == 0) water += accepted; else steam += accepted; setChanged(); }
            return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            FluidStack result = outputFluid(stack.getAmount());
            return FluidStack.isSameFluidSameComponents(result, stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!formed() || amount <= 0) return FluidStack.EMPTY;
            FluidStack result = outputFluid(amount);
            if (action.execute() && !result.isEmpty()) {
                if (kind == PowerMachines.Kind.FUSION) lava -= result.getAmount();
                else if (kind == PowerMachines.Kind.BOILER) steam -= result.getAmount(); else water -= result.getAmount();
                setChanged();
            }
            return result;
        }
    };
    private FluidStack outputFluid(int amount) {
        if (kind == PowerMachines.Kind.FUSION) return new FluidStack(Fluids.LAVA, Math.min(Math.max(0, amount), Math.max(0, lava - 10000)));
        return kind == PowerMachines.Kind.BOILER ? new FluidStack(SteamFluids.steam(), Math.min(Math.max(0, amount), steam))
            : new FluidStack(Fluids.WATER, Math.min(Math.max(0, amount), water));
    }
    private void pushFluid() {
        if (outputFluid(1).isEmpty()) return;
        Set<BlockPos> shape = new HashSet<>();
        shape.add(worldPosition);
        for (var cell : cells()) shape.add(position(cell));
        pushFrom(worldPosition, shape);
        for (var cell : cells()) {
            if (outputFluid(1).isEmpty()) break;
            BlockPos port = position(cell);
            if (level.getBlockEntity(port) instanceof PowerPartEntity) pushFrom(port, shape);
        }
    }
    private void pushFrom(BlockPos port, Set<BlockPos> shape) {
        for (Direction side : Direction.values()) {
            if (outputFluid(1).isEmpty()) return;
            BlockPos pos = port.relative(side);
            if (!level.isLoaded(pos) || shape.contains(pos)) continue;
            var target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side.getOpposite());
            if (target != null) fluids.drain(target.fill(outputFluid(1000), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }
    void dropContents() {
        if (kind == PowerMachines.Kind.WHISKY_BARREL) return; // Barrel loot preserves inventory and ageing together.
        for (int i = 0; i < inventory.getSlots(); i++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
    }
    void restoreBarrel(CompoundTag data) {
        if (kind != PowerMachines.Kind.WHISKY_BARREL) return;
        inventory.deserializeNBT(level.registryAccess(), data.getCompound("inventory"));
        chemical.load(data.getCompound("chemical"), level.registryAccess()); setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.ic2." + kind.id); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player ignored) {
        return new PowerMachineMenu(id, player, kind, inventory, data, ContainerLevelAccess.create(level, worldPosition), this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.put("inventory", inventory.serializeNBT(lookup));
        tag.putIntArray("state", new int[]{energy, heat, reserve, plasma, water, steam, lava, material, fuel, remainder, packet, packets, clock});
        tag.putDouble("fusion_remainder", fusionRemainder); tag.putDouble("production_rate", productionRate);
        if (windmill != null) tag.put("windmill", windmill.save());
        if (liquidFuel != null) tag.put("liquid_fuel", liquidFuel.save(lookup));
        if (chemical != null) tag.put("chemical", chemical.save(lookup));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); inventory.deserializeNBT(lookup, tag.getCompound("inventory"));
        int[] state = tag.getIntArray("state");
        if (state.length == 13) {
            packet = Mth.clamp(state[10], 1, 32768); packets = Mth.clamp(state[11], 1, 32);
            energy = Mth.clamp(state[0], 0, capacity()); heat = Mth.clamp(state[1], 0, kind == PowerMachines.Kind.FUSION ? 48000 : 24000);
            reserve = Mth.clamp(state[2], 0, 149); plasma = Mth.clamp(state[3], 0, 10000);
            water = Mth.clamp(state[4], 0, kind == PowerMachines.Kind.BOILER ? 16000 : 2000);
            steam = Mth.clamp(state[5], 0, kind == PowerMachines.Kind.BOILER ? 128000 : 16000);
            lava = Mth.clamp(state[6], 0, 522000); material = Mth.clamp(state[7], 0, 10000);
            fuel = Mth.clamp(state[8], 0, 10000); remainder = Math.floorMod(state[9], kind == PowerMachines.Kind.TUNNEL ? 100 : 1000);
            clock = Math.floorMod(state[12], 102400);
        }
        double saved = tag.getDouble("fusion_remainder"); fusionRemainder = Double.isFinite(saved) ? Mth.clamp(saved, 0, 10000) : 0;
        productionRate = tag.getDouble("production_rate") == 1.5 ? 1.5 : 1;
        if (windmill != null) windmill.load(tag.getCompound("windmill"));
        if (liquidFuel != null) liquidFuel.load(tag.getCompound("liquid_fuel"), lookup);
        if (chemical != null) chemical.load(tag.getCompound("chemical"), lookup);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return windmill != null ? net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this) : null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return windmill != null ? saveWithoutMetadata(lookup) : super.getUpdateTag(lookup);
    }
}
