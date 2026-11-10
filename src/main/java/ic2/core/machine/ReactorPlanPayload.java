package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

record ReactorPlanPayload(int menuId, int format, String code) implements CustomPacketPayload {
    private static final Type<ReactorPlanPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "reactor_plan"));

    @Override
    public Type<ReactorPlanPayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(TYPE, StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReactorPlanPayload::menuId,
            ByteBufCodecs.VAR_INT, ReactorPlanPayload::format,
            ByteBufCodecs.stringUtf8(ReactorPlanCode.MAX_LENGTH), ReactorPlanPayload::code,
            ReactorPlanPayload::new), ReactorPlanPayload::handle);
    }

    private static void handle(ReactorPlanPayload payload, IPayloadContext context) {
        var player = context.player();
        if (!(player.containerMenu instanceof ReactorPlannerMenu menu) || menu.containerId != payload.menuId()) {
            return;
        }
        if (player.level().isClientSide()) {
            ReactorPlannerScreen.copyCode(payload.code());
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !menu.stillValid(player)) {
            return;
        }
        try {
            if (payload.code().isEmpty()) {
                var formats = ReactorPlanCode.Format.values();
                if (payload.format() < 0 || payload.format() >= formats.length) {
                    return;
                }
                PacketDistributor.sendToPlayer(serverPlayer,
                    new ReactorPlanPayload(menu.containerId, payload.format(), menu.exportCode(formats[payload.format()])));
            } else {
                menu.importCode(payload.code());
                player.displayClientMessage(Component.translatable("gui.ic2.reactor_planner.code_imported"), true);
            }
        } catch (IllegalArgumentException exception) {
            player.displayClientMessage(Component.translatable("gui.ic2.reactor_planner.code_invalid"), true);
        }
    }
}
