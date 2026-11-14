package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

final class ElectricArmorChecks {
    static void run(ServerLevel level) throws Exception {
        var source = level.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("ic2:electricity")));
        var helmet = helmet(); var armor = (ModularArmor) helmet.getItem(); var wearer = mob(level, helmet);
        wearer.hurt(source, 10);
        require(wearer.getHealth() == 20 && armor.charge(helmet) == 10, "source special electrical protection absorbs the hit and stores ten EU in an empty crystal: health=" + wearer.getHealth() + ", EU=" + armor.charge(helmet));
        helmet = helmet(); armor = (ModularArmor) helmet.getItem(); armor.setCharge(helmet, 100000); wearer = mob(level, helmet); wearer.hurt(source, 10);
        require(wearer.getHealth() == 20 && armor.charge(helmet) == 92000, "full ungrounded electrical armor spends its source wear energy instead of overcharging");
        var pos = new BlockPos(790, 99, 790); var original = level.getBlockState(pos);
        try {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2 | 16);
            helmet = helmet(); armor = (ModularArmor) helmet.getItem(); armor.setCharge(helmet, 100000); wearer = mob(level, helmet);
            wearer.setItemSlot(EquipmentSlot.FEET, stack("nano_suit_boots")); wearer.hurt(source, 10);
            require(wearer.getHealth() == 20 && armor.charge(helmet) == 100000, "source grounded electric boots avoid full-armor electrical wear");
        } finally { level.setBlock(pos, original, 2 | 16); }
        var config = Class.forName("ic2.core.equipment.EquipmentConfig").getDeclaredField("SPECIAL_ELECTRIC"); config.setAccessible(true);
        var special = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) config.get(null); boolean previous = special.get();
        try {
            special.set(false); helmet = helmet(); armor = (ModularArmor) helmet.getItem(); wearer = mob(level, helmet); wearer.hurt(source, 10);
            require(wearer.getHealth() < 20 && armor.charge(helmet) == 0, "disabling source special protection restores normal affordable absorption without electrical charging");
        } finally { special.set(previous); }
        var absorptionField = Class.forName("ic2.core.equipment.EquipmentConfig").getDeclaredField("ARMOR_ABSORPTION"); absorptionField.setAccessible(true);
        var costField = Class.forName("ic2.core.equipment.EquipmentConfig").getDeclaredField("ARMOR_ENERGY_COST"); costField.setAccessible(true);
        var absorption = (net.neoforged.neoforge.common.ModConfigSpec.DoubleValue) absorptionField.get(null);
        var cost = (net.neoforged.neoforge.common.ModConfigSpec.DoubleValue) costField.get(null); double oldAbsorption = absorption.get(), oldCost = cost.get();
        try {
            absorption.set(2.0); cost.set(2.0); helmet = helmet(); armor = (ModularArmor) helmet.getItem(); armor.setCharge(helmet, 3200); wearer = mob(level, helmet);
            var properties = ((ProtectiveArmor) armor).absorption(helmet, wearer, wearer.damageSources().generic(), 10, EquipmentSlot.HEAD);
            require(Math.abs(properties.ratio() - .27) < .00001 && properties.maximum() == 2 && armor.charge(helmet) == 3200, "source absorption and energy cost configuration change the pure protection descriptor");
            wearer.hurt(wearer.damageSources().generic(), 10);
            require(wearer.getHealth() == 18 && armor.charge(helmet) == 0, "configured protection caps real absorbed damage by affordable EU and spends configured wear");
        } finally { absorption.set(oldAbsorption); cost.set(oldCost); }
    }
    private static net.minecraft.world.entity.animal.Cow mob(ServerLevel level, ItemStack helmet) {
        var mob = net.minecraft.world.entity.EntityType.COW.create(level); mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);
        mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(790.5, 100, 790.5); mob.setItemSlot(EquipmentSlot.HEAD, helmet); mob.tick(); mob.setHealth(20); return mob;
    }
    private static ItemStack helmet() { var shell = stack("nano_suit_helmet"); return ((ModularArmor) shell.getItem()).defaultArmor(); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
