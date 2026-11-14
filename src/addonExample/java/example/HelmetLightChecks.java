package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class HelmetLightChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.light.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused helmet light checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var position = player.position(); boolean sneak = player.isShiftKeyDown();
        long time = level.getGameTime(); var data = (ServerLevelData) level.getLevelData();
        var pos = new BlockPos(470, 133, 470); var previous = level.getBlockState(pos);
        var armor = (ModularArmor) stack("nano_suit_helmet").getItem(); var helmet = armor.defaultArmor();
        require(armor.install(helmet, ArmorModule.GENERIC, stack("light_module")), "light installs in helmet");
        armor.setCharge(helmet, 100);
        var keys = new EquipmentInput(false, false, false, true, true, false);
        try {
            player.setPos(470.5, 132, 470.5); player.setShiftKeyDown(true); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            data.setGameTime(100); EquipmentControls.update(player, keys); tick(helmet, player);
            require(level.isEmptyBlock(pos), "toggling light affects emission on the following tick");
            data.setGameTime(105); EquipmentControls.clear(player); tick(helmet, player);
            require(level.getBlockState(pos).getLightEmission(level, pos) == 15 && armor.charge(helmet) == 90,
                "source brightness 15 costs 10 EU: " + level.getBlockState(pos) + ", charge=" + armor.charge(helmet) + ", data=" + helmet.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
            data.setGameTime(110); tick(helmet, player); tick(helmet, player);
            require(armor.charge(helmet) == 80, "renewal costs 10 EU once, with no duplicate charge");
            var light = level.getBlockState(pos).getBlock();
            light.defaultBlockState().tick(level, pos, level.random);
            require(level.getBlockState(pos).is(light), "renewed light survives first expiration");
            level.getBlockState(pos).tick(level, pos, level.random);
            require(level.isEmptyBlock(pos), "unrenewed light expires");
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            data.setGameTime(115); tick(helmet, player);
            require(level.getBlockState(pos).is(light) && level.getFluidState(pos).isSource(), "light works under water");
            level.getBlockState(pos).tick(level, pos, level.random);
            require(level.getBlockState(pos).is(Blocks.WATER), "expired underwater light restores water");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            data.setGameTime(120); EquipmentControls.update(player, keys); tick(helmet, player);
            data.setGameTime(125); EquipmentControls.update(player, keys); tick(helmet, player);
            data.setGameTime(130); EquipmentControls.clear(player); tick(helmet, player);
            require(level.getBlockState(pos).is(light), "holding controls toggles again after five ticks");
            var removed = armor.removeModule(helmet, 1, false);
            require(!removed.isEmpty() && armor.install(helmet, ArmorModule.GENERIC, removed), "light can be reinstalled");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); data.setGameTime(135); tick(helmet, player);
            require(level.isEmptyBlock(pos), "removing light clears enabled state on armor: " + level.getBlockState(pos)
                + ", module=" + removed + ", data=" + helmet.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
        } finally {
            level.setBlockAndUpdate(pos, previous); data.setGameTime(time);
            player.setPos(position); player.setShiftKeyDown(sneak); EquipmentControls.clear(player);
        }
    }
    private static void tick(ItemStack helmet, net.minecraft.world.entity.player.Player player) {
        ((ArmorTickItem) helmet.getItem()).tickArmor(helmet, player, EquipmentSlot.HEAD);
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
