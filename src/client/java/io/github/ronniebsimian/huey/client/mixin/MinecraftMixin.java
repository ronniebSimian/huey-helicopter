package io.github.ronniebsimian.huey.client.mixin;

import io.github.ronniebsimian.huey.client.HueyModClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While manning a door gun, the attack button fires the gun instead of punching or mining. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void huey$fireInsteadOfAttack(CallbackInfoReturnable<Boolean> cir) {
		if (HueyModClient.isManningGun()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void huey$fireInsteadOfMining(boolean down, CallbackInfo ci) {
		if (HueyModClient.isManningGun()) {
			ci.cancel();
		}
	}
}
