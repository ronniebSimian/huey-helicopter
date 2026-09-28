package io.github.ronniebsimian.huey.client;

import io.github.ronniebsimian.huey.HueyMod;
import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** The "whop-whop". Follows the helicopter; louder and higher-pitched as the rotor spins up. */
public class HueyRotorSound extends AbstractTickableSoundInstance {
	private final HueyEntity huey;

	public HueyRotorSound(HueyEntity huey) {
		super(HueyMod.ROTOR_LOOP, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
		this.huey = huey;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;
		this.pitch = 0.5F;
		this.x = huey.getX();
		this.y = huey.getY();
		this.z = huey.getZ();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public boolean canPlaySound() {
		return !this.huey.isSilent();
	}

	@Override
	public void tick() {
		if (this.huey.isRemoved()) {
			this.stop();
			return;
		}
		this.x = this.huey.getX();
		this.y = this.huey.getY() + 2.0;
		this.z = this.huey.getZ();
		float rotor = this.huey.getRotorSpeed();
		// Volume above 1.0 doesn't get louder up close, it makes the sound carry further (~80 blocks at full).
		this.volume = rotor * 5.0F;
		this.pitch = 0.5F + rotor * 0.5F;
	}
}
