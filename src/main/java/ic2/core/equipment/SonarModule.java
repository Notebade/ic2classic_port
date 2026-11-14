package ic2.core.equipment;

import ic2.api.item.ArmorModule;
import ic2.api.item.ElectricEquipment;
import ic2.api.item.EquipmentControls;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

final class SonarModule extends Item implements ArmorModule {
    SonarModule(Properties properties) { super(properties); }
    @Override public ResourceLocation category(ItemStack module) { return HUD; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) { return slot == EquipmentSlot.HEAD; }

    @Override public void tickModule(ItemStack module, ItemStack armor, Player wearer) {
        var input = EquipmentControls.current(wearer);
        boolean pressed = input.hud() && input.alt();
        var tag = module.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long time = wearer.level().getGameTime();
        if (pressed && wearer.isShiftKeyDown() && time >= tag.getLong("last_toggle")) {
            tag.putLong("last_toggle", time + 20);
            tag.putBoolean("scan", !tag.getBoolean("scan"));
            wearer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "tooltip.item.ic2.sonar_module." + (tag.getBoolean("scan") ? "enable" : "disable")), false);
        } else if (pressed && !wearer.isShiftKeyDown() && !tag.contains("positions") && ElectricEquipment.use(armor, 20_000, wearer)) {
            var origin = wearer.blockPosition();
            tag.putIntArray("positions", wearer.level().getEntitiesOfClass(LivingEntity.class, new AABB(origin).inflate(250))
                .stream().mapToInt(LivingEntity::getId).toArray());
            tag.putLong("pos", origin.asLong());
            tag.putDouble("progress", 0);
            tag.putLong("started", time);
        }
        if (tag.getBoolean("scan") && time % 20 == 0) highlight(wearer, 25, 30);
        if (tag.contains("positions")) advanceWave(wearer, tag);
        module.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void advanceWave(Player wearer, CompoundTag tag) {
        double progress = tag.getDouble("progress");
        var bounds = new AABB(BlockPos.of(tag.getLong("pos"))).inflate(250 * progress);
        for (int id : tag.getIntArray("positions")) {
            if (wearer.level().getEntity(id) instanceof LivingEntity target && target != wearer && bounds.intersects(target.getBoundingBox())) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
            }
        }
        if (progress >= 1) {
            tag.remove("positions");
            tag.remove("progress");
        } else tag.putDouble("progress", progress + .005);
    }

    private static void highlight(Player wearer, int radius, int duration) {
        for (var target : wearer.level().getEntitiesOfClass(LivingEntity.class, new AABB(wearer.blockPosition()).inflate(radius),
                entity -> entity != wearer)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false));
        }
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.ic2.sonar.controls"));
    }
}
