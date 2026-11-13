package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

final class EnchanterChecks {
    private static final BlockPos POS = new BlockPos(288, 300, 288);
    private EnchanterChecks() {}

    static void run(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previousPosition = player.position();
        var previousMode = player.gameMode.getGameModeForPlayer();
        int previousXp = player.totalExperience, previousLevel = player.experienceLevel;
        float previousProgress = player.experienceProgress;
        level.getChunkAt(POS);
        try {
            player.setGameMode(GameType.SURVIVAL); player.setPos(POS.getCenter());
            player.totalExperience = 0; player.experienceLevel = 0; player.experienceProgress = 0;
            player.giveExperiencePoints(1000);
            for (int y = 0; y < 2; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                var pos = POS.offset(x, y, z); level.getChunkAt(pos);
                if (x != 0 || z != 0) level.setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
            }
            place(level);
            var entity = level.getBlockEntity(POS);
            var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            var inputs = inputs(level);
            inputs.insertItem(1, new ItemStack(Items.LAPIS_LAZULI, 3), false);
            for (int slot = 2; slot < 5; slot++) inputs.insertItem(slot, new ItemStack(Items.BOOK), false);
            inputs.insertItem(5, new ItemStack(Items.DIAMOND_PICKAXE), false);
            ApiSmokeChecks.tick(level, POS, entity.getType());
            menu.clickMenuButton(player, 7);
            require(value(level, "running") == 0, "starting without XP never creates a negative ledger");
            for (int transfer = 0; transfer < 10; transfer++) menu.clickMenuButton(player, 3);
            require(value(level, "stored_xp") == 1000 && player.totalExperience == 0, "deposit transfers raw player XP, not levels");
            menu.clickMenuButton(player, 3);
            require(value(level, "stored_xp") == 1000, "internal XP capacity is 1000");
            for (int packet = 0; packet < 1000; packet++) ((EnergySink) entity).receiveEnergy(512);
            menu.clickMenuButton(player, 7);
            require(value(level, "running") == 1 && value(level, "required_xp") == 248, "source level-30 enchant cost is reserved");
            menu.clickMenuButton(player, 6);
            require(value(level, "stored_xp") == 1000 && player.totalExperience == 0, "running operation blocks XP withdrawal");
            menu.broadcastChanges();
            var preview = menu.slots.get(43).getItem().copy();
            require(preview.is(Items.DIAMOND_PICKAXE) && preview.isEnchanted() && !menu.slots.get(43).mayPickup(player), "preview is synchronized and cannot be taken");
            for (int tick = 0; tick < 50; tick++) ApiSmokeChecks.tick(level, POS, entity.getType());
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            var staleInput = inputs;
            var staleSink = (EnergySink) entity;
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState()); place(level);
            entity = level.getBlockEntity(POS); entity.loadWithComponents(saved, level.registryAccess());
            require(staleInput.getSlots() == 0 && staleSink.receiveEnergy(512) == 0, "removed enchanter capabilities cannot mutate old inventory/energy");
            menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            for (int tick = 0; tick < 50; tick++) ApiSmokeChecks.tick(level, POS, entity.getType());
            var output = outputs(level).extractItem(0, 1, false);
            require(ItemStack.isSameItemSameComponents(preview, output), "saved seed and progress produce the exact preview after reload");
            require(value(level, "stored_xp") == 752 && Ic2Api.machineInfo(level, POS).orElseThrow().energyStored() == 462000, "enchant completes with exactly 248 XP and 50000 EU");
            inputs = inputs(level);
            require(inputs.getStackInSlot(1).isEmpty() && inputs.getStackInSlot(2).isEmpty() && inputs.getStackInSlot(5).isEmpty(), "enchant consumes target, three lapis and three books");

            var sharpness = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
            var sword = new ItemStack(Items.DIAMOND_SWORD); sword.enchant(sharpness, 1);
            var supplied = new ItemStack(Items.ENCHANTED_BOOK); supplied.enchant(sharpness, 1);
            inputs.insertItem(5, sword, false); inputs.insertItem(2, supplied, false);
            menu.clickMenuButton(player, 0); menu.clickMenuButton(player, 7);
            require(value(level, "required_xp") == 20 && value(level, "running") == 1, "anvil cost counts supplied and existing enchantments");
            for (int tick = 0; tick < 100; tick++) ApiSmokeChecks.tick(level, POS, entity.getType());
            output = outputs(level).extractItem(0, 1, false);
            require(EnchantmentHelper.getEnchantmentsForCrafting(output).getLevel(sharpness) == 2 && value(level, "stored_xp") == 732, "equal levels combine and consume exact anvil XP");

            inputs.insertItem(5, output, false); inputs.insertItem(2, new ItemStack(Items.BOOK), false);
            menu.clickMenuButton(player, 0); menu.clickMenuButton(player, 7);
            require(value(level, "required_xp") == 50 && value(level, "progress_target") == 250, "disenchant uses source 50 XP and 250 ticks");
            for (int tick = 0; tick < 250; tick++) ApiSmokeChecks.tick(level, POS, entity.getType());
            output = outputs(level).extractItem(0, 1, false);
            require(output.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(output).getLevel(sharpness) == 2, "removed enchantment transfers to a book");
            require(inputs.getStackInSlot(5).is(Items.DIAMOND_SWORD) && !inputs.getStackInSlot(5).isEnchanted() && value(level, "stored_xp") == 682, "disenchant preserves the item and consumes 50 XP");
            menu.clickMenuButton(player, 6);
            require(value(level, "stored_xp") == 582 && player.totalExperience == 100, "idle withdrawal returns raw XP");
            menu.clickMenuButton(player, 3);
            require(value(level, "stored_xp") == 682 && player.totalExperience == 0, "raw XP round trip conserves both ledgers");

            menu.slots.get(5).setByPlayer(ItemStack.EMPTY);
            menu.slots.get(5).safeInsert(new ItemStack(ExampleAddon.ELECTRIC_TEST_ITEM.get()));
            for (int slot = 2; slot < 5; slot++) inputs.insertItem(slot, new ItemStack(Items.BOOK), false);
            inputs.insertItem(1, new ItemStack(Items.LAPIS_LAZULI, 3), false);
            menu.clickMenuButton(player, 0); menu.clickMenuButton(player, 7);
            for (int tick = 0; tick < 100; tick++) ApiSmokeChecks.tick(level, POS, entity.getType());
            output = outputs(level).extractItem(0, 1, false);
            var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(output);
            require(output.is(ExampleAddon.ELECTRIC_TEST_ITEM.get()) && enchantments.size() == 1
                && enchantments.getLevel(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.EFFICIENCY)) > 0,
                "addon ElectricEnchantable contract enables exactly the enchantment selected by the addon");
            com.mojang.logging.LogUtils.getLogger().info("IC2 electric enchanter production checks passed: all modes, XP conservation, reload, preview and addon item contract");
        } finally {
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            for (int y = 0; y < 2; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                if (x != 0 || z != 0) level.setBlockAndUpdate(POS.offset(x, y, z), Blocks.AIR.defaultBlockState());
            player.setGameMode(previousMode); player.setPos(previousPosition);
            player.totalExperience = previousXp; player.experienceLevel = previousLevel; player.experienceProgress = previousProgress;
        }
    }
    private static void place(ServerLevel level) { level.setBlockAndUpdate(POS, Ic2Api.block(ResourceLocation.parse("ic2:electric_enchanter")).orElseThrow().defaultBlockState()); }
    private static IItemHandler inputs(ServerLevel level) { return level.getCapability(Capabilities.ItemHandler.BLOCK, POS, Direction.UP); }
    private static IItemHandler outputs(ServerLevel level) { return level.getCapability(Capabilities.ItemHandler.BLOCK, POS, Direction.DOWN); }
    private static long value(ServerLevel level, String key) { return Ic2Api.machineInfo(level, POS).orElseThrow().values().get(key); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
