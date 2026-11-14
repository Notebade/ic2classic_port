package ic2.core.equipment;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FoodCans extends Item {
    private final boolean chorus;

    private FoodCans(Properties properties, boolean chorus) {
        super(properties);
        this.chorus = chorus;
    }

    static void register(DeferredRegister.Items items) {
        var empty = items.registerSimpleItem("tin_can");
        for (String variant : new String[]{"", "_bad", "_poison", "_chicken", "_golden_apple", "_enchanted_golden_apple", "_chorus_fruit", "_pufferfish"}) {
            items.registerItem("filled_tin_can" + variant, p -> {
                var food = new FoodProperties.Builder().nutrition(2).saturationModifier(.95f).alwaysEdible().fast()
                    .usingConvertsTo(empty.get());
                switch (variant) {
                    case "_bad" -> food.effect(new MobEffectInstance(MobEffects.CONFUSION, 150), .8f)
                        .effect(new MobEffectInstance(MobEffects.HUNGER, 150), .8f);
                    case "_poison" -> food.effect(new MobEffectInstance(MobEffects.POISON, 50), 1);
                    case "_chicken" -> food.effect(new MobEffectInstance(MobEffects.HUNGER, 300), .3f);
                    case "_golden_apple" -> food.effect(new MobEffectInstance(MobEffects.REGENERATION, 25, 1), 1)
                        .effect(new MobEffectInstance(MobEffects.ABSORPTION, 600), 1);
                    case "_enchanted_golden_apple" -> food.effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1)
                        .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1500), 1)
                        .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1500), 1)
                        .effect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3), 1);
                    case "_pufferfish" -> food.effect(new MobEffectInstance(MobEffects.POISON, 1200, 3), 1)
                        .effect(new MobEffectInstance(MobEffects.HUNGER, 300, 2), 1)
                        .effect(new MobEffectInstance(MobEffects.CONFUSION, 300, 1), 1);
                    default -> { }
                }
                return new FoodCans(p.food(food.build()), variant.equals("_chorus_fruit"));
            });
        }
    }

    public static String outputId(ItemStack input) {
        if (input.is(Items.ROTTEN_FLESH)) return "filled_tin_can_bad";
        if (input.is(Items.SPIDER_EYE) || input.is(Items.POISONOUS_POTATO)) return "filled_tin_can_poison";
        if (input.is(Items.CHICKEN)) return "filled_tin_can_chicken";
        if (input.is(Items.GOLDEN_APPLE)) return "filled_tin_can_golden_apple";
        if (input.is(Items.ENCHANTED_GOLDEN_APPLE)) return "filled_tin_can_enchanted_golden_apple";
        if (input.is(Items.CHORUS_FRUIT)) return "filled_tin_can_chorus_fruit";
        if (input.is(Items.PUFFERFISH)) return "filled_tin_can_pufferfish";
        return "filled_tin_can";
    }

    @Override public String getDescriptionId() {
        return "item.ic2.filled_tin_can";
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        var result = super.finishUsingItem(stack, level, eater);
        if (!level.isClientSide && eater instanceof Player player) {
            player.heal(2);
            if (chorus) teleport(level, player);
        }
        return result;
    }

    private static void teleport(Level level, Player player) {
        var origin = player.position();
        var random = player.getRandom();
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = origin.x + (random.nextDouble() - .5) * 16;
            double y = net.minecraft.util.Mth.clamp(origin.y + random.nextInt(16) - 8,
                level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            double z = origin.z + (random.nextDouble() - .5) * 16;
            if (player.isVehicle()) player.ejectPassengers();
            var event = net.neoforged.neoforge.event.EventHooks.onChorusFruitTeleport(player, x, y, z);
            if (event.isCanceled()) return;
            if (player.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
                var sound = net.minecraft.sounds.SoundEvents.CHORUS_FRUIT_TELEPORT;
                level.playSound(null, origin.x, origin.y, origin.z, sound, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
                player.playSound(sound, 1, 1);
                return;
            }
        }
    }
}
