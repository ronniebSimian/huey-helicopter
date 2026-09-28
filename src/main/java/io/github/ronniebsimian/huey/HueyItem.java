package io.github.ronniebsimian.huey;

import io.github.ronniebsimian.huey.entity.HueyEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Right-click the ground to deploy a Huey, facing the way you're looking. */
public class HueyItem extends Item {
	public HueyItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		Vec3 at = context.getClickLocation();
		HueyEntity huey = HueyMod.HUEY.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (huey == null) {
			return InteractionResult.FAIL;
		}
		huey.snapTo(at.x, at.y, at.z, player != null ? player.getYRot() : 0.0F, 0.0F);
		if (!level.noCollision(huey, huey.getBoundingBox())) {
			if (player != null && !level.isClientSide()) {
				player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.huey.huey.no_room"), true);
			}
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			ItemStack stack = context.getItemInHand();
			huey.setCustomName(stack.getCustomName());
			level.addFreshEntity(huey);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
