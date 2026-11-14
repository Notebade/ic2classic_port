package ic2.core.equipment;

import ic2.api.item.EquipmentControls;
import ic2.api.item.EquipmentInput;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

record EquipmentInputPayload(int mask) implements CustomPacketPayload {
    private static final Type<EquipmentInputPayload> TYPE = new Type<>(ResourceLocation.parse("ic2:equipment_input"));
    @Override public Type<EquipmentInputPayload> type() { return TYPE; }
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, StreamCodec.composite(ByteBufCodecs.VAR_INT, EquipmentInputPayload::mask, EquipmentInputPayload::new),
            (payload, context) -> { if ((payload.mask & ~EquipmentInput.KNOWN_MASK) == 0) EquipmentControls.update(context.player(), EquipmentInput.fromMask(payload.mask)); });
    }
    static void send(EquipmentInput input) { PacketDistributor.sendToServer(new EquipmentInputPayload(input.mask())); }
}
