package me.jkmcameron.rs.client.render.model;

import me.jkmcameron.rs.block.entity.SlabBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class BakedSlabModel implements BakedModel, FabricBakedModel
{

    private final BakedModel baseModel;

    public BakedModel getBaseModel()
    {
        return baseModel;
    }

    public BakedSlabModel(BakedModel baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public boolean isVanillaAdapter()
    {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context)
    {
        Direction omittedFace = switch(state.get(Properties.AXIS))
        {
            case X -> state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? Direction.EAST  : Direction.WEST;
            case Y -> null;
            case Z -> state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? Direction.SOUTH : Direction.NORTH;
        };

        SlabBlockEntity blockEntity = (SlabBlockEntity) blockView.getBlockEntity(pos);
        boolean isMixed = blockEntity != null && blockEntity.hasMate();

        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        MeshBuilder builder = renderer.meshBuilder();
        QuadEmitter emitter = builder.getEmitter();

        if(isMixed)
        {
            BakedModel mateSlabModel = MinecraftClient.getInstance().getBakedModelManager().getModel(BlockModels.getModelId(blockEntity.getMateBlockState()));

            for(int j = 0; j <= ModelHelper.NULL_FACE_ID; j++) for(BakedQuad quad : mateSlabModel.getQuads(null, ModelHelper.faceFromIndex(j), randomSupplier.get()))
            {
                if(ModelHelper.faceFromIndex(j) == (omittedFace == null ? null : omittedFace.getOpposite())) continue;

                emitter.fromVanilla(quad, renderer.materialFinder().find(), ModelHelper.faceFromIndex(j));
                emitter.tag();
                emitter.cullFace(null);
                emitter.emit();
            }
        }

        for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) for (BakedQuad quad : baseModel.getQuads(null, ModelHelper.faceFromIndex(i), randomSupplier.get()))
        {
            if (isMixed && ModelHelper.faceFromIndex(i) == omittedFace) continue;

            emitter.fromVanilla(quad, renderer.materialFinder().find(), ModelHelper.faceFromIndex(i));
            emitter.cullFace(null);
            emitter.emit();
        }

        context.meshConsumer().accept(builder.build());
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context)
    {

    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) { return baseModel.getQuads(state,face,random); }
    @Override public boolean useAmbientOcclusion() { return baseModel.useAmbientOcclusion(); }
    @Override public boolean hasDepth() { return baseModel.hasDepth(); }
    @Override public boolean isSideLit() { return baseModel.isSideLit(); }
    @Override public boolean isBuiltin() { return baseModel.isBuiltin(); }
    @Override public Sprite getParticleSprite() { return baseModel.getParticleSprite(); }
    @Override public ModelTransformation getTransformation() { return baseModel.getTransformation(); }
    @Override public ModelOverrideList getOverrides() { return baseModel.getOverrides(); }

}
