package me.enigmartyr.rs.client.render.block.entity;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;

public class SlabBlockEntityRenderer  implements BlockEntityRenderer<SlabBlockEntity> {
    private static final RenderLayer SOLID_NO_CRUMBLING = RenderLayer.of("solid_no_crumbling", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 0x200000, false, false, RenderLayer.of(RenderLayer.SOLID_PROGRAM));
    public SlabBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) { }

    /**
     * @author enigmartyr
     * @reason Makes it so that the mate slab is rendered in addition to the base slab; custom render layer
     *         required so that the mate slab does not also have the break animation (hasCrumbling = false)
     */
    Block lastMate;
    @Override
    public void render(@NotNull SlabBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity.hasMate())
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(blockEntity.getMateBlockState(blockEntity.getCachedState()), blockEntity.getPos(), blockEntity.getWorld(), matrices, vertexConsumers.getBuffer(SOLID_NO_CRUMBLING), true, Random.create());
    }
}
