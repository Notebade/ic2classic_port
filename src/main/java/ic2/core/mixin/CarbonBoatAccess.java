package ic2.core.mixin;

import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Boat.class)
public interface CarbonBoatAccess {
    @Accessor("lastYd") void ic2$setLastYDelta(double value);
}
