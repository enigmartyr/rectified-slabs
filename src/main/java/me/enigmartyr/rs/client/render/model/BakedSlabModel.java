package me.enigmartyr.rs.client.render.model;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
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
import java.util.function.Supplier;

public record BakedSlabModel(BakedModel baseModel) implements BakedModel, FabricBakedModel {

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<net.minecraft.util.math.random.Random> randomSupplier, RenderContext context) {
        Direction omittedFace = switch (state.get(Properties.AXIS)) {
            case X -> state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? Direction.EAST : Direction.WEST;
            case Y -> null;
            case Z -> state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? Direction.SOUTH : Direction.NORTH;
        };

        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        MaterialFinder finder = renderer.materialFinder();

        MeshBuilder builder = renderer.meshBuilder();
        QuadEmitter emitter = builder.getEmitter();

        for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++)
            for (BakedQuad quad : baseModel.getQuads(null, ModelHelper.faceFromIndex(i), randomSupplier.get())) {
                emitter.fromVanilla(quad, finder.find(), ModelHelper.faceFromIndex(i));
                emitter.cullFace(null);
                emitter.emit();
            }

        context.meshConsumer().accept(builder.build());
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<net.minecraft.util.math.random.Random> randomSupplier, RenderContext context) {
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, net.minecraft.util.math.random.Random random) {
        return baseModel.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return baseModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return baseModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return baseModel.isBuiltin();
    }

    @Override
    public Sprite getParticleSprite() {
        return baseModel.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return baseModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return baseModel.getOverrides();
    }

}
