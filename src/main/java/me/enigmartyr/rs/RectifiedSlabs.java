package me.enigmartyr.rs;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.mojang.serialization.Lifecycle;
import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import me.enigmartyr.rs.client.render.block.entity.SlabBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.List;

public class RectifiedSlabs implements ModInitializer, ClientModInitializer, PreLaunchEntrypoint {
	public static BlockEntityType<SlabBlockEntity> SLAB_BLOCK_ENTITY;

	@Override
	public void onInitialize()
	{
		List slabs = Registries.BLOCK.stream().filter(block -> block instanceof SlabBlock).toList();

		SLAB_BLOCK_ENTITY = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				"rect:mixed_slab_block_entity",
				FabricBlockEntityTypeBuilder.create(SlabBlockEntity::new, (Block[]) slabs.toArray(new Block[0])
				).build());

		RegistryEntryAddedCallback.event(Registries.BLOCK).register((rawId, id, object) ->
		{
			if(object instanceof SlabBlock)
			{
				slabs.add(object);

				((MutableRegistry) Registries.BLOCK_ENTITY_TYPE).set(
						Registries.BLOCK_ENTITY_TYPE.getRawId(SLAB_BLOCK_ENTITY),
						Registries.BLOCK_ENTITY_TYPE.getKey(),
						FabricBlockEntityTypeBuilder.create(SlabBlockEntity::new, (Block[]) slabs.toArray(new Block[0])
						).build(), Lifecycle.stable());
			}
		});

		if(FabricLoader.getInstance().isModLoaded("carpet")) CarpetSettings.movableBlockEntities = true;
	}

	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(SLAB_BLOCK_ENTITY, SlabBlockEntityRenderer::new);
	}

	@Override
	public void onPreLaunch() {
		MixinExtrasBootstrap.init();
	}
}
