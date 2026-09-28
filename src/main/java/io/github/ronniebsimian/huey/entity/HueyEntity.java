package io.github.ronniebsimian.huey.entity;

import io.github.ronniebsimian.huey.HueyMod;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The UH-1 "Huey".
 *
 * How flying works: whoever sits in the pilot seat "owns" the helicopter's movement. Their game
 * runs the flight code below and Minecraft sends the result to the server, exactly like boats.
 * With no pilot, the server runs the same code (so an empty Huey settles to the ground).
 *
 * Seats (see SEATS): pilot, copilot, two M60 door gunners, and four troop spots.
 */
public class HueyEntity extends VehicleEntity {
	// ------------------------------------------------------------------ tuning
	public static final float MAX_HEALTH = 100.0F;
	private static final float SPOOL_UP_PER_TICK = 1.0F / 80.0F;    // 4 seconds to full rotor speed
	private static final float SPOOL_DOWN_PER_TICK = 1.0F / 200.0F; // 10 seconds to stop
	private static final double FORWARD_ACCEL = 0.045;               // top speed ~ accel / (1 - drag)
	private static final double REVERSE_ACCEL = 0.02;
	private static final double AIR_DRAG = 0.965;
	private static final double CLIMB_SPEED = 0.40;
	private static final double DESCEND_SPEED = 0.45;
	private static final double GRAVITY = 0.04;
	private static final float YAW_ACCEL = 0.9F;
	private static final float MAX_PITCH = 14.0F;

	private static final int GUN_COOLDOWN_TICKS = 3;                 // ~400 rounds per minute
	private static final float GUN_DAMAGE = 4.0F;
	private static final double GUN_RANGE = 96.0;
	private static final float GUN_HEAT_PER_SHOT = 3.5F;
	private static final float GUN_OVERHEAT = 100.0F;           // ~29 rounds (~4.4 s) of continuous fire
	private static final int GUN_OVERHEAT_LOCK_TICKS = 75;      // ~4 s locked out after overheating
	private static final float GUN_UNLOCK_HEAT = 25.0F;
	private static final float GUN_COOL_PER_TICK = 1.5F;

	// ------------------------------------------------------------------ seats
	public static final int PILOT = 0;
	public static final int COPILOT = 1;
	public static final int GUNNER_LEFT = 2;
	public static final int GUNNER_RIGHT = 3;
	public static final int SEAT_COUNT = 8;

	/**
	 * left/up/forward are in blocks from the helicopter's center; facing is degrees added to the
	 * helicopter's heading (-90 = faces out the left door, +90 = right door).
	 * These match the model coordinates in tools/gen_model.py divided by 16.
	 */
	public record Seat(String name, double left, double up, double forward, float facing) {
	}

	public static final Seat[] SEATS = {
		new Seat("pilot", -8 / 16.0, 17 / 16.0, 26 / 16.0, 0.0F),
		new Seat("copilot", 8 / 16.0, 17 / 16.0, 26 / 16.0, 0.0F),
		new Seat("gunner_left", 12 / 16.0, 16 / 16.0, -20 / 16.0, -90.0F),
		new Seat("gunner_right", -12 / 16.0, 16 / 16.0, -20 / 16.0, 90.0F),
		new Seat("troop", -4.5 / 16.0, 17 / 16.0, -15 / 16.0, 0.0F),
		new Seat("troop", 4.5 / 16.0, 17 / 16.0, -15 / 16.0, 0.0F),
		new Seat("door", 14.5 / 16.0, 12 / 16.0, 4 / 16.0, -90.0F),
		new Seat("door", -14.5 / 16.0, 12 / 16.0, 4 / 16.0, 90.0F),
	};

