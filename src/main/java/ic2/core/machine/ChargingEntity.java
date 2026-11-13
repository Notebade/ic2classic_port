package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import ic2.core.item.RechargeableItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ChargingEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    final ChargingDevices.Kind kind = ((ChargingBlock) getBlockState().getBlock()).kind;
    int energy;
    private int activeTime, cooldown;
    private boolean sorted;
    private int nuclearTicks, nuclearProduction;
    final RedirectorRouting routing = kind.node() ? new RedirectorRouting(this) : null;
    final ItemStackHandler inventory = new ItemStackHandler(kind.slots()) {
        @Override public int getSlotLimit(int slot) { return kind.pad() && slot > kind.projectionSlots() ? 64 : 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override protected void onContentsChanged(int slot) { energy = Math.min(energy, capacity()); setChanged(); }
    };
    final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof RechargeableItem cell)) return ItemStack.EMPTY;
            boolean source = sourceSlot(slot);
            return (source ? cell.charge(stack) == 0 : cell.charge(stack) == cell.capacity())
                ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 22; }
        @Override public int get(int index) { int value = value(index / 2); return index % 2 == 0 ? value & 65535 : value >>> 16; }
        @Override public void set(int index, int value) {}
    };
    ChargingEntity(BlockPos pos, BlockState state) { super(ChargingDevices.ENTITY.get(), pos, state); }
    Direction front() { return kind.pad() ? Direction.DOWN : getBlockState().getValue(BlockStateProperties.FACING); }
    private int value(int index) {
        return switch (index) {
            case 0 -> stored(); case 1 -> capacity(); case 2 -> sorted ? 1 : 0;
            case 3 -> routing == null ? 0 : routing.presentMask();
            case 10 -> routing == null ? activeTime : routing.pending.size();
            default -> index >= 4 && index < 10 && routing != null ? routing.weights[index - 4] : 0;
        };
    }
    private boolean sourceSlot(int slot) { return kind.family == ChargingDevices.Family.STATION ? slot < 16 : slot == (kind.pad() ? 0 : 16); }
    private boolean accepts(int slot, ItemStack stack) {
        if (slot < 0 || slot >= kind.slots() || stack.isEmpty()) return false;
        if (kind.pad() && slot > 0) return slot <= kind.projectionSlots()
            ? stack.getItem() instanceof PadUpgradeItem : stack.getItem() instanceof ic2.api.machine.MachineUpgrade;
        if (kind == ChargingDevices.Kind.PAD_FISSION && stack.getItem() instanceof ic2.core.item.UraniumFuelRod) return true;
        if (kind.pad() && (stack.is(Items.REDSTONE) || stack.is(Items.REDSTONE_BLOCK))) return true;
        int packet = kind.inventoryGrid() && (kind.family == ChargingDevices.Family.STATION || slot < 16) ? Integer.MAX_VALUE : inputPacket();
        if (!RechargeableItem.supports(stack.copyWithCount(1), packet, !sourceSlot(slot))) return false;
        if (kind.family == ChargingDevices.Family.BENCH && sorted && slot < 16 && slot % 4 == 0) {
            EquipmentSlot expected = equipment(slot / 4);
            return stack.getItem() instanceof net.minecraft.world.item.Equipable equipable && equipable.getEquipmentSlot() == expected;
        }
        return true;
    }
    private static EquipmentSlot equipment(int row) { return switch (row) { case 0 -> EquipmentSlot.HEAD; case 1 -> EquipmentSlot.CHEST; case 2 -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET; }; }
    void tick() {
        int before = energy; boolean running = switch (kind.family) {
            case BENCH -> tickBench(); case STATION -> tickStation(); case PAD -> tickPad();
            case MASTER -> { routing.initialize(); yield false; }
            case SLAVE -> routing.tick();
        };
        if (getBlockState().getValue(BlockStateProperties.LIT) != running)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, running), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (kind.family == ChargingDevices.Family.BENCH) updateBenchState(running);
        if (energy != before || running) setChanged();
    }
    private void updateBenchState(boolean running) {
        boolean any = false, full = true;
        for (int slot = 0; slot < 16; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof RechargeableItem cell) { any = true; full &= cell.charge(stack) == cell.capacity(); }
        }
        int state = !any ? 0 : running ? 2 : full ? 3 : 1;
        if (getBlockState().getValue(ChargingBenchBlock.CHARGE_STATE) != state)
            level.setBlock(worldPosition, getBlockState().setValue(ChargingBenchBlock.CHARGE_STATE, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private boolean tickBench() {
        discharge(16);
        int used = 0;
        for (int slot = 0; slot < 16; slot++) used += charge(inventory.getStackInSlot(slot), energy, Integer.MAX_VALUE);
        return used > 0;
    }
    private boolean tickStation() {
        int used = charge(inventory.getStackInSlot(16), energy, Integer.MAX_VALUE);
        for (int slot = 0; slot < 16; slot++) discharge(slot);
        for (int packet = 0; packet < 4 && energy > 0; packet++) {
            int sent = EnergyNetwork.send(level, worldPosition, Math.min(kind.packet, energy), side -> true, 0);
            energy -= sent; used += sent;
        }
        return used > 0;
    }
    private void discharge(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        int tier = kind.family == ChargingDevices.Family.STATION ? Integer.MAX_VALUE : inputPacket();
        if (!RechargeableItem.supports(stack, tier, false)) return;
        RechargeableItem cell = (RechargeableItem) stack.getItem();
        int amount = ChargingDistribution.transfer(cell.charge(stack), energy, capacity(), cell.transferRate());
        if (amount > 0) { cell.setCharge(stack, cell.charge(stack) - amount); energy += amount; setChanged(); }
    }
    private int charge(ItemStack stack, int budget, int packet) {
        if (!RechargeableItem.supports(stack, packet)) return 0;
        RechargeableItem cell = (RechargeableItem) stack.getItem();
        int amount = ChargingDistribution.transfer(Math.min(budget, energy), cell.charge(stack), cell.capacity(), cell.transferRate());
        if (amount > 0) { cell.setCharge(stack, cell.charge(stack) + amount); energy -= amount; setChanged(); }
        return amount;
    }
    void activatePad() { if (cooldown == 0) { activeTime = 20; setChanged(); } }
    private boolean tickPad() {
        discharge(0); redstone();
        fissionFuel();
        if (cooldown > 0) { cooldown--; setChanged(); return false; }
        if (activeTime == 0) return false;
        activeTime--; setChanged();
        int flags = padFlags();
        boolean pvp = level.getServer().isPvpAllowed();
        double range = PadUpgradeRules.range(kind.range, flags);
        var area = new AABB(worldPosition).inflate(range, .5, range);
        if (PadUpgradeRules.has(flags, 0) && pvp && energy > 0) {
            var victims = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area, entity -> !entity.isSpectator() && entity.isAlive());
            if (!victims.isEmpty()) {
                for (var victim : victims) {
                    victim.hurt(ElectricHazards.source(level), 1);
                    victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, kind.tier() * 20, kind.tier() * 2));
                }
                energy -= Math.min(transferLimit(), energy); return true;
            }
        }
        boolean drain = PadUpgradeRules.drain(kind.tier(), flags, pvp);
        List<ItemStack> targets = new ArrayList<>();
        for (Player player : level.getEntitiesOfClass(Player.class, area, player -> !player.isSpectator() && player.isAlive())) {
            List<ItemStack> held = new ArrayList<>();
            if (PadUpgradeRules.has(flags, 4)) for (ItemStack armor : player.getArmorSlots()) eligible(held, armor, drain);
            if (held.isEmpty()) {
                for (int i = PadUpgradeRules.firstSlot(player.getInventory().selected, flags); i <= PadUpgradeRules.lastSlot(player.getInventory().selected, flags); i++) eligible(held, player.getInventory().getItem(i), drain);
                eligible(held, player.getOffhandItem(), drain);
            }
            if (held.isEmpty() && !PadUpgradeRules.has(flags, 4)) for (ItemStack armor : player.getArmorSlots()) eligible(held, armor, drain);
            targets.addAll(held);
        }
        int budget = Math.min(transferLimit(), energy), used = 0;
        java.util.Collections.shuffle(targets);
        for (int i = 0; i < targets.size(); i++) {
            int share = ChargingDistribution.share(budget, targets.size() - i);
            int amount = drain ? drainTarget(targets.get(i), share) : charge(targets.get(i), share, inputPacket());
            used += amount; budget -= amount;
        }
        if (used == 0) { activeTime = 0; cooldown = 20; }
        return used > 0;
    }
    private void eligible(List<ItemStack> items, ItemStack stack, boolean drain) {
        if (!RechargeableItem.supports(stack, inputPacket(), !drain)) return;
        var cell = (RechargeableItem) stack.getItem();
        if (drain ? cell.charge(stack) > 0 : cell.charge(stack) < cell.capacity()) items.add(stack);
    }
    private int drainTarget(ItemStack stack, int budget) {
        var cell = (RechargeableItem) stack.getItem();
        int moved = Math.min(Math.min(budget, energy), Math.min(cell.transferRate(), cell.charge(stack)));
        cell.setCharge(stack, cell.charge(stack) - moved); energy -= moved; setChanged(); return moved;
    }
    int padFlags() {
        int flags = 0;
        for (int i = 1; i <= kind.projectionSlots(); i++) if (inventory.getStackInSlot(i).getItem() instanceof PadUpgradeItem upgrade) flags |= 1 << upgrade.kind.ordinal();
        return flags;
    }
    private MachineModifiers modifiers() {
        return kind.pad() ? MachineModifiers.read(inventory, 1 + kind.projectionSlots(), kind.capacity, kind.tier())
            : new MachineModifiers(kind.capacity, kind.packet, 1, 1);
    }
    private int capacity() { return modifiers().capacity(); }
    private int inputPacket() { return modifiers().packet(); }
    private int transferLimit() { return MachineUpgradeRules.scale(kind.packet, modifiers().speed()); }
    private void fissionFuel() {
        if (kind != ChargingDevices.Kind.PAD_FISSION || energy >= capacity()) return;
        if (nuclearTicks > 0) { nuclearTicks--; energy = Math.min(capacity(), energy + nuclearProduction); setChanged(); return; }
        var stack = inventory.getStackInSlot(0);
        if (!(stack.getItem() instanceof ic2.core.item.UraniumFuelRod rod)) return;
        nuclearProduction = rod.cells() * rod.selfPulsesPerCell() * rod.type().energyPerPulse() * GenerationConfig.NUCLEAR.get() / 20;
        nuclearTicks = 20;
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) inventory.setStackInSlot(0, rod.depletedRods());
        else stack.setDamageValue(stack.getDamageValue() + 1);
        setChanged();
    }
    private void redstone() {
        ItemStack stack = inventory.getStackInSlot(0);
        int value = stack.is(Items.REDSTONE) ? 500 : stack.is(Items.REDSTONE_BLOCK) ? 4500 : 0;
        if (value > 0 && capacity() - energy >= value) { inventory.extractItem(0, 1, false); energy += value; setChanged(); }
    }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) {
        return packet > 0 && packet <= inputPacket() && switch (kind.family) {
            case BENCH -> energy < capacity();
            case PAD -> side == Direction.DOWN && energy < capacity();
            case MASTER -> side == front() && routing.ready();
            default -> false;
        };
    }
    @Override public int receiveEnergy(int offered) {
        if (offered <= 0) return 0;
        if (kind == ChargingDevices.Kind.MASTER) return routing.receive(Math.min(offered, kind.packet));
        if (kind.family != ChargingDevices.Family.BENCH && !kind.pad()) return 0;
        int accepted = Math.min(Math.min(offered, inputPacket()), capacity() - energy);
        energy += accepted; if (accepted > 0) setChanged(); return accepted;
    }
    void button(Player player, int button) {
        if (kind == ChargingDevices.Kind.MASTER) { routing.button(button); return; }
        if (kind.family != ChargingDevices.Family.BENCH) return;
        if (button == 0) { sorted = !sorted; setChanged(); return; }
        if (!sorted || button < 1 || button > 3) return;
        int[] slots = {0,4,8,12,5,9};
        EquipmentSlot[] equipment = {EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stored = inventory.getStackInSlot(slots[i]), worn = player.getItemBySlot(equipment[i]);
            if (worn.getCount() > 1) continue;
            boolean put = button != 2, take = button != 3;
            if (!put && !worn.isEmpty() || !take && !stored.isEmpty()) continue;
            if (put && !worn.isEmpty() && !accepts(slots[i], worn)) continue;
            if (take && !stored.isEmpty() && equipment[i].isArmor() && player.getEquipmentSlotForItem(stored) != equipment[i]) continue;
            inventory.setStackInSlot(slots[i], put ? worn.copy() : ItemStack.EMPTY);
            player.setItemSlot(equipment[i], take ? stored.copy() : ItemStack.EMPTY);
        }
        player.getInventory().setChanged(); setChanged();
    }
    int stored() { return routing == null ? energy : energy + routing.pending.stream().mapToInt(Integer::intValue).sum(); }
    @Override public MachineSnapshot machineInfo() {
        var values = new java.util.HashMap<String, Long>();
        values.put("packet_eu", (long) inputPacket());
        if (kind.family == ChargingDevices.Family.BENCH) values.put("sorted", sorted ? 1L : 0L);
        if (kind.family == ChargingDevices.Family.STATION) values.put("packets_per_tick", 4L);
        if (kind.pad()) {
            values.put("active_ticks", (long) activeTime); values.put("cooldown_ticks", (long) cooldown);
            values.put("installed_upgrades", (long) padFlags()); values.put("range_milliblocks", Math.round(PadUpgradeRules.range(kind.range, padFlags()) * 1000));
            values.put("nuclear_ticks", (long) nuclearTicks);
            values.put("transfer_limit_eu", (long) transferLimit());
            values.put("nuclear_output_eu", (long) nuclearProduction);
        }
        if (routing != null) {
            values.put("pending_packets", (long) routing.pending.size()); values.put("connected_sides", (long) routing.presentMask());
            for (Direction side : Direction.values()) values.put("share_" + side.getName(), (long) routing.weights[side.ordinal()]);
        }
        return MachineTelemetry.snapshot(this, stored(), capacity(), MachineSnapshot.EnergyUnit.EU, values);
    }
    void dropContents() {
        for (int i = 0; i < inventory.getSlots(); i++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.ic2." + kind.id); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player ignored) {
        return new ChargingMenu(id, player, kind, inventory, data, ContainerLevelAccess.create(level, worldPosition), this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.put("inventory", inventory.serializeNBT(lookup)); tag.putInt("energy", energy);
        tag.putBoolean("sorted", sorted); tag.putInt("active", activeTime); tag.putInt("cooldown", cooldown);
        tag.putInt("nuclear_ticks", nuclearTicks); tag.putInt("nuclear_production", nuclearProduction);
        if (routing != null) routing.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        var saved = tag.getCompound("inventory").copy(); saved.putInt("Size", kind.slots()); inventory.deserializeNBT(lookup, saved);
        energy = Mth.clamp(tag.getInt("energy"), 0, capacity()); sorted = tag.getBoolean("sorted");
        activeTime = Mth.clamp(tag.getInt("active"), 0, 20); cooldown = Mth.clamp(tag.getInt("cooldown"), 0, 20);
        nuclearTicks = Math.clamp(tag.getInt("nuclear_ticks"), 0, 20); nuclearProduction = Math.clamp(tag.getInt("nuclear_production"), 0, 120000);
        if (routing != null) routing.load(tag);
    }
}
