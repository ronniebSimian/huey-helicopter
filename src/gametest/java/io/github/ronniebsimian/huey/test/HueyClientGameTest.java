package io.github.ronniebsimian.huey.test;

import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

/**
 * Flies a Huey end to end in a real game client and takes screenshots along the way.
 * Screenshots land in build/run/clientGameTest/screenshots/.
 */
public class HueyClientGameTest implements FabricClientGameTest {
	private static final double X = 0.5;
	private static final double Z = 0.5;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			int ground = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0);
			});
			String y = Integer.toString(ground);
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("fill -12 " + (ground - 1) + " -12 12 " + (ground + 12) + " 12 air");
			world.getServer().runCommand("fill -12 " + (ground - 1) + " -12 12 " + (ground - 1) + " 12 grass_block");

			// --- spawn and photograph from four sides
			int hueyId = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				HueyEntity huey = HueyMod.HUEY.create(level, EntitySpawnReason.COMMAND);
				huey.snapTo(X, ground, Z, 0.0F, 0.0F); // facing south (+z)
				level.addFreshEntity(huey);
				return huey.getId();
			});
			context.waitFor(mc -> mc.level != null && mc.level.getEntity(hueyId) != null);
			context.getInput().pressKey(options -> options.keyToggleGui); // hide the HUD for clean "photos" (F1)
			shoot(context, world, "01_front_left", X + 7, ground + 3, Z + 9, ground);
			shoot(context, world, "02_left_side", X + 11, ground + 2, Z, ground);
			shoot(context, world, "03_rear_right", X - 8, ground + 4, Z - 10, ground);
			shoot(context, world, "04_right_side", X - 11, ground + 2, Z, ground);
			shoot(context, world, "05_top", X + 4, ground + 14, Z + 4, ground);
			context.getInput().pressKey(options -> options.keyToggleGui); // HUD back on

			// --- board as pilot and fly
			// walk up to it (let the teleport settle first, like a real player), then climb in
			world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %d %.2f", X + 3, ground, Z));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.startRiding(huey(server.overworld(), hueyId));
			});
			context.waitTicks(5);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			look(context, 0.0F, 15.0F);
			context.waitTicks(90); // rotor spool-up
			context.takeScreenshot("06_pilot_spooled");
			context.getInput().holdKeyFor(options -> options.keyJump, 60);
			double climbed = world.getServer().computeOnServer(server -> huey(server.overworld(), hueyId).getY()) - ground;
			context.takeScreenshot("07_hover");
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(40);
			context.takeScreenshot("08_forward_flight");
			context.getInput().releaseKey(options -> options.keyUp);
			context.getInput().holdKeyFor(options -> options.keyRight, 30);
			context.takeScreenshot("09_turning");
			Vec3 after = world.getServer().computeOnServer(server -> huey(server.overworld(), hueyId).position());
			System.out.println("[HUEY TEST] climbed " + climbed + " blocks; after flight at " + after);
			if (climbed < 5.0) {
				throw new AssertionError("Huey failed to climb (only " + climbed + " blocks)");
			}
			if (after.horizontalDistance() < 10.0) {
				throw new AssertionError("Huey failed to fly forward (at " + after + ")");
			}

			// --- land again
			context.getInput().holdKeyFor(options -> options.keySprint, 120);
			boolean landed = world.getServer().computeOnServer(server -> huey(server.overworld(), hueyId).onGround());
			System.out.println("[HUEY TEST] landed=" + landed);

			// --- move to the left door gun and shoot a zombie
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				HueyEntity huey = huey(server.overworld(), hueyId);
				huey.switchSeat(player); // pilot -> copilot
				huey.switchSeat(player); // copilot -> left gunner
				Zombie zombie = EntityType.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND);
				Vec3 left = new Vec3(Math.cos(Math.toRadians(huey.getYRot())), 0, Math.sin(Math.toRadians(huey.getYRot())));
				Vec3 at = huey.position().add(left.scale(9));
				zombie.snapTo(at.x, huey.getY() + 1, at.z, 0, 0);
				zombie.setNoAi(true);
				zombie.addTag("huey_target");
				server.overworld().addFreshEntity(zombie);
			});
			context.waitTicks(10);
			boolean isGunner = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				return huey(server.overworld(), hueyId).getSeatOf(player) == HueyEntity.GUNNER_LEFT;
			});
			if (!isGunner) {
				throw new AssertionError("switch seat did not reach the left gunner seat");
			}
			// aim with the mouse (a /tp would kick the gunner out of the seat)
			float[] aim = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				var target = server.overworld().getEntities(EntityType.ZOMBIE, e -> e.getTags().contains("huey_target")).get(0);
				Vec3 d = target.getEyePosition().subtract(player.getEyePosition());
				float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
				float pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
				return new float[] {yaw, pitch};
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			look(context, aim[0], aim[1]);
			context.waitTicks(5);
			context.takeScreenshot("10_gunner_aim");
			context.getInput().holdKey(options -> options.keyAttack);
			context.waitTicks(4);
			context.takeScreenshot("11_gunner_firing");
			context.waitTicks(16);
			context.getInput().releaseKey(options -> options.keyAttack);
			// look forward-and-down: the gun on the outside of the door should follow
			look(context, aim[0] - 40.0F, 25.0F);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(3);
			context.takeScreenshot("11b_gun_follows_aim");
			boolean stillSeated = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getVehicle() instanceof HueyEntity);
			if (!stillSeated) {
				throw new AssertionError("gunner fell out of the seat while firing");
			}
			float zombieHealth = world.getServer().computeOnServer(server -> {
				var list = server.overworld().getEntities(EntityType.ZOMBIE, e -> e.getTags().contains("huey_target"));
				return list.isEmpty() ? 0.0F : list.get(0).getHealth();
			});
			System.out.println("[HUEY TEST] zombie health after burst: " + zombieHealth);
			if (zombieHealth > 10.0F) {
				throw new AssertionError("door gun did no damage");
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				huey(server.overworld(), hueyId).switchSeat(player); // -> right gunner
				huey(server.overworld(), hueyId).switchSeat(player); // -> troop
				huey(server.overworld(), hueyId).switchSeat(player); // -> troop
				huey(server.overworld(), hueyId).switchSeat(player); // -> door
				huey(server.overworld(), hueyId).switchSeat(player); // -> door (right)
				huey(server.overworld(), hueyId).switchSeat(player); // -> pilot
			});
			context.waitTicks(5);
			look(context, 0.0F, 10.0F);
			context.takeScreenshot("12_pilot_first_person");
		}
	}

	/** This Fabric test kit has no lookAt(), so turn the player directly. */
	private static void look(ClientGameTestContext context, float yaw, float pitch) {
		context.runOnClient(mc -> {
			mc.player.setYRot(yaw);
			mc.player.setXRot(pitch);
		});
	}

	private static HueyEntity huey(ServerLevel level, int id) {
		return (HueyEntity) level.getEntity(id);
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String name, double x, double y, double z, int ground) {
		world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", x, y, z, X, ground + 1.8, Z));
		context.waitTicks(3);
		world.getClientWorld().waitForChunksRender();
		context.takeScreenshot(name);
	}
}
