package ic2.core.equipment;

import ic2.api.item.ElectricEquipment;
import ic2.core.item.RechargeableItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

final class NanoSaber extends RechargeableItem implements ic2.api.item.ElectricEnchantable {
    private static final ItemAttributeModifiers OFF = attributes(3);
    private static final ItemAttributeModifiers ON = attributes(19);
    static Runnable miningSound = () -> {};
    static java.util.function.BooleanSupplier clientAlt = () -> false;
    private static final String ACTIVE = "ic2_saber_active";
    private static final String DELAY = "ic2_saber_delay";

    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, "ic2");
    private static final java.util.List<String> SOUND_NAMES = java.util.List.of("powerup", "swing_1", "swing_2", "swing_3", "idle");
    private static final java.util.List<? extends java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> EVENTS =
        SOUND_NAMES.stream().map(name -> SOUNDS.register("tools.nanosaber.nanosaber_" + name,
            net.minecraft.sounds.SoundEvent::createVariableRangeEvent)).toList();

    static void register(net.neoforged.bus.api.IEventBus bus, net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        SOUNDS.register(bus);
        items.registerItem("nano_saber", NanoSaber::new);
    }

    NanoSaber(Properties properties) {
        super(properties.stacksTo(1).attributes(OFF), 40_000, 128, 128);
    }

    private static ItemAttributeModifiers attributes(int damage) {
        return ItemAttributeModifiers.builder().add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build();
    }

    static net.minecraft.sounds.SoundEvent soundEvent(int index) { return EVENTS.get(index).get(); }

    static boolean active(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(ACTIVE);
    }

    private void setActive(ItemStack stack, boolean active) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(ACTIVE, active);
        });
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, active ? ON : OFF);
    }

    @Override public boolean canProvideEnergy() { return false; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        boolean enabled = active(stack);
        boolean alt = level.isClientSide ? clientAlt.getAsBoolean() : ic2.api.item.EquipmentControls.current(player).alt();
        if (enabled ? !alt : !ElectricEquipment.canUse(stack, 16)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            setActive(stack, !enabled);
            if (active(stack)) sound(player, 0);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ability == ItemAbilities.SWORD_SWEEP && active(stack);
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide || !active(stack)) return true;
        if (attacker instanceof Player && !attacker.level().getServer().isPvpAllowed()) return true;
        if (target instanceof Player player) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) continue;
                var armor = player.getItemBySlot(slot);
                if (!(armor.getItem() instanceof ic2.api.item.ModularArmor electric)) continue;
                int tier = (Integer.numberOfTrailingZeros(Math.max(32, electric.minimumPacket(armor))) - 5) / 2 + 1;
                int use = (int) (3000 * Math.pow(6.25, tier - 1));
                electric.setCharge(armor, Math.max(0, electric.charge(armor) - use));
                if (electric.charge(armor) == 0) player.setItemSlot(slot, ItemStack.EMPTY);
                drain(stack, attacker, 16);
            }
        }
        drain(stack, attacker, 40);
        return true;
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || !active(stack)) return;
        int ticks = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(DELAY) + 1;
        boolean due = ticks >= (selected ? 16 : 64);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(DELAY, due ? 0 : ticks));
        if (due) drain(stack, player, 16);
    }

    private static void sound(LivingEntity owner, int index) {
        owner.level().playSound(null, owner.blockPosition(), EVENTS.get(index).get(), net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
    }

    private void drain(ItemStack stack, LivingEntity owner, int cost) {
        if (!ElectricEquipment.use(stack, cost, owner)) setActive(stack, false);
    }

    @Override public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player) { return !player.isCreative(); }

    @Override public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (!active(stack)) return 1;
        miningSound.run();
        return state.is(net.minecraft.world.level.block.Blocks.COBWEB) ? 15 : state.is(net.minecraft.tags.BlockTags.SWORD_EFFICIENT) ? 1.5F : 1;
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.COBWEB);
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0) ElectricEquipment.use(stack, 10, owner);
        return true;
    }

    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || active(oldStack) != active(newStack);
    }
    @Override public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }
    @Override public int electricEnchantability(ItemStack stack) { return 14; }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
        return !enchantment.is(net.minecraft.world.item.enchantment.Enchantments.MENDING)
            && (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)
                || enchantment.value().isSupportedItem(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD)));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("key.mouse.right").append(": ")
            .append(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.nano_saber.enable")));
        tooltip.add(net.minecraft.network.chat.Component.literal("Alt + ").append(net.minecraft.network.chat.Component.translatable("key.mouse.right")).append(": ")
            .append(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.nano_saber.disable")));
    }
}
