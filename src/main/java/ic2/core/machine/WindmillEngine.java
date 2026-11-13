package ic2.core.machine;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.items.ItemStackHandler;

final class WindmillEngine {
    private WindmillCycle.State state = new WindmillCycle.State(0, 0);
    private double freeAir, airSpeed;
    private int radius = -1;
    private Direction facing;
    private double renderTime = -1;
    private float renderAngle;
    int tick(ServerLevel level, BlockPos pos, Direction front, ItemStackHandler inventory, int clock, int energy) {
        ItemStack stack = inventory.getStackInSlot(0);
        boolean rotor = stack.getItem() instanceof WindRotor && front.getAxis().isHorizontal();
        double target = 0;
        if (rotor) {
            var blade = (WindRotor) stack.getItem();
            boolean resample = radius != blade.kind.radius || facing != front || clock % 1024 == 0;
            if (resample) {
                radius = blade.kind.radius; facing = front;
                freeAir = measure(level, pos, front, radius);
            }
            if (resample || clock % 128 == 0) airSpeed = WindProduction.windSpeed(level) / 100;
            target = airSpeed * freeAir * blade.kind.effectiveness * (level.isRaining() ? 1.5 : 1) * (level.isThundering() ? 1.5 : 1);
            if (clock % 64 == 0 && target > 0 && stack.isDamageableItem()) {
                if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) inventory.setStackInSlot(0, ItemStack.EMPTY);
                else stack.setDamageValue(stack.getDamageValue() + 1);
            }
        } else { radius = -1; freeAir = 0; airSpeed = 0; }
        var step = state.step(target, rotor, freeAir > 0, energy);
        state = step.next();
        return step.energy();
    }
    private static double measure(ServerLevel level, BlockPos pos, Direction front, int radius) {
        var area = WindmillCycle.area(front.getStepX(), front.getStepZ(), radius);
        var box = BoundingBox.fromCorners(pos.offset(area.minX(), area.minY(), area.minZ()), pos.offset(area.maxX(), area.maxY(), area.maxZ()));
        int[] mills = {1};
        int connected = ConnectedSpace.count(level, pos, box, Integer.MAX_VALUE, candidate -> {
            var block = level.getBlockState(candidate);
            if (block.is(PowerMachines.Kind.WINDMILL.block())) { mills[0]++; return true; }
            return block.isAir();
        }) + 1;
        int volume = (area.maxX() - area.minX() + 1) * (area.maxY() - area.minY() + 1) * (area.maxZ() - area.minZ() + 1);
        return WindmillCycle.freeRatio(connected, volume, mills[0]);
    }
    int speedThousandths() { return (int) (state.speed() * 1000); }
    boolean spinning() { return state.speed() > 0; }
    int freePermille() { return (int) (freeAir * 1000); }
    int outputHundredths() { return (int) (state.speed() * 25600); }
    float angle(double time) {
        if (renderTime >= 0) renderAngle = (float) ((renderAngle + Math.clamp(time - renderTime, 0, 2) * state.speed() * 18) % 360);
        renderTime = time; return renderAngle;
    }
    void telemetry(Map<String, Long> values) {
        values.put("speed_thousandths", (long) speedThousandths()); values.put("free_air_permille", (long) freePermille());
        values.put("output_eu_hundredths", (long) outputHundredths());
    }
    CompoundTag save() {
        var tag = new CompoundTag(); tag.putDouble("speed", state.speed()); tag.putInt("remainder", state.remainder()); return tag;
    }
    void load(CompoundTag tag) {
        double speed = tag.getDouble("speed");
        state = new WindmillCycle.State(Double.isFinite(speed) ? Mth.clamp(speed, 0, 3.375) : 0,
            Mth.clamp(tag.getInt("remainder"), 0, WindmillCycle.DIVISOR));
        radius = -1; facing = null;
    }
}
