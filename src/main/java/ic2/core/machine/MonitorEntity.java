package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.ItemStackHandler;

final class MonitorEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    private int energy;
    private boolean requireRedstone = true;
    int color = 5, scale = 4, alignment;
    int width = 1, height = 1;
    boolean master = true;
    private long gridId;
    List<Component> lines = List.of();
    final ItemStackHandler cards = new ItemStackHandler(8){

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ReactorCardItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    final ContainerData data = new ContainerData(){

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> energy; case 1 -> requireRedstone ? 1 : 0;
                case 2 -> color; case 3 -> scale; case 4 -> alignment; case 5 -> gridId != 0 ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }
    };

    MonitorEntity(BlockPos p, BlockState s) {
        super(NuclearInfrastructure.MONITOR_ENTITY.get(), p, s);
    }

    void tick() {
        var group = group();
        var leader = group.getFirst();
        if (leader != this) return;
        int cost = Math.max(width, height);
        boolean active = group.stream().mapToInt(m -> m.energy).sum() >= cost
            && (!requireRedstone || group.stream().anyMatch(m -> level.hasNeighborSignal(m.worldPosition)));
        if (active) {
            for (var panel : group) {
                int used = Math.min(cost, panel.energy);
                panel.energy -= used; cost -= used;
                if (used > 0) panel.setChanged();
            }
        }
        boolean stateChanged = getBlockState().getValue(BlockStateProperties.LIT) != active;
        for (var panel : group) {
            if (panel.getBlockState().getValue(BlockStateProperties.LIT) != active)
                level.setBlock(panel.worldPosition, panel.getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        if (stateChanged || level.getGameTime() % 5 == 0) {
            var next = new ArrayList<Component>();
            if (active) for (var panel : group) for (int i = 0; i < 8; i++) {
                var card = panel.cards.getStackInSlot(i);
                if (!card.isEmpty()) next.add(ReactorCardItem.linkedStatus(level, card));
            }
            if (!lines.equals(next)) {
                lines = List.copyOf(next);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private List<MonitorEntity> group() {
        var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var right = facing.getCounterClockWise();
        var found = new java.util.LinkedHashMap<BlockPos, MonitorEntity>();
        var queue = new java.util.ArrayDeque<MonitorEntity>();
        found.put(worldPosition, this); queue.add(this);
        while (!queue.isEmpty() && found.size() <= 256) {
            var panel = queue.removeFirst();
            for (var direction : new Direction[]{right, right.getOpposite(), Direction.UP, Direction.DOWN}) {
                var pos = panel.worldPosition.relative(direction);
                if (found.containsKey(pos) || !level.isLoaded(pos)) continue;
                if (level.getBlockEntity(pos) instanceof MonitorEntity next && next.gridId == gridId
                    && next.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                    found.put(pos, next); queue.add(next);
                }
            }
        }
        var cells = new java.util.HashSet<Long>();
        for (var pos : found.keySet()) cells.add(MonitorLayout.key(
            (pos.getX() - worldPosition.getX()) * right.getStepX() + (pos.getZ() - worldPosition.getZ()) * right.getStepZ(),
            worldPosition.getY() - pos.getY()));
        var bounds = found.size() > 256 ? null : MonitorLayout.rectangle(cells);
        if (bounds == null) {
            setBounds(true, 1, 1);
            return List.of(this);
        }
        var origin = worldPosition.relative(right, bounds.x()).below(bounds.y());
        var leader = found.get(origin);
        var result = new ArrayList<MonitorEntity>();
        result.add(leader);
        for (var panel : found.values()) {
            panel.setBounds(panel == leader, bounds.width(), bounds.height());
            if (panel != leader) result.add(panel);
        }
        return result;
    }

    private void setBounds(boolean leader, int w, int h) {
        if (master == leader && width == w && height == h) return;
        master = leader; width = w; height = h;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    void configure(int button) {
        boolean nextRedstone = !requireRedstone;
        int nextColor = (color + 1) % 16, nextScale = Math.clamp(scale + (button == 2 ? -1 : 1), 1, 10);
        int nextAlignment = (alignment + 1) % 3;
        long nextGrid = gridId == 0 ? level.random.nextLong() | 1L : 0;
        for (var panel : group()) {
            switch (button) {
                case 0 -> panel.requireRedstone = nextRedstone;
                case 1 -> panel.color = nextColor;
                case 2, 3 -> panel.scale = nextScale;
                case 4 -> panel.alignment = nextAlignment;
                case 5 -> panel.gridId = nextGrid;
                default -> { }
            }
            panel.setChanged();
            level.sendBlockUpdated(panel.worldPosition, panel.getBlockState(), panel.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean canReceiveEnergy(Direction side, int output, int packet) {
        return packet > 0 && packet <= 8192 && energy < 1000;
    }

    @Override
    public int receiveEnergy(int amount) {
        int used = Math.max(0, Math.min(amount, 1000 - energy));
        energy += used;
        if (used > 0) setChanged();
        return used;
    }

    @Override
    public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, 1000, MachineSnapshot.EnergyUnit.EU, Map.of("cards", (long)java.util.stream.IntStream.range(0, 8).filter((i)->!cards.getStackInSlot(i).isEmpty()).count(), "requires_redstone", requireRedstone ? 1L : 0L));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ic2.monitor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MonitorMenu(id, inv, cards, data, ContainerLevelAccess.create(level, worldPosition), this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("energy", energy);
        tag.putBoolean("redstone", requireRedstone);
        tag.putInt("color", color); tag.putInt("scale", scale); tag.putInt("alignment", alignment);
        tag.putLong("grid_id", gridId);
        tag.put("cards", cards.serializeNBT(lookup));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        energy = Math.clamp(tag.getInt("energy"), 0, 1000);
        requireRedstone = !tag.contains("redstone") || tag.getBoolean("redstone");
        color = tag.contains("color") ? Math.clamp(tag.getInt("color"), 0, 15) : 5;
        scale = tag.contains("scale") ? Math.clamp(tag.getInt("scale"), 1, 10) : 4;
        alignment = Math.clamp(tag.getInt("alignment"), 0, 2);
        gridId = tag.getLong("grid_id");
        if (tag.contains("screen_width")) {
            width = Math.clamp(tag.getInt("screen_width"), 1, 256);
            height = Math.clamp(tag.getInt("screen_height"), 1, 256);
            master = tag.getBoolean("screen_master");
        }
        if (tag.contains("cards")) {
            var saved = tag.getCompound("cards").copy();
            saved.putInt("Size", 8);
            cards.deserializeNBT(lookup, saved);
        }
        if (tag.contains("line_count")) {
            var next = new ArrayList<Component>();
            for (int i = 0; i < Math.clamp(tag.getInt("line_count"), 0, 2048); i++) {
                var line = Component.Serializer.fromJson(tag.getString("line_" + i), lookup);
                if (line != null) next.add(line);
            }
            lines = List.copyOf(next);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        var tag = saveWithoutMetadata(lookup);
        tag.putInt("screen_width", width); tag.putInt("screen_height", height); tag.putBoolean("screen_master", master);
        tag.putInt("line_count", lines.size());
        for (int i = 0; i < lines.size(); i++) tag.putString("line_" + i, Component.Serializer.toJson(lines.get(i), lookup));
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
