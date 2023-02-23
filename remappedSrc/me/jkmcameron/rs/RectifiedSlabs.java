package me.jkmcameron.rs;

import com.mojang.serialization.Lifecycle;
import me.jkmcameron.rs.block.entity.SlabBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import java.util.List;
import java.util.OptionalInt;

public class RectifiedSlabs implements ModInitializer
{
	public static BlockEntityType<SlabBlockEntity> SLAB_BLOCK_ENTITY;

	@Override
	public void onInitialize()
	{
		List slabs = Registry.BLOCK.stream().filter(block -> block instanceof SlabBlock).toList();

		SLAB_BLOCK_ENTITY = Registry.register(
				Registry.BLOCK_ENTITY_TYPE,
				"rect:mixed_slab_block_entity",
				FabricBlockEntityTypeBuilder.create(SlabBlockEntity::new, (Block[]) slabs.toArray(new Block[0])
				).build());

		RegistryEntryAddedCallback.event(Registry.BLOCK).register((rawId, id, object) ->
		{
			if(object instanceof SlabBlock)
			{
				slabs.add(object);

				((MutableRegistry)Registry.BLOCK_ENTITY_TYPE).replace(
						OptionalInt.of(Registry.BLOCK_ENTITY_TYPE.getRawId(SLAB_BLOCK_ENTITY)),
						Registry.BLOCK_ENTITY_TYPE.getKey(),
						FabricBlockEntityTypeBuilder.create(SlabBlockEntity::new, (Block[]) slabs.toArray(new Block[0])
						).build(), Lifecycle.stable());
			}
		});
	}
}
