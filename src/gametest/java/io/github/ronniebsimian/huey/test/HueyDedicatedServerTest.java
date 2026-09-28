package io.github.ronniebsimian.huey.test;

import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Same idea as the singleplayer test, but against a real dedicated server with flying NOT allowed
 * (the default in server.properties). Proves the pilot isn't kicked, the server accepts the
 * movement, and nothing client-only leaks into the server.
 */
public class HueyDedicatedServerTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		Properties props = new Properties();
		props.setProperty("allow-flight", "false");
		try (TestDedicatedServerContext server = context.worldBuilder().createServer(props);
			TestDedicatedServerConnection connection = server.connect()) {
			System.out.println("[HUEY MP TEST] connected");
			connection.waitForChunksRender();
			System.out.println("[HUEY MP TEST] chunks rendered");
			int hueyId = server.computeOnServer(mc -> {
				ServerPlayer player = mc.getPlayerList().getPlayers().get(0);
				ServerLevel level = player.level();
				int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
				HueyEntity huey = HueyMod.HUEY.create(level, EntitySpawnReason.COMMAND);
				huey.snapTo(player.getX() + 4, ground, player.getZ(), 0.0F, 0.0F);
				level.addFreshEntity(huey);
				return huey.getId();
			});
			System.out.println("[HUEY MP TEST] spawned " + hueyId);
			context.waitFor(mc -> mc.level != null && mc.level.getEntity(hueyId) != null);
			System.out.println("[HUEY MP TEST] client sees huey");
			double startY = server.computeOnServer(mc -> mc.overworld().getEntity(hueyId).getY());
			server.runOnServer(mc -> mc.getPlayerList().getPlayers().get(0).startRiding(mc.overworld().getEntity(hueyId)));
			context.waitTicks(5);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(85);
			// climb, then hold a hover for well over the 80 ticks vanilla allows a "floating" vehicle
			context.getInput().holdKeyFor(options -> options.keyJump, 40);
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(30);
			context.getInput().releaseKey(options -> options.keyUp);
			context.waitTicks(160);
			context.takeScreenshot("mp_01_hovering_on_dedicated_server");

			boolean stillConnected = context.computeOnClient(mc -> mc.getConnection() != null && mc.player != null && mc.player.getVehicle() instanceof HueyEntity);
			double serverY = server.computeOnServer(mc -> mc.overworld().getEntity(hueyId).getY());
			double clientY = context.computeOnClient(mc -> mc.level.getEntity(hueyId).getY());
			System.out.println("[HUEY MP TEST] connected=" + stillConnected + " startY=" + startY + " serverY=" + serverY + " clientY=" + clientY);
			if (!stillConnected) {
				throw new AssertionError("pilot was disconnected or dismounted on the dedicated server");
			}
			if (serverY - startY < 5.0) {
				throw new AssertionError("server did not accept the climb: " + startY + " -> " + serverY);
			}
			if (Math.abs(serverY - clientY) > 1.0) {
				throw new AssertionError("client and server disagree on altitude: " + clientY + " vs " + serverY);
			}
		}
	}
}