	// ------------------------------------------------------------------ synced data
	@SuppressWarnings("unchecked")
	private static final EntityDataAccessor<Integer>[] DATA_SEAT = new EntityDataAccessor[SEAT_COUNT];
	static {
		for (int i = 0; i < SEAT_COUNT; i++) {
			DATA_SEAT[i] = SynchedEntityData.defineId(HueyEntity.class, EntityDataSerializers.INT);
		}
	}
	private static final EntityDataAccessor<Float> DATA_ROTOR = SynchedEntityData.defineId(HueyEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(HueyEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_HEAT_LEFT = SynchedEntityData.defineId(HueyEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_HEAT_RIGHT = SynchedEntityData.defineId(HueyEntity.class, EntityDataSerializers.FLOAT);

	// ------------------------------------------------------------------ client hooks
	/** Set by the client mod: reads the local player's movement keys. Empty on dedicated servers. */
	public static Supplier<Input> clientInput = () -> Input.EMPTY;
	/** Set by the client mod: starts the rotor sound when a Huey first appears. */
	public static Consumer<HueyEntity> clientSpawnHook = huey -> {
	};

	// ------------------------------------------------------------------ state
	private float deltaYaw;
	private boolean clientSeen;
	// visual-only (client)
	private float rotorAngle;
	private float rotorAngleO;
	private float roll;
	private float rollO;
	// guns (server)
	private final boolean[] trigger = new boolean[2];
	private final int[] gunCooldown = new int[2];
	private final boolean[] overheated = new boolean[2];
	private final int[] overheatTicks = new int[2];

	public HueyEntity(EntityType<? extends HueyEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		for (EntityDataAccessor<Integer> seat : DATA_SEAT) {
			builder.define(seat, -1);
		}
		builder.define(DATA_ROTOR, 0.0F);
		builder.define(DATA_HEALTH, MAX_HEALTH);
		builder.define(DATA_HEAT_LEFT, 0.0F);
		builder.define(DATA_HEAT_RIGHT, 0.0F);
	}

	// ================================================================== tick
	@Override
	public void tick() {
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}
		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}

		super.tick();
		this.interpolation.interpolate(); // 1.21.x: vehicles advance their own smoothing each tick

		if (this.level() instanceof ServerLevel serverLevel) {
			this.tickRotorSpool();
			this.tickGuns(serverLevel);
		} else {
			this.tickVisuals();
			if (!this.clientSeen) {
				this.clientSeen = true;
				clientSpawnHook.accept(this);
			}
		}

		if (this.isLocalInstanceAuthoritative()) {
			boolean localPilot = this.level().isClientSide() && this.getControllingPassenger() != null;
			this.fly(localPilot ? clientInput.get() : Input.EMPTY);
			this.move(MoverType.SELF, this.getDeltaMovement());
		} else {
			this.setDeltaMovement(Vec3.ZERO);
			this.deltaYaw = 0.0F;
		}

		this.applyEffectsFromBlocks();
	}

	private void tickRotorSpool() {
		float rotor = this.getRotorSpeed();
		boolean powered = this.getControllingPassenger() != null && !this.isEyeInFluid(FluidTags.WATER);
		rotor = powered ? Math.min(1.0F, rotor + SPOOL_UP_PER_TICK) : Math.max(0.0F, rotor - SPOOL_DOWN_PER_TICK);
		this.entityData.set(DATA_ROTOR, rotor);
	}

	/** How much lift the rotor makes: none below 60% rotor speed, full at 100%. */
	public float getLift() {
		return Mth.clamp((this.getRotorSpeed() - 0.6F) / 0.4F, 0.0F, 1.0F);
	}

	private void fly(Input in) {
		float lift = this.getLift();
		Vec3 v = this.getDeltaMovement();
		double vx = v.x;
		double vy = v.y;
		double vz = v.z;

		// Yaw (A / D)
		float yawInput = (in.right() ? 1.0F : 0.0F) - (in.left() ? 1.0F : 0.0F);
		this.deltaYaw = this.deltaYaw * 0.8F + yawInput * YAW_ACCEL * lift;
		this.setYRot(this.getYRot() + this.deltaYaw);

		// Forward / back (W / S). The nose tilts down to fly forward, like the real thing.
		float fwdInput = (in.forward() ? 1.0F : 0.0F) - (in.backward() ? 1.0F : 0.0F);
		boolean airborne = !this.onGround() || vy > 0.0;
		float targetPitch = airborne ? fwdInput * MAX_PITCH * lift : 0.0F;
		this.setXRot(Mth.approach(this.getXRot(), targetPitch, 1.2F));
		if (airborne && lift > 0.0F) {
			double accel = (fwdInput > 0 ? FORWARD_ACCEL : REVERSE_ACCEL) * fwdInput * lift;
			float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
			vx += -Mth.sin(yawRad) * accel;
			vz += Mth.cos(yawRad) * accel;
		}

		// Up / down (Space / Sprint key). With no input a spun-up Huey holds its altitude.
		double targetVy = ((in.jump() ? CLIMB_SPEED : 0.0) - (in.sprint() ? DESCEND_SPEED : 0.0)) * lift;
		vy = Mth.lerp(0.12 * lift, vy, targetVy) - GRAVITY * (1.0F - lift);
		if (this.isInWater()) {
			vy = Math.max(vy, 0.02); // skids float, just barely
		}

		double drag = this.onGround() ? 0.5 : AIR_DRAG;
		this.setDeltaMovement(vx * drag, vy, vz * drag);
	}

	private void tickVisuals() {
		this.rotorAngleO = this.rotorAngle;
		this.rotorAngle += this.getRotorSpeed() * 62.0F;
		if (this.rotorAngle > 3600.0F) {
			this.rotorAngle -= 3600.0F;
			this.rotorAngleO -= 3600.0F;
		}
		this.rollO = this.roll;
		float yawRate = Mth.wrapDegrees(this.getYRot() - this.yRotO);
		this.roll = Mth.lerp(0.15F, this.roll, Mth.clamp(yawRate * 3.0F, -18.0F, 18.0F));
	}

	public float getRotorAngle(float partialTick) {
		return Mth.lerp(partialTick, this.rotorAngleO, this.rotorAngle);
	}

	public float getRoll(float partialTick) {
		return Mth.lerp(partialTick, this.rollO, this.roll);
	}

	@Override
	public boolean isFlyingVehicle() {
		return true; // stops the server kicking the pilot for "flying"
	}

	@Override
	protected double getDefaultGravity() {
		return GRAVITY;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
		if (this.getLift() > 0.5F) {
			this.resetFallDistance(); // a powered descent is not a fall
		} else {
			super.checkFallDamage(ya, onGround, onState, pos);
		}
	}

	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

	@Override
	public InterpolationHandler getInterpolation() {
		return this.interpolation;
	}

	// ================================================================== seats
	public float getRotorSpeed() {
		return this.entityData.get(DATA_ROTOR);
	}

	public float getHealth() {
		return this.entityData.get(DATA_HEALTH);
	}

	private void setHealth(float health) {
		this.entityData.set(DATA_HEALTH, Mth.clamp(health, 0.0F, MAX_HEALTH));
	}

	public float getGunHeat(int seat) {
		return this.entityData.get(seat == GUNNER_LEFT ? DATA_HEAT_LEFT : DATA_HEAT_RIGHT);
	}

	public @Nullable Entity getSeatOccupant(int seat) {
		int id = this.entityData.get(DATA_SEAT[seat]);
		if (id < 0) {
			return null;
		}
		Entity entity = this.level().getEntity(id);
		return entity != null && entity.getVehicle() == this ? entity : null;
	}

	public int getSeatOf(Entity passenger) {
		int id = passenger.getId();
		for (int i = 0; i < SEAT_COUNT; i++) {
			if (this.entityData.get(DATA_SEAT[i]) == id) {
				return i;
			}
		}
		return -1;
	}

	public static boolean isGunnerSeat(int seat) {
		return seat == GUNNER_LEFT || seat == GUNNER_RIGHT;
	}

	private boolean isSeatFree(int seat) {
		return this.getSeatOccupant(seat) == null;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getSeatOccupant(PILOT) instanceof Player pilot ? pilot : null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().size() < SEAT_COUNT && !this.isEyeInFluid(FluidTags.WATER);
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (!this.level().isClientSide() && this.getSeatOf(passenger) < 0) {
			// Players fill the crew seats first; mobs only ever ride in the back.
			int first = passenger instanceof Player ? 0 : 4;
			for (int i = first; i < SEAT_COUNT; i++) {
				if (this.isSeatFree(i)) {
					this.entityData.set(DATA_SEAT[i], passenger.getId());
					break;
				}
			}
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!this.level().isClientSide()) {
			int seat = this.getSeatOf(passenger);
			if (seat >= 0) {
				this.entityData.set(DATA_SEAT[seat], -1);
				if (isGunnerSeat(seat)) {
					this.trigger[seat - GUNNER_LEFT] = false;
				}
			}
		}
	}

	/** Called when a player presses the switch-seat key. Moves them to the next free seat. */
	public void switchSeat(Player player) {
		int current = this.getSeatOf(player);
		if (current < 0) {
			return;
		}
		for (int step = 1; step < SEAT_COUNT; step++) {
			int next = (current + step) % SEAT_COUNT;
			if (this.isSeatFree(next)) {
				this.entityData.set(DATA_SEAT[current], -1);
				this.entityData.set(DATA_SEAT[next], player.getId());
				if (isGunnerSeat(current)) {
					this.trigger[current - GUNNER_LEFT] = false;
				}
				return;
			}
		}
		player.displayClientMessage(Component.translatable("huey.message.no_free_seat"), true);
	}

	/** Seat position relative to the helicopter, rotated to its current heading. */
	private Vec3 seatOffset(Seat seat) {
		return new Vec3(seat.left, seat.up, seat.forward).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int seat = this.getSeatOf(passenger);
		if (seat < 0) {
			// Seat data hasn't arrived yet on this client; fall back to boarding order.
			seat = Math.min(this.getPassengers().indexOf(passenger), SEAT_COUNT - 1);
			seat = Math.max(seat, 0);
		}
		return this.seatOffset(SEATS[seat]);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		super.positionRider(passenger, moveFunction);
		if (passenger.isLocalInstanceAuthoritative()) {
			passenger.setYRot(passenger.getYRot() + this.deltaYaw);
			passenger.setYHeadRot(passenger.getYHeadRot() + this.deltaYaw);
		}
		this.clampRotation(passenger);
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		passenger.yRotO += this.clampRotation(passenger);
	}

	private float seatFacing(Entity passenger) {
		int seat = this.getSeatOf(passenger);
		return this.getYRot() + (seat >= 0 ? SEATS[seat].facing : 0.0F);
	}

	private float clampRotation(Entity passenger) {
		float bodyYaw = this.seatFacing(passenger);
		passenger.setYBodyRot(bodyYaw);
		float delta = Mth.wrapDegrees(passenger.getYRot() - bodyYaw);
		float limit = isGunnerSeat(this.getSeatOf(passenger)) ? 110.0F : 105.0F;
		float clamped = Mth.clamp(delta, -limit, limit);
		passenger.setYRot(passenger.getYRot() + clamped - delta);
		return clamped - delta;
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// Step out of whichever side of the helicopter you were sitting on.
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		Vec3 left = new Vec3(Mth.cos(yawRad), 0.0, Mth.sin(yawRad));
		Vec3 rel = passenger.position().subtract(this.position());
		double side = rel.dot(left) >= 0.0 ? 1.0 : -1.0;
		Vec3 target = this.position().add(left.scale(side * (this.getBbWidth() / 2.0 + 0.9)));
		BlockPos base = BlockPos.containing(target.x, this.getY(), target.z);
		for (int dy : new int[] {0, 1, -1, 2, -2}) {
			Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), base.above(dy), true);
			if (safe != null) {
				return safe;
			}
		}
		if (!this.onGround()) {
			return new Vec3(target.x, passenger.getY(), target.z); // jumping out in flight - good luck
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	// ================================================================== interaction
	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		InteractionResult base = super.interact(player, hand);
		if (base != InteractionResult.PASS) {
			return base;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Items.IRON_INGOT) && this.getHealth() < MAX_HEALTH && !this.hasPassenger(player)) {
			if (!this.level().isClientSide()) {
				this.setHealth(this.getHealth() + 10.0F);
				stack.consume(1, player);
				this.playSound(SoundEvents.ANVIL_USE, 0.6F, 1.4F);
				player.displayClientMessage(Component.translatable("huey.message.repaired", Math.round(this.getHealth()), Math.round(MAX_HEALTH)), true);
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!this.level().isClientSide()) {
			return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return AbstractBoat.canVehicleCollide(this, entity);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return true;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	// ================================================================== damage
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return true;
		}
		if (this.isInvulnerableToBase(source)) {
			return false;
		}
		Entity attacker = source.getEntity();
		if (attacker != null && this.hasPassenger(attacker)) {
			return false; // crew can't damage their own ride
		}
		if (attacker instanceof Player player && player.getAbilities().instabuild) {
			this.discard(); // creative mode: remove instantly
			return true;
		}

		this.setHurtDir(-this.getHurtDir());
		this.setHurtTime(10);
		this.setDamage(Math.min(this.getDamage() + amount * 10.0F, 60.0F));
		this.markHurt();
		this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DAMAGE, attacker);
		this.setHealth(this.getHealth() - amount);

