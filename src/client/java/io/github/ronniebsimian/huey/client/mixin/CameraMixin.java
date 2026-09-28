package io.github.ronniebsimian.huey.client.mixin;

import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Pull the third-person camera back so you can see the whole helicopter. */
@Mixin(Camera.class)
public class CameraMixin {
	@Shadow
	private Entity entity;

	@ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
	private float huey$zoomOut(float distance) {
		return this.entity != null && this.entity.getVehicle() instanceof HueyEntity ? Math.max(distance, 14.0F) : distance;
	}
}
