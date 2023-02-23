package me.jkmcameron.rs.mixin.client.render.model.json;

import com.mojang.datafixers.util.Either;
import me.jkmcameron.rs.client.render.model.BakedSlabModel;
import me.jkmcameron.rs.mixin.client.render.model.IJsonUnbakedModel;
import me.jkmcameron.rs.mixin.client.render.model.IModelLoader;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mixin(WeightedUnbakedModel.class)
public class WeightedUnbakedModelMixin
{
    @Inject(method = "bake", at = @At("HEAD"), cancellable = true)
    public void bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId, CallbackInfoReturnable<BakedModel> cir)
    {
        Block block = Registry.BLOCK.getOrEmpty(new Identifier(modelId.getNamespace(), modelId.getPath())).orElse(null);

        if(block instanceof SlabBlock)
        {
            WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();

            for (ModelVariant modelVariant : ((WeightedUnbakedModel)(Object)this).getVariants())
            {
                JsonUnbakedModel unbaked = (JsonUnbakedModel) loader.getOrLoadModel(modelVariant.getLocation());

                // Needed for some reason
                ((IJsonUnbakedModel)unbaked).getTextureMap().put("particle", (Either<SpriteIdentifier, String>) ((IJsonUnbakedModel)unbaked).getTextureMap().values().toArray()[0]);

                if(((ModelIdentifier)modelId).getVariant().contains("type=double"))
                {
                    builder.add(unbaked.bake(loader, ((IModelLoader)loader).getSpriteAtlasManager()::getSprite, modelVariant, modelVariant.getLocation()), modelVariant.getWeight());
                    break;
                }

                BakedModel baked = null;

                if(((ModelIdentifier)modelId).getVariant().contains("axis=y"))
                    baked = unbaked.bake(loader, ((IModelLoader)loader).getSpriteAtlasManager()::getSprite, modelVariant, modelVariant.getLocation());

                unbaked = getZfromY(unbaked);

                if(((ModelIdentifier)modelId).getVariant().contains("axis=z"))
                    baked = unbaked.bake(loader, ((IModelLoader)loader).getSpriteAtlasManager()::getSprite, modelVariant, modelVariant.getLocation());

                unbaked = getXfromZ(unbaked);

                if(((ModelIdentifier)modelId).getVariant().contains("axis=x"))
                    baked = unbaked.bake(loader, ((IModelLoader)loader).getSpriteAtlasManager()::getSprite, modelVariant, modelVariant.getLocation());

                builder.add(new BakedSlabModel(baked), modelVariant.getWeight());
            }

            cir.setReturnValue(builder.build());
        }
    }

    private JsonUnbakedModel getZfromY(JsonUnbakedModel yUnbakedModel)
    {
        ModelElement ySlabElement = yUnbakedModel.getElements().get(0);
        Map<Direction, ModelElementFace> yModelFaces = ySlabElement.faces;

        Map<Direction, ModelElementFace> zModelFaces = new HashMap<>();
        for (Direction direction : Direction.values())
        {
            ModelElementFace refBlockFace = yModelFaces.get(direction);

            float[] newUVs = yModelFaces.get(direction.rotateCounterclockwise(Direction.Axis.X)).textureData.uvs.clone();

            switch(direction)
            {
                case WEST, DOWN -> newUVs = new float[] { 16 - newUVs[3],      newUVs[0], 16 - newUVs[1],      newUVs[2] };
                case EAST, UP   -> newUVs = new float[] {      newUVs[1], 16 - newUVs[2],      newUVs[3], 16 - newUVs[0] };
            }

            if(direction.getAxis() == Direction.Axis.Y) newUVs = new float[] { 16 - newUVs[3], 16 - newUVs[2], 16 - newUVs[1], 16 - newUVs[0] };

            zModelFaces.put(direction, new ModelElementFace(direction, refBlockFace.tintIndex, refBlockFace.textureId, new ModelElementTexture(newUVs, refBlockFace.textureData.rotation)));
        }

        ModelElement zSlabElement = new ModelElement(new Vec3f(ySlabElement.from.getX(), ySlabElement.from.getZ(), ySlabElement.from.getY()), new Vec3f(ySlabElement.to.getX(), ySlabElement.to.getZ(), ySlabElement.to.getY()), zModelFaces, ySlabElement.rotation, ySlabElement.shade);

        return new JsonUnbakedModel(((IJsonUnbakedModel)yUnbakedModel).getParentId(), List.of(zSlabElement), ((IJsonUnbakedModel) yUnbakedModel).getTextureMap(), yUnbakedModel.useAmbientOcclusion(), yUnbakedModel.getGuiLight(), yUnbakedModel.getTransformations(), yUnbakedModel.getOverrides());
    }


    private JsonUnbakedModel getXfromZ(JsonUnbakedModel zUnbakedModel)
    {
        ModelElement zSlabElement = zUnbakedModel.getElements().get(0);
        Map<Direction, ModelElementFace> zModelFaces = zSlabElement.faces;

        Map<Direction, ModelElementFace> xModelFaces = new HashMap<>();
        for (Direction direction : Direction.values())
        {
            ModelElementFace refBlockFace = zModelFaces.get(direction.rotateClockwise(Direction.Axis.Y));

            float[] newUVs = zModelFaces.get(direction.rotateCounterclockwise(Direction.Axis.Y)).textureData.uvs.clone();

            switch(direction)
            {
                case UP    -> newUVs = new float[] {      newUVs[1], 16 - newUVs[2],      newUVs[3], 16 - newUVs[0] };
                case DOWN  -> newUVs = new float[] { 16 - newUVs[3], 16 - newUVs[2], 16 - newUVs[1], 16 - newUVs[0] };
            }

            if(direction.getAxis() == Direction.Axis.Z) newUVs = new float[] { 16 - newUVs[2], newUVs[1], 16 - newUVs[0], newUVs[3] };

            xModelFaces.put(direction, new ModelElementFace(direction, refBlockFace.tintIndex, refBlockFace.textureId, new ModelElementTexture(newUVs, refBlockFace.textureData.rotation)));
        }

        ModelElement xSlabElement = new ModelElement(new Vec3f(zSlabElement.from.getZ(), zSlabElement.from.getX(), zSlabElement.from.getY()), new Vec3f(zSlabElement.to.getZ(), zSlabElement.to.getX(), zSlabElement.to.getY()), xModelFaces, zSlabElement.rotation, zSlabElement.shade);

        return new JsonUnbakedModel(((IJsonUnbakedModel)zUnbakedModel).getParentId(), List.of(xSlabElement), ((IJsonUnbakedModel) zUnbakedModel).getTextureMap(), zUnbakedModel.useAmbientOcclusion(), zUnbakedModel.getGuiLight(), zUnbakedModel.getTransformations(), zUnbakedModel.getOverrides());
    }
}
