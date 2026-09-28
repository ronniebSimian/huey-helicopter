package io.github.ronniebsimian.huey.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class HueyRenderState extends EntityRenderState {
	public float yRot;
	public float pitch;
	public float roll;
	public float rotorAngle;
	public float hurtTime;
	public int hurtDir;
	public float damageTime;
	/** Door gun aim relative to pointing straight out of the door, in degrees (positive pitch = down). */
	public float gunLeftYaw;
	public float gunLeftPitch;
	public float gunRightYaw;
	public float gunRightPitch;
}
