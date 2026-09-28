package io.github.ronniebsimian.huey;

import io.github.ronniebsimian.huey.entity.HueyEntity;
import io.github.ronniebsimian.huey.network.HueyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HueyMod implements ModInitializer {
	public static final String MOD_ID = "huey";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<EntityType<?>> HUEY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("huey"));
	public static final EntityType<HueyEntity> HUEY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		HUEY_KEY,
		EntityType.Builder.of(HueyEntity::new, MobCategory.MISC)
			.noLootTable()
			.sized(3.0F, 3.4F)
			.eyeHeight(1.5F)
			.clientTrackingRange(16)
			.build(HUEY_KEY)
	);

	public static final ResourceKey<Item> HUEY_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("huey"));
	public static final Item HUEY_ITEM = Registry.register(
		BuiltInRegistries.ITEM, HUEY_ITEM_KEY, new HueyItem(new Item.Properties().setId(HUEY_ITEM_KEY).stacksTo(1))
	);

	public static final SoundEvent ROTOR_LOOP = registerSound("entity.huey.rotor");
	public static final SoundEvent DOOR_GUN = registerSound("entity.huey.door_gun");

	public static final ResourceKey<DamageType> DOOR_GUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("door_gun"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static SoundEvent registerSound(String name) {
		Identifier id = id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	@Override
	public void onInitialize() {
		HueyNetworking.registerCommon();
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(HUEY_ITEM));
		LOGGER.info("Huey helicopter loaded. Get to the chopper.");
	}
}
