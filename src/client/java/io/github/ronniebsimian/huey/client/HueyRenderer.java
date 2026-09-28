package io.github.ronniebsimian.huey.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class HueyRenderer extends EntityRenderer<HueyEntity, HueyRenderState> {
	private static final Identifier TEXTURE = HueyMod.id("textures/entity/huey.png");
	private final HueyModel model;

	public HueyRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new HueyModel(context.bakeLayer(HueyModel.LAYER));
		this.shadowRadius = 1.6F;
	}

	@Override
	public HueyRenderState createRenderState() {
		return new HueyRenderState();
	}

	@Override
	public void extractRenderState(HueyEntity entity, HueyRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.pitch = entity.getXRot(partialTicks);
		state.roll = entity.getRoll(partialTicks);
		state.rotorAngle = entity.getRotorAngle(partialTicks);
		state.hurtTime = entity.getHurtTime() - partialTicks;
		state.hurtDir = entity.getHurtDir();
		state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);

		Entity left = entity.getSeatOccupant(HueyEntity.GUNNER_LEFT);
		Entity right = entity.getSeatOccupant(HueyEntity.GUNNER_RIGHT);
		state.gunLeftYaw = left != null ? gunYaw(entity, left, -90.0F, partialTicks) : 0.0F;
		state.gunLeftPitch = left != null ? Mth.clamp(left.getXRot(partialTicks), -30.0F, 60.0F) : 0.0F;
		state.gunRightYaw = right != null ? gunYaw(entity, right, 90.0F, partialTicks) : 0.0F;
		state.gunRightPitch = right != null ? Mth.clamp(right.getXRot(partialTicks), -30.0F, 60.0F) : 0.0F;
	}

	private static float gunYaw(HueyEntity huey, Entity gunner, float seatFacing, float partialTicks) {
		float rel = Mth.wrapDegrees(gunner.getYRot(partialTicks) - (huey.getYRot(partialTicks) + seatFacing));
		return Mth.clamp(rel, -80.0F, 80.0F);
	}

	@Override
	public void submit(HueyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.5F, 0.0F); // tilt around the middle of the cabin, not the skids
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		poseStack.rotateDegrees(Axis.XP, -state.pitch);
		poseStack.rotateDegrees(Axis.ZP, state.roll);
		if (state.hurtTime > 0.0F) {
			poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 40.0F * state.hurtDir);
		}
		poseStack.translate(0.0F, -1.5F, 0.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(HueyEntity entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(6.0, 1.5, 6.0); // rotor & tail stick out
	}
}
