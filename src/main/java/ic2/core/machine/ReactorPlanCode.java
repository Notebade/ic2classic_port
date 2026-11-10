package ic2.core.machine;

import ic2.core.IC2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class ReactorPlanCode {
    static final int MAX_LENGTH = 512;
    private static final int ROWS = ReactorRules.SLOT_COUNT / ReactorRules.COLUMNS;
    private static final String TALON_URL = "http://www.talonfiremage.pwp.blueyonder.co.uk/v3/reactorplanner.html?";
    private static final int[] TALON_PARTS = {
        -1, 0, 1, 2, 36, 34, 35, 18, 19, 21, 20, 26, 15, 16, 27, 28, 29, 30, 31, 33, 32, 12, 13, 14, 17
    };
    // The IDs are positional in the original IC2 Classic planner registry.
    private static final String[] PARTS = {
        "uranium_rod_single", "uranium_rod_dual", "uranium_rod_quad",
        "uranium_rod_redstone_single", "uranium_rod_redstone_dual", "uranium_rod_redstone_quad",
        "uranium_rod_blaze_single", "uranium_rod_blaze_dual", "uranium_rod_blaze_quad",
        "uranium_rod_ender_pearl_single", "uranium_rod_ender_pearl_dual", "uranium_rod_ender_pearl_quad",
        "heat_storage_single", "heat_storage_triple", "heat_storage_six",
        "condensator", "condensator_lap", "heat_pack",
        "heat_vent", "heat_vent_core", "heat_vent_advanced", "heat_vent_overclocked",
        "steam_vent", "steam_vent_core", "steam_vent_overclocked", "steam_vent_advanced",
        "heat_spreader", "heat_exchanger", "heat_exchanger_core", "heat_exchanger_spread", "heat_exchanger_advanced",
        "plating", "plating_containment", "plating_heat_capacity", "reflector", "reflector_thick",
        "uranium_rod_isotopic", "uranium_rod_nether_star_single", "uranium_rod_nether_star_dual", "uranium_rod_nether_star_quad",
        "uranium_rod_charcoal_single", "uranium_rod_charcoal_dual", "uranium_rod_charcoal_quad",
        "uranium_rod_isotopic_redstone", "uranium_rod_isotopic_blaze", "uranium_rod_isotopic_ender_pearl",
        "uranium_rod_isotopic_nether_star", "uranium_rod_isotopic_charcoal",
        "electric_vent", "electric_vent_core", "electric_vent_overclocked", "electric_vent_advanced",
        "reactor_fuel_cell", "reflector_iridium", "heat_pump",
        "heat_balancer", "heat_balancer_core", "heat_balancer_spread", "heat_balancer_advanced"
    };

    record Layout(int width, boolean steam, int heat, String name, ItemStack[] items) {}

    enum Format {
        V2, V1, NBT, TALON
    }

    static Layout decode(String code) {
        if (code.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid planner code");
        }
        if (code.startsWith(TALON_URL)) {
            return readTalon(new Bits(base36(code.substring(TALON_URL.length()))));
        }
        var value = base36(code);
        byte[] raw = value.toByteArray();
        if (raw.length >= 2 && raw[0] == 31 && (raw[1] & 255) == 139) {
            return readOldNbt(raw);
        }
        var bits = new Bits(value);
        int version = bits.read(2);
        if (version != 2 && (version != 3 || bits.read(4) != 15 || bits.read(5) != 1)) {
            throw new IllegalArgumentException("Unsupported planner code");
        }
        boolean steam = bits.read(1) != 0;
        int width = bits.read(3) + ReactorRules.BASE_COLUMNS;
        if (width > ReactorRules.COLUMNS) {
            throw new IllegalArgumentException("Invalid reactor width");
        }
        int heat = version == 2 ? readHeat(bits) : bits.read(20);
        char[] name = new char[bits.read(6)];
        for (int index = name.length - 1; index >= 0; index--) {
            name[index] = (char) bits.read(8);
        }
        var items = emptyLayout();
        if (version == 2) {
            readV2(bits, width, steam, items);
        } else {
            readV1(bits, width, steam, items);
        }
        if (bits.value.signum() != 0) {
            throw new IllegalArgumentException("Unexpected planner data");
        }
        return new Layout(width, steam, heat, new String(name), items);
    }

    private static BigInteger base36(String code) {
        if (!code.matches("[0-9a-zA-Z]+")) {
            throw new IllegalArgumentException("Invalid planner code");
        }
        return new BigInteger(code, 36);
    }

    private static ItemStack[] emptyLayout() {
        var items = new ItemStack[ReactorRules.SLOT_COUNT];
        Arrays.fill(items, ItemStack.EMPTY);
        return items;
    }

    private static Layout readTalon(Bits bits) {
        int heat = bits.read(10) * 100;
        int[] ids = new int[ReactorRules.SLOT_COUNT];
        int[] counts = new int[ids.length];
        for (int x = ReactorRules.COLUMNS - 1; x >= 0; x--) {
            for (int y = ROWS - 1; y >= 0; y--) {
                int slot = y * ReactorRules.COLUMNS + x;
                int type = bits.read(7);
                counts[slot] = type > 64 ? type - 63 : 1;
                ids[slot] = type > 64 ? bits.read(7) : type;
            }
        }
        int width = bits.read(3) + ReactorRules.BASE_COLUMNS;
        if (width > ReactorRules.COLUMNS || bits.value.signum() != 0) {
            throw new IllegalArgumentException("Invalid Talon plan");
        }
        var items = emptyLayout();
        for (int slot = 0; slot < ids.length; slot++) {
            int id = ids[slot];
            if (id >= TALON_PARTS.length) {
                throw new IllegalArgumentException("Invalid Talon part");
            }
            if (id != 0) {
                readPart(items, slot, width, false, TALON_PARTS[id], counts[slot]);
            }
        }
        return new Layout(width, false, heat, "", items);
    }

    private static Layout readOldNbt(byte[] raw) {
        try (var stream = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(raw)))) {
            String snbt = stream.readUTF();
            // Old plans contain only flat slot arrays inside a list of component compounds.
            int depth = 0;
            for (int index = 0; index < snbt.length(); index++) {
                char character = snbt.charAt(index);
                if (character == '{' || character == '[') {
                    if (++depth > 4) {
                        throw new IllegalArgumentException("Invalid old planner structure");
                    }
                } else if (character == '}' || character == ']') {
                    depth--;
                }
            }
            if (snbt.length() > 4096 || depth != 0 || snbt.indexOf('"') >= 0 || snbt.indexOf('\'') >= 0) {
                throw new IllegalArgumentException("Invalid old planner data");
            }
            var tag = TagParser.parseTag(snbt);
            if (!tag.contains("Data", Tag.TAG_LIST)) {
                throw new IllegalArgumentException("Missing old planner data");
            }
            var items = emptyLayout();
            boolean steam = tag.getBoolean("SteamReactor");
            var entries = tag.getList("Data", Tag.TAG_COMPOUND);
            for (int index = 0; index < entries.size(); index++) {
                var entry = entries.getCompound(index);
                for (int slot : entry.getIntArray("Slots")) {
                    if (slot != -1) {
                        readPart(items, slot, ReactorRules.COLUMNS, steam, entry.getShort("ID"), 1);
                    }
                }
            }
            return new Layout(ReactorRules.COLUMNS, steam, 0, "", items);
        } catch (IOException | CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid old planner code", exception);
        }
    }

    private static int readHeat(Bits bits) {
        return bits.read(1) == 0 ? 0 : bits.read(bits.read(1) == 0 ? 15 : 20);
    }

    private static void readV1(Bits bits, int width, boolean steam, ItemStack[] items) {
        int types = bits.read(6);
        for (int part = 0; part < types; part++) {
            int id = bits.read(10);
            int slots = bits.read(6);
            for (int entry = 0; entry < slots; entry++) {
                int slot = bits.read(6);
                readPart(items, slot, width, steam, id, bits.read(6) + 1);
            }
        }
    }

    private static void readV2(Bits bits, int width, boolean steam, ItemStack[] items) {
        int idBits = bits.read(4) + 1;
        int[] palette = new int[bits.read(6)];
        for (int index = 0; index < palette.length; index++) {
            palette[index] = bits.read(idBits);
        }
        int slotBits = bits.read(4) + 1;
        int columns = bits.read(ReactorRules.COLUMNS);
        for (int x = ReactorRules.COLUMNS - 1; x >= 0; x--) {
            if ((columns & (1 << x)) == 0) {
                continue;
            }
            int rows = bits.read(ROWS);
            for (int y = ROWS - 1; y >= 0; y--) {
                if ((rows & (1 << y)) == 0) {
                    continue;
                }
                int index = bits.read(slotBits);
                if (index >= palette.length) {
                    throw new IllegalArgumentException("Invalid palette index");
                }
                int count = bits.read(1) == 0 ? 1 : bits.read(6) + 1;
                readPart(items, y * ReactorRules.COLUMNS + x, width, steam, palette[index], count);
            }
        }
    }

    private static void readPart(ItemStack[] items, int slot, int width, boolean steam, int id, int count) {
        if (id < 0 || id >= PARTS.length || slot < 0 || slot >= items.length
            || slot % ReactorRules.COLUMNS >= width || !items[slot].isEmpty()) {
            throw new IllegalArgumentException("Invalid reactor slot or part");
        }
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, PARTS[id])));
        if (!ReactorGrid.accepts(stack, steam)) {
            throw new IllegalArgumentException("Incompatible reactor part");
        }
        if (stack.getItem() instanceof HeatPumpItem) {
            HeatPumpItem.setDirections(stack, (count - 1) & 15);
        } else {
            stack.setCount(Math.min(count, stack.getMaxStackSize()));
        }
        items[slot] = stack;
    }

    static String encode(Format format, int width, boolean steam, int heat, String name, IntFunction<ItemStack> itemAt) {
        return switch (format) {
            case V2 -> writeV2(width, steam, heat, name, itemAt);
            case V1 -> writeV1(width, steam, heat, name, itemAt);
            case NBT -> writeOldNbt(steam, itemAt);
            case TALON -> writeTalon(width, steam, heat, itemAt);
        };
    }

    private static String writeV2(int width, boolean steam, int heat, String name, IntFunction<ItemStack> itemAt) {
        var palette = new ArrayList<Integer>();
        var indices = new int[ReactorRules.SLOT_COUNT];
        Arrays.fill(indices, -1);
        int largestId = 0;
        for (int slot = 0; slot < indices.length; slot++) {
            var stack = itemAt.apply(slot);
            if (stack.isEmpty() || slot % ReactorRules.COLUMNS >= width) {
                continue;
            }
            int id = partId(stack);
            if (!palette.contains(id)) {
                palette.add(id);
            }
            indices[slot] = palette.indexOf(id);
            largestId = Math.max(largestId, id);
        }
        var bits = new Bits(BigInteger.ZERO);
        int slotBits = bitWidth(palette.size());
        int columns = 0;
        for (int x = 0; x < width; x++) {
            int rows = 0;
            for (int y = 0; y < ROWS; y++) {
                int slot = y * ReactorRules.COLUMNS + x;
                if (indices[slot] < 0) {
                    continue;
                }
                var stack = itemAt.apply(slot);
                int count = encodedCount(stack);
                if (count > 1) {
                    bits.write(count - 1, 6);
                }
                bits.write(count > 1 ? 1 : 0, 1);
                bits.write(indices[slot], slotBits);
                rows |= 1 << y;
            }
            if (rows != 0) {
                bits.write(rows, ROWS);
                columns |= 1 << x;
            }
        }
        bits.write(columns, ReactorRules.COLUMNS);
        bits.write(slotBits - 1, 4);
        int idBits = bitWidth(largestId);
        for (int index = palette.size() - 1; index >= 0; index--) {
            bits.write(palette.get(index), idBits);
        }
        bits.write(palette.size(), 6);
        bits.write(idBits - 1, 4);
        writeName(bits, name);
        if (heat > 0) {
            boolean wide = heat >= 1 << 15;
            bits.write(heat, wide ? 20 : 15);
            bits.write(wide ? 1 : 0, 1);
        }
        bits.write(heat > 0 ? 1 : 0, 1);
        bits.write(width - ReactorRules.BASE_COLUMNS, 3);
        bits.write(steam ? 1 : 0, 1);
        bits.write(2, 2);
        return bits.value.toString(36);
    }

    private static String writeV1(int width, boolean steam, int heat, String name, IntFunction<ItemStack> itemAt) {
        var groups = groupSlots(itemAt);
        var bits = new Bits(BigInteger.ZERO);
        for (var entry : groups.entrySet()) {
            for (int slot : entry.getValue()) {
                bits.write(encodedCount(itemAt.apply(slot)) - 1, 6);
                bits.write(slot, 6);
            }
            bits.write(entry.getValue().size(), 6);
            bits.write(entry.getKey(), 10);
        }
        bits.write(groups.size(), 6);
        writeName(bits, name);
        bits.write(heat, 20);
        bits.write(width - ReactorRules.BASE_COLUMNS, 3);
        bits.write(steam ? 1 : 0, 1);
        bits.write(1, 5);
        bits.write(63, 6);
        return bits.value.toString(36);
    }

    private static String writeOldNbt(boolean steam, IntFunction<ItemStack> itemAt) {
        var entries = new ListTag();
        for (var group : groupSlots(itemAt).entrySet()) {
            var entry = new CompoundTag();
            entry.putShort("ID", group.getKey().shortValue());
            var slots = group.getValue();
            entry.putIntArray("Slots", slots.size() == 1 ? new int[] {slots.getFirst(), -1}
                : slots.stream().mapToInt(Integer::intValue).toArray());
            entries.add(entry);
        }
        var tag = new CompoundTag();
        tag.put("Data", entries);
        tag.putBoolean("SteamReactor", steam);
        var bytes = new ByteArrayOutputStream();
        try (var stream = new DataOutputStream(new GZIPOutputStream(bytes))) {
            stream.writeUTF(tag.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not write old planner code", exception);
        }
        return new BigInteger(bytes.toByteArray()).toString(36);
    }

    private static String writeTalon(int width, boolean steam, int heat, IntFunction<ItemStack> itemAt) {
        if (steam) {
            throw new IllegalArgumentException("Talon only supports electric reactors");
        }
        var bits = new Bits(BigInteger.ZERO);
        bits.write(width - ReactorRules.BASE_COLUMNS, 3);
        for (int x = 0; x < ReactorRules.COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                var stack = itemAt.apply(y * ReactorRules.COLUMNS + x);
                int id = stack.isEmpty() ? 0 : talonId(partId(stack));
                bits.write(id, 7);
                if (id != 0 && stack.getCount() > 1) {
                    bits.write(63 + stack.getCount(), 7);
                }
            }
        }
        bits.write(heat / 100, 10);
        return TALON_URL + bits.value.toString(36);
    }

    private static int talonId(int id) {
        for (int index = 1; index < TALON_PARTS.length; index++) {
            if (TALON_PARTS[index] == id) {
                return index;
            }
        }
        throw new IllegalArgumentException("Talon does not support this reactor part");
    }

    private static LinkedHashMap<Integer, List<Integer>> groupSlots(IntFunction<ItemStack> itemAt) {
        var groups = new LinkedHashMap<Integer, List<Integer>>();
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            var stack = itemAt.apply(slot);
            if (!stack.isEmpty()) {
                groups.computeIfAbsent(partId(stack), key -> new ArrayList<>()).add(slot);
            }
        }
        return groups;
    }

    private static int partId(ItemStack stack) {
        int id = Arrays.asList(PARTS).indexOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (id < 0) {
            throw new IllegalArgumentException("Unrecognized reactor part");
        }
        return id;
    }

    private static int encodedCount(ItemStack stack) {
        return stack.getItem() instanceof HeatPumpItem ? HeatPumpItem.directions(stack) + 1 : stack.getCount();
    }

    private static void writeName(Bits bits, String name) {
        int length = Math.min(63, name.length());
        for (int index = 0; index < length; index++) {
            bits.write(name.charAt(index) & 255, 8);
        }
        bits.write(length, 6);
    }

    private static int bitWidth(int value) {
        return Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(value));
    }

    private static final class Bits {
        private BigInteger value;

        private Bits(BigInteger value) {
            this.value = value;
        }

        private int read(int width) {
            int field = value.intValue() & ((1 << width) - 1);
            value = value.shiftRight(width);
            return field;
        }

        private void write(int field, int width) {
            value = value.shiftLeft(width).or(BigInteger.valueOf(field));
        }
    }
}
