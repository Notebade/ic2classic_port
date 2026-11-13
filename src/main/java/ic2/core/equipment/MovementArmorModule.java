package ic2.core.equipment;

import ic2.api.item.*;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;

final class MovementArmorModule extends Item implements ArmorModule {
    private enum Kind { JUMP, SPEED, STEP, PARACHUTE, WATER }
    private static final ResourceLocation STEP = ArmorModule.id("step_assist");
    private final Kind kind;
    private MovementArmorModule(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    static void register(DeferredRegister.Items items) {
        items.registerItem("jump_boost_module", p -> new MovementArmorModule(p, Kind.JUMP));
        items.registerItem("speed_boost_module", p -> new MovementArmorModule(p, Kind.SPEED));
        items.registerItem("step_assist_module", p -> new MovementArmorModule(p, Kind.STEP));
        items.registerItem("parachute_module", p -> new MovementArmorModule(p, Kind.PARACHUTE));
        items.registerItem("water_walker_module", p -> new MovementArmorModule(p, Kind.WATER));
    }
    @Override public ResourceLocation category(ItemStack module) { return kind == Kind.PARACHUTE ? BACK : MOVEMENT; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) {
        return switch (kind) { case JUMP, WATER -> slot == EquipmentSlot.FEET; case SPEED -> slot == EquipmentSlot.LEGS; case STEP -> slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET; case PARACHUTE -> true; };
    }
    @Override public Set<ResourceLocation> features(ItemStack module, ItemStack armor) { return kind == Kind.STEP ? Set.of(STEP) : Set.of(); }
    @Override public void tickModule(ItemStack module, ItemStack armor, Player player) {
        var data = module.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        switch (kind) {
            case JUMP -> jump(armor, player, data);
            case SPEED -> speed(armor, player, data);
            case STEP -> step(armor, player, data);
            case PARACHUTE -> parachute(player, data);
            case WATER -> water(armor, player);
        }
        module.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    private static boolean canSpend(ItemStack armor, int amount) { return armor.getItem() instanceof ElectricItem power && power.charge(armor) >= amount; }
    private static void spend(ItemStack armor, int amount) { var power = (ElectricItem) armor.getItem(); power.setCharge(armor, Math.max(0, power.charge(armor) - amount)); }
    private static void jump(ItemStack armor, Player player, CompoundTag data) {
        float reserve = data.getFloat("jump_reserve");
        if (reserve < 1 && player.onGround() && canSpend(armor, 1000)) { reserve = 1; spend(armor, 1000); }
        var input = EquipmentControls.current(player);
        if (player.getDeltaMovement().y >= 0 && reserve > 0 && !player.isInWater()) {
            if (input.jump() && input.boost()) {
                boolean original = EquipmentConfig.ORIGINAL_QUANTUM.get(); double multiplier = reserve == 1 ? original ? 3.5 : 1.75 : 1;
                player.setDeltaMovement(player.getDeltaMovement().multiply(multiplier, 1, multiplier).add(0, reserve * (original ? .3 : .15), 0));
                reserve *= .75F; player.hurtMarked = true;
            } else if (reserve < 1) reserve = 0;
        }
        data.putFloat("jump_reserve", reserve);
    }
    private static void speed(ItemStack armor, Player player, CompoundTag data) {
        var input = EquipmentControls.current(player);
        if (!canSpend(armor, 1000) || !(player.isSprinting() || input.boost() && input.forward())) return;
        int ticks = data.getInt("speed_ticks") + 1; data.putInt("speed_ticks", ticks % 10);
        if (ticks % 10 == 0) spend(armor, 1000);
        float acceleration = player.isInWater() ? .1F : !player.onGround() && !player.isFallFlying() ? EquipmentConfig.ORIGINAL_QUANTUM.get() ? .25F : .025F : .22F;
        player.moveRelative(acceleration, new Vec3(0, 0, 1)); player.hurtMarked = true;
    }
    private static void step(ItemStack armor, Player player, CompoundTag data) {
        if (player.level().getGameTime() % 20 != 0) return;
        if (data.getDouble("step_y") < player.getY() && player.onGround() && canSpend(armor, 250)) spend(armor, 250);
        data.putDouble("step_y", player.getY());
    }
    private static void parachute(Player player, CompoundTag data) {
        boolean gliding = data.getBoolean("gliding"); var input = EquipmentControls.current(player); long now = player.level().getGameTime();
        boolean pressed = input.jump() && player.isShiftKeyDown();
        if (!gliding && pressed && !player.onGround() && player.getDeltaMovement().y < 0 && !player.isFallFlying() && !player.isInWater()) {
            data.putBoolean("gliding", true); data.putLong("last_press", now); return;
        }
        if (!gliding) return;
        if (player.onGround() || player.isInWater() || pressed && now - data.getLong("last_press") >= 5) { data.putBoolean("gliding", false); return; }
        var velocity = player.getDeltaMovement(); player.setDeltaMovement(velocity.x, Math.max(-.2, velocity.y), velocity.z);
        player.moveRelative(.02F, new Vec3(0, 0, 1)); player.fallDistance = 0; player.hurtMarked = true; resetFlight(player);
    }
    static void resetFlight(Player player) {
        if (player instanceof ServerPlayer server && server.connection instanceof FlightTimeAccess access) access.ic2$resetFlightTime();
    }
    private static void water(ItemStack armor, Player player) {
        var pos = player.blockPosition(); var level = player.level();
        if (player.isShiftKeyDown() || player.getDeltaMovement().y >= 0 || !canSpend(armor, 100) || !level.isEmptyBlock(pos)
            || Math.abs(player.getY() - pos.getY()) >= .1 || !level.containsAnyLiquid(new net.minecraft.world.phys.AABB(pos.below()))) return;
        if (level.getGameTime() % 10 == 0) spend(armor, 100);
        var velocity = player.getDeltaMovement(); player.setDeltaMovement(velocity.x, 0, velocity.z); player.setOnGround(true); player.hurtMarked = true; resetFlight(player);
        if (level instanceof net.minecraft.server.level.ServerLevel server) server.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP, player.getX(), player.getY() - .2, player.getZ(), 2, .2, 0, .2, .04);
    }
    static void refreshStep(Player player) {
        boolean enabled = false;
        for (var stack : player.getArmorSlots()) if (stack.getItem() instanceof ModularArmor armor && armor.features(stack).contains(STEP)) { enabled = true; break; }
        var attribute = player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) return;
        if (enabled && !attribute.hasModifier(STEP)) attribute.addTransientModifier(new AttributeModifier(STEP, .6, AttributeModifier.Operation.ADD_VALUE));
        else if (!enabled) attribute.removeModifier(STEP);
    }
}
