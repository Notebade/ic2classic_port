package ic2.core.mixin;

import ic2.core.equipment.FlightTimeAccess;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class EquipmentFlightMixin implements FlightTimeAccess {
    @Shadow private int aboveGroundTickCount;
    @Override public void ic2$resetFlightTime() { aboveGroundTickCount = 0; }
}
