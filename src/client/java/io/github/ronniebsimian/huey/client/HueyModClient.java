package io.github.ronniebsimian.huey.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import io.github.ronniebsimian.huey.network.HueyNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Input;

public class HueyModClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HueyMod.id("huey"));
	public static final KeyMapping SWITCH_SEAT = new KeyMapping("key.huey.switch_seat", InputConstants.KEY_G, CATEGORY);

	private static boolean triggerSent;
	private static int lastSeat = -1;

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(HueyModel.LAYER, HueyModelGeometry::create);
		EntityRenderers.register(HueyMod.HUEY, HueyRenderer::new);
		KeyBindingHelper.registerKeyBinding(SWITCH_SEAT);

		HueyEntity.clientInput = () -> {
			LocalPlayer player = Minecraft.getInstance().player;
			return player != null ? player.input.keyPresses : Input.EMPTY;
		};
		HueyEntity.clientSpawnHook = huey -> Minecraft.getInstance().getSoundManager().play(new HueyRotorSound(huey));

		ClientTickEvents.START_CLIENT_TICK.register(HueyModClient::tick);
	}

	/** True while the local player is manning a door gun (the attack button fires instead of punching). */
	public static boolean isManningGun() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.getVehicle() instanceof HueyEntity huey && HueyEntity.isGunnerSeat(huey.getSeatOf(player));
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		HueyEntity huey = player != null && player.getVehicle() instanceof HueyEntity h ? h : null;

		// door gun trigger: hold the attack button (left mouse)
		boolean firing = huey != null && isManningGun() && mc.screen == null && mc.options.keyAttack.isDown();
		if (firing != triggerSent && mc.getConnection() != null) {
			ClientPlayNetworking.send(new HueyNetworking.TriggerPayload(firing));
			triggerSent = firing;
		}

		while (SWITCH_SEAT.consumeClick()) {
			if (huey != null) {
				ClientPlayNetworking.send(HueyNetworking.SwitchSeatPayload.INSTANCE);
			}
		}

		if (huey == null) {
			lastSeat = -1;
			return;
		}
		int seat = huey.getSeatOf(player);
		if (seat != lastSeat || player.tickCount % 10 == 0) {
			player.displayClientMessage(hud(mc, huey, seat, seat != lastSeat), true);
			lastSeat = seat;
		}
	}

	/** Action-bar text: controls when you first sit down, then live gauges. */
	private static Component hud(Minecraft mc, HueyEntity huey, int seat, boolean justSat) {
		if (seat < 0) {
			return Component.empty();
		}
		String seatName = HueyEntity.SEATS[seat].name();
		if (justSat) {
			return Component.translatable("huey.hud.seat." + seatName)
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal("  "))
				.append(Component.translatable("huey.hud.help." + seatName,
					mc.options.keyUp.getTranslatedKeyMessage(), mc.options.keyDown.getTranslatedKeyMessage(),
					mc.options.keyLeft.getTranslatedKeyMessage(), mc.options.keyRight.getTranslatedKeyMessage(),
					mc.options.keyJump.getTranslatedKeyMessage(), mc.options.keySprint.getTranslatedKeyMessage(),
					mc.options.keyShift.getTranslatedKeyMessage(), SWITCH_SEAT.getTranslatedKeyMessage(),
					mc.options.keyAttack.getTranslatedKeyMessage())
					.withStyle(ChatFormatting.GRAY));
		}
		if (seat == HueyEntity.PILOT || seat == HueyEntity.COPILOT) {
			int rotor = Math.round(huey.getRotorSpeed() * 100);
			int alt = (int) Math.floor(huey.getY());
			int kmh = (int) Math.round(Math.hypot(huey.getX() - huey.xo, huey.getZ() - huey.zo) * 20 * 3.6); // blocks/tick -> km/h
			int hp = Math.round(huey.getHealth());
			MutableComponent rpm = Component.translatable("huey.hud.rotor", rotor)
				.withStyle(rotor < 100 ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
			return rpm.append(Component.translatable("huey.hud.flight", alt, kmh, hp, Math.round(HueyEntity.MAX_HEALTH)).withStyle(ChatFormatting.WHITE));
		}
		if (HueyEntity.isGunnerSeat(seat)) {
			float heat = huey.getGunHeat(seat);
			int bars = Math.round(heat / 10.0F);
			String gauge = "|".repeat(bars) + ".".repeat(10 - bars);
			ChatFormatting color = huey.isOverheated(seat) ? ChatFormatting.RED : heat > 60 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
			return Component.translatable(huey.isOverheated(seat) ? "huey.hud.overheat" : "huey.hud.heat", gauge).withStyle(color);
		}
		return Component.translatable("huey.hud.seat." + seatName).withStyle(ChatFormatting.GOLD);
	}
}
