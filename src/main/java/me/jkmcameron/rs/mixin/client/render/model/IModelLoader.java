package me.jkmcameron.rs.mixin.client.render.model;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;


@Mixin(ModelLoader.class)
public interface IModelLoader
{
    @Accessor
    ResourceManager getResourceManager();

    @Accessor
    Map<Triple<Identifier, AffineTransformation, Boolean>, BakedModel> getBakedModelCache();

    @Accessor
    SpriteAtlasManager getSpriteAtlasManager();

    @Invoker
    UnbakedModel callGetOrLoadModel(Identifier id);

}
