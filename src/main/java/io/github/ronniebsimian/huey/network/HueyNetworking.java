package io.github.ronniebsimian.huey.network;

import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Only two small messages go from player to server. Flying itself needs no custom packets:
 * Minecraft already syncs vehicles that a player controls (the same way boats work).
 */
public final class HueyNetworking {
	private HueyNetworking() {
	}

	/** A door gunner started or stopped holding the trigger. */
	public record TriggerPayload(boolean firing) implements CustomPacketPayload {
		public static final Type<TriggerPayload> TYPE = new Type<>(HueyMod.id("trigger"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.BOOL, TriggerPayload::firing, TriggerPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Move to the next free seat. */
	public record SwitchSeatPayload() implements CustomPacketPayload {
		public static final SwitchSeatPayload INSTANCE = new SwitchSeatPayload();
		public static final Type<SwitchSeatPayload> TYPE = new Type<>(HueyMod.id("switch_seat"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SwitchSeatPayload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registerCommon() {
		PayloadTypeRegistry.playC2S().register(TriggerPayload.TYPE, TriggerPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SwitchSeatPayload.TYPE, SwitchSeatPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TriggerPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof HueyEntity huey) {
				huey.setTrigger(context.player(), payload.firing());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SwitchSeatPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof HueyEntity huey) {
				huey.switchSeat(context.player());
			}
		});
	}
}
