package ic2.core.mixin;

import ic2.core.machine.IronFurnaceBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class FurnaceCookTimeMixin {
    @Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
    private static void ic2$ironCookTime(Level level, AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Integer> callback) {
        if (furnace instanceof IronFurnaceBlockEntity) {
            callback.setReturnValue(Math.max(1, (int) (callback.getReturnValueI() * 4L / 5)));
        }
    }
}
