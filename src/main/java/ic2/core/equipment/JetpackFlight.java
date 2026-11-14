package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import ic2.api.item.EquipmentControls;
import ic2.api.item.EquipmentInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Source jetpack state machine shared by worn fuel packs, electric packs and installed modules. */
final class JetpackFlight {
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, "ic2");
    private static final java.util.List<? extends java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> EVENTS =
        java.util.List.of("loop", "fire").stream().map(name -> SOUNDS.register("tools.jetpack.jetpack_" + name,
            net.minecraft.sounds.SoundEvent::createVariableRangeEvent)).toList();
    static void register(net.neoforged.bus.api.IEventBus bus) { SOUNDS.register(bus); }
    static net.minecraft.sounds.SoundEvent sound(int mode) { return EVENTS.get(mode).get(); }
    interface Pack extends ConfigurableTool {
        Profile flightProfile();
        int fuel(ItemStack power);
        int maxFuel();
        void spend(ItemStack power, Player player, int amount);
    }
    record Profile(boolean electric, boolean disable, double power, double drop, double heightDivisor,
                   int rocketLimit, int normalCost, int hoverCost, double normalThrust, double hoverThrust, double advancedThrust) {
        boolean advanced() { return rocketLimit > 0; }
        int cost(int hover) { return hover == 1 ? hoverCost : normalCost; }
        double thrust(int hover) { return hover == 0 ? normalThrust : hover == 1 ? hoverThrust : advancedThrust; }
    }
    private JetpackFlight() {}

    static void tick(Pack pack, ItemStack stack, ItemStack power, Player player) {
        var state = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var input = EquipmentControls.current(player); var profile = pack.flightProfile();
        if (toggleDisabled(state, input, profile, player)) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(state)); return;
        }
        var playerData = player.getPersistentData();
        int specialTicks = playerData.getInt("SpecialMovementTicker");
        if (specialTicks > 0) {
            playerData.putInt("SpecialMovementTicker", --specialTicks);
            if (specialTicks == 0) playerData.putBoolean("SpecialMovement", false);
        }
        int timer = Math.max(0, state.getInt("ToggleTimer"));
        int delay = Math.max(0, state.getInt("RocketDelay"));
        int hover = Math.max(0, pack.modes().indexOf(pack.mode(stack)));
        boolean rocket = profile.advanced() && state.getBoolean("RocketMode");
        if (input.jump() && input.mode() && !input.alt() && timer == 0) {
            hover = (hover + 1) % pack.modes().size(); timer = 10;
            state.putString("ic2_tool_mode", pack.modes().get(hover));
            message(player, hover == 0 ? "hover_off" : hover == 1 ? "hover_on" : "adv_hover_on");
            if (hover != 0 && rocket) {
                rocket = false; state.remove("UsedEnergy"); message(player, "rocket_mode_off_forced");
            }
        }
        if (input.mode() && input.boost() && profile.advanced() && timer == 0) {
            timer = 10; rocket = !rocket; message(player, rocket ? "rocket_mode_on" : "rocket_mode_off");
            if (rocket && hover != 0) {
                hover = 0; state.putString("ic2_tool_mode", "normal"); message(player, "hover_off_forced");
            } else if (!rocket) state.remove("UsedEnergy");
        }
        int useMode = 2;
        if (rocket && input.boost() && delay == 0) {
            int charged = Math.max(0, state.getInt("UsedEnergy"));
            if (pack.fuel(power) > 0 && charged <= profile.rocketLimit) {
                int amount = Math.min(profile.normalCost * 5, pack.fuel(power));
                pack.spend(power, player, amount); state.putInt("UsedEnergy", charged + amount);
            }
            useMode = 0;
        }
        var affected = player.getRootVehicle();
        if (input.alt() && rocket && delay == 0) {
            delay = release(pack, power, player, affected, state); rocket = false;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 4, (1 + (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * .2f) * .7f);
        } else if (input.alt() && delay == 0 && !input.mode()) {
            hover = hover == 0 ? 1 : 0; timer = 10;
        }
        if (input.jump() && !rocket && delay == 0
                || hover == 2 && !affected.onGround()
                || hover == 1 && !affected.onGround() && affected.getDeltaMovement().y < -.15) {
            useMode = thrust(pack, power, player, affected, input, hover, state);
        }
        state.putBoolean("RocketMode", rocket);
        state.putInt("ToggleTimer", Math.max(0, timer - 1));
        state.putInt("RocketDelay", Math.max(0, delay - 1));
        state.putInt("FuelUseTime", Math.max(0, state.getInt("FuelUseTime") - 1));
        state.putInt("LastUseMode", useMode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(state));
    }

    private static boolean toggleDisabled(CompoundTag state, EquipmentInput input, Profile profile, Player player) {
        int ticker = Math.max(0, state.getInt("JetpackTicker"));
        boolean disabled = state.getBoolean("disabled");
        if (ticker > 0) state.putInt("JetpackTicker", ticker - 1);
        else if (input.toggle() && (disabled || profile.disable)) {
            state.putInt("JetpackTicker", 10); state.putBoolean("disabled", !disabled);
            state.putInt("LastUseMode", 2); message(player, disabled ? "on" : "off"); return true;
        }
        return disabled;
    }

    private static int thrust(Pack pack, ItemStack powerStack, Player player, Entity affected, EquipmentInput input, int hover, CompoundTag state) {
        int fuel = pack.fuel(powerStack); if (fuel <= 0) return 2;
        var profile = pack.flightProfile();
        boolean dropped = fuel / (double) pack.maxFuel() <= profile.drop;
        double power = profile.power * Math.min(1, fuel / (pack.maxFuel() * profile.drop));
        double forward = power * profile.thrust(hover) * 2;
        if (input.forward() && !player.isFallFlying()) affected.moveRelative(.02f, new Vec3(0, 0, .4 * forward));
        int ceiling = (int) (player.level().getMaxBuildHeight() / profile.heightDivisor);
        power *= Math.clamp((ceiling - affected.getY()) / 25, 0, 1);
        var motion = affected.getDeltaMovement(); double next = Math.min(motion.y + power * .2, .6);
        if (state.getBoolean("SpecialMode") && !player.onGround()) {
            affected.setDeltaMovement(motion.add(0, next, 0));
            player.getPersistentData().putBoolean("SpecialMovement", affected.getDeltaMovement().y < 0);
            player.getPersistentData().putInt("SpecialMovementTicker", 40);
        }
        else if (player.isFallFlying()) {
            forward = power * profile.thrust(hover) * 2;
            affected.moveRelative(.1f, new Vec3(0, 0, .4 * Math.min(forward, .7)));
        } else affected.setDeltaMovement(motion.x, next, motion.z);
        if (hover != 0) {
            double cap = hover == 1 ? -.2 : 0;
            if (!player.isShiftKeyDown() || !input.jump()) {
                cap = hover != 1 && !player.isShiftKeyDown() ? 0 : -.2;
                if (profile.electric && input.jump()) cap = hover == 1 ? .1 : .3;
            }
            var current = affected.getDeltaMovement();
            if (current.y > cap) affected.setDeltaMovement(current.x, Math.max(motion.y, cap), current.z);
        }
        int useMode = dropped ? 1 : 2;
        int fuelTime = state.getInt("FuelUseTime");
        if (fuelTime == 0) {
            pack.spend(powerStack, player, profile.cost(hover));
            if (!dropped) useMode = 0;
            if (hover == 1 && !input.jump()) state.putInt("FuelUseTime", 5);
        } else if (fuelTime <= 1 && !dropped) useMode = 0;
        affected.hurtMarked = true;
        if (!state.getBoolean("SpecialMode")) resetFall(player, affected);
        return useMode;
    }

    private static int release(Pack pack, ItemStack power, Player player, Entity affected, CompoundTag state) {
        int energy = state.getInt("UsedEnergy"); if (energy <= 0) return 0;
        state.putInt("UsedEnergy", 0); pack.spend(power, player, 50);
        var profile = pack.flightProfile();
        int charges = energy / profile.normalCost / 50;
        double speed = profile.power * charges / 4;
        if (player.isFallFlying()) {
            double forward = profile.power * profile.normalThrust * 2 * Math.min(speed * 2, 10);
            affected.moveRelative(10, new Vec3(0, 0, .4 * forward));
        } else { affected.setDeltaMovement(affected.getDeltaMovement().add(0, Math.min(speed, 10), 0)); resetFall(player, affected); }
        affected.hurtMarked = true;
        return (int) (speed * 10);
    }
    private static void resetFall(Player player, Entity affected) {
        affected.resetFallDistance(); affected.walkDist = 0;
        if (player instanceof net.minecraft.server.level.ServerPlayer server && server.connection instanceof FlightTimeAccess flight) flight.ic2$resetFlightTime();
    }
    private static void message(Player player, String key) { player.displayClientMessage(Component.translatable("info.item.ic2.jetpack." + key), false); }

    static void tooltip(Pack pack, ItemStack stack, java.util.List<Component> tooltip) {
        key(tooltip, "jump_to_fly", "key.jump");
        key(tooltip, "mode_switch", "key.ic2.mode", "key.jump");
        key(tooltip, "quick_toggle", "key.ic2.alt");
        if (pack.flightProfile().disable) key(tooltip, "toggle", "key.ic2.toggle");
        if (pack.flightProfile().advanced()) {
            key(tooltip, "toggle_rocket", "key.ic2.mode", "key.ic2.boost");
            key(tooltip, "charge_rocket", "key.ic2.boost"); key(tooltip, "release_rocket", "key.ic2.alt");
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (data.getBoolean("RocketMode")) tooltip.add(Component.translatable("tooltip.item.ic2.jetpack.rocket_charge",
                (int) (100L * data.getInt("UsedEnergy") / pack.flightProfile().rocketLimit)));
        }
    }
    private static void key(java.util.List<Component> tooltip, String description, String... keys) {
        var line = Component.empty();
        for (var key : keys) { if (!line.getSiblings().isEmpty()) line.append(" + "); line.append(Component.translatable(key)); }
        tooltip.add(line.append(": ").append(Component.translatable("tooltip.item.ic2.jetpack." + description)));
    }
}
