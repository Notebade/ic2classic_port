package ic2.core.equipment;

import ic2.core.mixin.CarbonBoatAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class CarbonBoats {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "ic2");
    static final java.util.function.Supplier<EntityType<CarbonBoat>> TYPE = ENTITIES.register("carbon_boat", () -> EntityType.Builder.<CarbonBoat>of(CarbonBoat::new, MobCategory.MISC).sized(1.375F, .5625F).clientTrackingRange(10).build("ic2:carbon_boat"));
    private static java.util.function.Supplier<Item> item;
    static void register(IEventBus bus, DeferredRegister.Items items) { ENTITIES.register(bus); item = items.registerItem("carbon_boat", CarbonBoatItem::new); }
    static final class CarbonBoat extends Boat {
        CarbonBoat(EntityType<? extends Boat> type, Level level) { super(type, level); setVariant(Boat.Type.OAK); }
        @Override public Item getDropItem() { return item.get(); }
        @Override public void onInsideBubbleColumn(boolean down) {}
        @Override public void onAboveBubbleCol(boolean down) {}
        @Override protected void checkFallDamage(double y, boolean grounded, BlockState state, BlockPos pos) {
            ((CarbonBoatAccess) (Boat) this).ic2$setLastYDelta(getDeltaMovement().y);
            if (grounded) fallDistance = 0;
            else if (!level().getFluidState(blockPosition().below()).is(FluidTags.WATER) && y < 0) fallDistance -= (float) y;
        }
    }
    private static final class CarbonBoatItem extends Item {
        CarbonBoatItem(Properties properties) { super(properties.stacksTo(1)); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand); var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
            if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
            var sight = player.getBoundingBox().expandTowards(player.getViewVector(1).scale(5)).inflate(1);
            for (var entity : level.getEntities(player, sight, EntitySelector.NO_SPECTATORS.and(e -> e.isPickable()))) {
                if (entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition())) return InteractionResultHolder.pass(stack);
            }
            var boat = TYPE.get().create(level); boat.setPos(hit.getLocation()); boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox().inflate(-.1))) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide) {
                if (!level.addFreshEntity(boat)) return InteractionResultHolder.fail(stack);
                stack.consume(1, player); level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, hit.getLocation());
                player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
