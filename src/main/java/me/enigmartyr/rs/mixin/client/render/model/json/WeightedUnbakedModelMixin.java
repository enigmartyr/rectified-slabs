package me.enigmartyr.rs.mixin.client.render.model.json;

import com.mojang.datafixers.util.Either;
import me.enigmartyr.rs.client.render.model.BakedSlabModel;
import net.minecraft.block.Block;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(WeightedUnbakedModel.class)
public class WeightedUnbakedModelMixin
{
    @Inject(method = "bake", at = @At("HEAD"), cancellable = true)
    public void rectifiedslabs_bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId, CallbackInfoReturnable<BakedModel> cir)
    {
        Block block = Registries.BLOCK.getOrEmpty(new Identifier(modelId.getNamespace(), modelId.getPath())).orElse(null);

        if(block instanceof SlabBlock)
        {
            boolean isPillar = false;
            for(Block bl : Registries.BLOCK)
                if (bl instanceof PillarBlock)
                    if(Registries.BLOCK.getId(bl).getPath().contains(modelId.getPath().replace("_slab", "").replace("slab", "")))
                        isPillar = true;

            WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();

            for (ModelVariant modelVariant : ((WeightedUnbakedModel)(Object)this).getVariants())
            {
                JsonUnbakedModel unbaked = (JsonUnbakedModel) baker.getOrLoadModel(modelVariant.getLocation());

                // Gets a list of what textures a slab makes use of, if more than 1 then it is assumed that the block is directional
                List<String> textures = unbaked.textureMap.values().stream().map(e -> e.left().get().getTextureId().toString()).distinct().collect(Collectors.toList());
                boolean isDirectional = false /*textures.size() > 1*/;

                // Needed for some reason
                unbaked.textureMap.put("particle", (Either<SpriteIdentifier, String>) unbaked.textureMap.values().toArray()[0]);

                if(((ModelIdentifier)modelId).getVariant().contains("type=double"))
                {
                    builder.add(unbaked.bake(baker, textureGetter, modelVariant, modelVariant.getLocation()), modelVariant.getWeight());
                    break;
                }

                BakedModel baked = null;

                switch (((ModelIdentifier) modelId).getVariant().split("axis=")[1].charAt(0))
                {
                    case 'x':
                        if (isDirectional) baked = unbaked.bake(baker, textureGetter, ModelRotation.X90_Y90, modelVariant.getLocation());
                        else               baked = transformZintoX(transformYintoZ(unbaked)).bake(baker, textureGetter, modelVariant, modelVariant.getLocation());
                        break;
                    case 'y':
                        baked = unbaked.bake(baker, textureGetter, modelVariant, modelVariant.getLocation());
                        break;
                    case 'z':
                        if (isDirectional) baked = unbaked.bake(baker, textureGetter, ModelRotation.X270_Y0, modelVariant.getLocation());
                        else               baked = transformYintoZ(unbaked).bake(baker, textureGetter, modelVariant, modelVariant.getLocation());
                        break;
                }

                builder.add(new BakedSlabModel(baked), modelVariant.getWeight());
            }

            cir.setReturnValue(builder.build());
        }
    }

    private JsonUnbakedModel transformYintoZ(JsonUnbakedModel yUnbakedModel)
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

        ModelElement zSlabElement = new ModelElement(new Vector3f(ySlabElement.from.x(), ySlabElement.from.z(), ySlabElement.from.y()), new Vector3f(ySlabElement.to.x(), ySlabElement.to.z(), ySlabElement.to.y()), zModelFaces, ySlabElement.rotation, ySlabElement.shade);

        return new JsonUnbakedModel(yUnbakedModel.parentId, List.of(zSlabElement), yUnbakedModel.textureMap, yUnbakedModel.useAmbientOcclusion(), yUnbakedModel.getGuiLight(), yUnbakedModel.getTransformations(), yUnbakedModel.getOverrides());
    }

    private JsonUnbakedModel transformZintoX(JsonUnbakedModel zUnbakedModel)
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

        ModelElement xSlabElement = new ModelElement(new Vector3f(zSlabElement.from.z(), zSlabElement.from.x(), zSlabElement.from.y()), new Vector3f(zSlabElement.to.z(), zSlabElement.to.x(), zSlabElement.to.y()), xModelFaces, zSlabElement.rotation, zSlabElement.shade);

        return new JsonUnbakedModel(zUnbakedModel.parentId, List.of(xSlabElement), zUnbakedModel.textureMap, zUnbakedModel.useAmbientOcclusion(), zUnbakedModel.getGuiLight(), zUnbakedModel.getTransformations(), zUnbakedModel.getOverrides());
    }
}
