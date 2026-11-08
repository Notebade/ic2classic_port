package ic2.core.item;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class ScrapBoxItem extends Item {
    private static final ResourceKey<LootTable> CONTENTS = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "items/scrapbox"));

    public ScrapBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack boxes = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            List<ItemStack> drops = open(server, player.position());
            if (!drops.isEmpty()) {
                boxes.consume(1, player);
                for (ItemStack drop : drops) {
                    player.drop(drop, false);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(boxes, level.isClientSide);
    }

    private static List<ItemStack> open(ServerLevel level, Vec3 origin) {
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
            .create(LootContextParamSets.CHEST);
        return level.getServer().reloadableRegistries().getLootTable(CONTENTS).getRandomItems(params);
    }

    public static void registerDispenser() {
        DispenserBlock.registerBehavior(IC2.SCRAP_BOX.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack boxes) {
                List<ItemStack> drops = open(source.level(), source.center());
                if (!drops.isEmpty()) {
                    boxes.shrink(1);
                    var direction = source.state().getValue(DispenserBlock.FACING);
                    var position = DispenserBlock.getDispensePosition(source);
                    for (ItemStack drop : drops) {
                        spawnItem(source.level(), drop, 6, direction, position);
                    }
                }
                return boxes;
            }
        });
    }
}