		if (this.getHealth() <= 0.0F) {
			boolean meleeByPlayer = attacker instanceof Player && source.getDirectEntity() == attacker;
			if (meleeByPlayer) {
				this.destroy(level, this.getDropItem()); // taken apart by hand: get the item back
			} else {
				this.crash(level);
			}
		}
		return true;
	}

	private void crash(ServerLevel level) {
		this.ejectPassengers();
		level.explode(this, this.getX(), this.getY() + 1.5, this.getZ(), 2.5F, Level.ExplosionInteraction.NONE);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.0, this.getZ(), 40, 1.5, 1.0, 1.5, 0.02);
		this.kill(level);
	}

	@Override
	protected Item getDropItem() {
		return HueyMod.HUEY_ITEM;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(HueyMod.HUEY_ITEM);
	}

	// ================================================================== door guns
	public void setTrigger(Player player, boolean firing) {
		int seat = this.getSeatOf(player);
		if (isGunnerSeat(seat)) {
			this.trigger[seat - GUNNER_LEFT] = firing;
		}
	}

	private void tickGuns(ServerLevel level) {
		for (int g = 0; g < 2; g++) {
			int seat = GUNNER_LEFT + g;
			EntityDataAccessor<Float> heatData = g == 0 ? DATA_HEAT_LEFT : DATA_HEAT_RIGHT;
			float heat = this.entityData.get(heatData);
			if (this.gunCooldown[g] > 0) {
				this.gunCooldown[g]--;
			}
			boolean wantsFire = this.trigger[g] && this.getSeatOccupant(seat) instanceof Player;
			if (this.overheated[g]) {
				// Locked out. The gauge stays pinned at max until the barrel has cooled enough to fire again.
				if (--this.overheatTicks[g] <= 0) {
					this.overheated[g] = false;
					heat = GUN_UNLOCK_HEAT;
				}
			} else if (wantsFire) {
				// Holding the trigger: the barrel only heats up (no cooling between rounds).
				if (this.gunCooldown[g] == 0 && this.fireGun(level, (Player) this.getSeatOccupant(seat), seat)) {
					this.gunCooldown[g] = GUN_COOLDOWN_TICKS;
					heat += GUN_HEAT_PER_SHOT;
					if (heat >= GUN_OVERHEAT) {
						heat = GUN_OVERHEAT;
						this.overheated[g] = true;
						this.overheatTicks[g] = GUN_OVERHEAT_LOCK_TICKS;
						this.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.2F);
					}
				}
			} else {
				heat = Math.max(0.0F, heat - GUN_COOL_PER_TICK);
			}
			if (heat != this.entityData.get(heatData)) {
				this.entityData.set(heatData, heat);
			}
		}
	}

	public boolean isOverheated(int seat) {
		return this.getGunHeat(seat) >= GUN_OVERHEAT - 0.01F;
	}

	/** Fires one M60 round (instant-hit). Returns false if the gunner is aiming into the helicopter. */
	private boolean fireGun(ServerLevel level, Player gunner, int seat) {
		Vec3 look = gunner.getViewVector(1.0F);
		float outwardYaw = (this.getYRot() + SEATS[seat].facing) * Mth.DEG_TO_RAD;
		Vec3 outward = new Vec3(-Mth.sin(outwardYaw), 0.0, Mth.cos(outwardYaw));
		if (look.dot(outward) < -0.15) {
			return false; // the gun mount can't swing that far
		}
		Vec3 dir = look.add(this.random.triangle(0.0, 0.012), this.random.triangle(0.0, 0.012), this.random.triangle(0.0, 0.012)).normalize();
		Vec3 muzzle = gunner.getEyePosition().add(outward.scale(0.9)).add(0.0, -0.35, 0.0);
		Vec3 end = muzzle.add(dir.scale(GUN_RANGE));

		BlockHitResult blockHit = level.clip(new ClipContext(muzzle, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, gunner));
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
			gunner,
			muzzle,
			end,
			new AABB(muzzle, end).inflate(1.0),
			e -> e.isPickable() && !e.isSpectator() && e != this && e.getRootVehicle() != this,
			muzzle.distanceToSqr(end)
		);

		// muzzle flash, sound and a tracer every other round
		level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
		level.playSound(null, muzzle.x, muzzle.y, muzzle.z, HueyMod.DOOR_GUN, SoundSource.PLAYERS, 2.5F, 0.92F + this.random.nextFloat() * 0.12F);
		Vec3 hitPos = entityHit != null ? entityHit.getLocation() : end;
		if (this.tickCount % 2 == 0) {
			double length = muzzle.distanceTo(hitPos);
			DustParticleOptions tracer = new DustParticleOptions(0xFF6A2A, 0.9F);
			for (double d = 1.0; d < length; d += 1.6) {
				Vec3 p = muzzle.add(dir.scale(d));
				level.sendParticles(tracer, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		if (entityHit != null) {
			Entity target = entityHit.getEntity();
			DamageSource bullet = new DamageSource(
				level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HueyMod.DOOR_GUN_DAMAGE), this, gunner
			);
			target.hurtServer(level, bullet, GUN_DAMAGE);
			level.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 4, 0.1, 0.1, 0.1, 0.2);
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			BlockState state = level.getBlockState(blockHit.getBlockPos());
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), hitPos.x, hitPos.y, hitPos.z, 5, 0.1, 0.1, 0.1, 0.1);
		}
		return true;
	}

	// ================================================================== save / load
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Health", this.getHealth());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setHealth(input.getFloatOr("Health", MAX_HEALTH));
	}
}
